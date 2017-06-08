import copy
import time

import torch
import torch.nn as nn
from torch.autograd import Variable

import Trainer
import onmt
import onmt.modules


def loadImageLibs():
    "Conditional import of torch image libs."
    global Image, transforms
    from PIL import Image
    from torchvision import transforms


class Translator(object):
    def __init__(self, opt):
        self.opt = opt

        # print "def Translator::Translator() opt:", repr(opt)

        self.gpus = []
        if self.opt.gpu > -1:
            self.gpus = [ self.opt.gpu ]

        self.tt = torch.cuda if opt.cuda else torch
        self.beam_accum = None

        checkpoint = torch.load(opt.model)
        # print('checkpoint: %s' % repr(checkpoint))

        self.model_opt = checkpoint['opt']
        # print "def Translator::Translator() model_opt:", repr(model_opt)

        self.dicts = checkpoint['dicts']

        # print(' * vocabulary size. source = %d; target = %d' %
        #   (self.getSourceDict().size(), self.getTargetDict().size()))
        # print(' * maximum batch size. %d' % opt.batch_size)

        # print('Building model...')

        self._type = self.model_opt.encoder_type \
            if "encoder_type" in self.model_opt else "text"

        if self._type == "text":
            encoder = onmt.Models.Encoder(self.model_opt, self.getSourceDict())
        elif self._type == "img":
            loadImageLibs()
            encoder = onmt.modules.ImageEncoder(self.model_opt)

        decoder = onmt.Models.Decoder(self.model_opt, self.getTargetDict())

        model = onmt.Models.NMTModel(encoder, decoder)
        model.load_state_dict(checkpoint['model'])

        generator = nn.Sequential(nn.Linear(self.model_opt.rnn_size, self.getTargetDict().size()),nn.LogSoftmax())
        generator.load_state_dict(checkpoint['generator'])

        # generator_state_dict = super(nn.Sequential, generator).state_dict()
        # print 'def Translator::Translator generator:', repr(generator)
        # for name, param in sorted(generator_state_dict.items()):
        #         print ('def Translator::Translator generator_state_dict name',name)
        #         print ('def Translator::Translator generator_state_dict own_state[name]',generator_state_dict[name])


        if opt.cuda:
            model.cuda()
            generator.cuda()
        else:
            model.cpu()
            generator.cpu()

        model.generator = generator

        self.optim = checkpoint['optim']

        self.optim.set_parameters(model.parameters())
        self.optim.optimizer.load_state_dict(checkpoint['optim'].optimizer.state_dict())


        self.model = model
        self.model.eval()

        self.trainer = Trainer.Trainer(self.model_opt)


    def initBeamAccum(self):
        self.beam_accum = {
            "predicted_ids": [],
            "beam_parent_ids": [],
            "scores": [],
            "log_probs": []}

    def _getBatchSize(self, batch):
        if self._type == "text":
            return batch.size(1)
        else:
            return batch.size(0)

    def buildData(self, srcBatch, goldBatch, volatile=True):
        # This needs to be the same as preprocess.py.
        if self._type == "text":
            srcData = [self.getSourceDict().convertToIdx(b,
                                                  onmt.Constants.UNK_WORD)
                       for b in srcBatch]
        elif self._type == "img":
            srcData = [transforms.ToTensor()(
                Image.open(self.opt.src_img_dir + "/" + b[0]))
                       for b in srcBatch]

        tgtData = None
        if goldBatch:
            tgtData = [self.getTargetDict().convertToIdx(b,
                       onmt.Constants.UNK_WORD,
                       onmt.Constants.BOS_WORD,
                       onmt.Constants.EOS_WORD) for b in goldBatch]

        return onmt.Dataset(srcData, tgtData,
                            self.opt.batch_size, self.opt.cuda, volatile,
                            data_type=self._type)

    def buildTargetTokens(self, pred, src, attn):
        tokens = self.getTargetDict().convertToLabels(pred, onmt.Constants.EOS)
        tokens = tokens[:-1]  # EOS
        if self.opt.replace_unk:
            for i in range(len(tokens)):
                if tokens[i] == onmt.Constants.UNK_WORD:
                    _, maxIndex = attn[i].max(0)
                    tokens[i] = src[maxIndex[0]]
        return tokens

    def getSourceDict(self):
        return self.dicts['src']

    def getTargetDict(self):
        return self.dicts['tgt']

    def translateBatch(self, srcBatch, tgtBatch, model=None):

        if model == None: model = self.model

        batchSize = srcBatch[0].size(1)
        beamSize = self.opt.beam_size

        #  (1) run the encoder on the src
        encStates, context = model.encoder(srcBatch)
        srcBatch = srcBatch[0]  # drop the lengths needed for encoder

        rnnSize = context.size(2)
        encStates = (model._fix_enc_hidden(encStates[0]),
                     model._fix_enc_hidden(encStates[1]))

        decoder = model.decoder
        attentionLayer = decoder.attn
        useMasking = self._type == "text"

        #  This mask is applied to the attention model inside the decoder
        #  so that the attention ignores source padding
        padMask = None
        if useMasking:
            padMask = srcBatch.data.eq(onmt.Constants.PAD).t()

        def mask(padMask):
            if useMasking:
                attentionLayer.applyMask(padMask)

        # (2) if a target is specified, compute the 'goldScore'
        #  (i.e. log likelihood) of the target under the model
        goldScores = context.data.new(batchSize).zero_()
        if tgtBatch is not None:
            decStates = encStates
            # decOut = model.make_init_decoder_output(context)

            mask(padMask)
            initOutput = model.make_init_decoder_output(context)

            decOut, decStates, attn = model.decoder(
                tgtBatch[:-1], decStates, context, initOutput)
            for dec_t, tgt_t in zip(decOut, tgtBatch[1:].data):
                gen_t = model.generator.forward(dec_t)
                tgt_t = tgt_t.unsqueeze(1)
                scores = gen_t.data.gather(1, tgt_t)
                scores.masked_fill_(tgt_t.eq(onmt.Constants.PAD), 0)
                goldScores += scores

        # (3) run the decoder to generate sentences, using beam search

        # Expand tensors for each beam.
        context = Variable(context.data.repeat(1, beamSize, 1))
        decStates = (Variable(encStates[0].data.repeat(1, beamSize, 1)),
                     Variable(encStates[1].data.repeat(1, beamSize, 1)))

        beam = [onmt.Beam(beamSize, self.opt.cuda) for k in range(batchSize)]

        decOut = model.make_init_decoder_output(context)

        if useMasking:
            padMask = srcBatch.data.eq(
                onmt.Constants.PAD).t() \
                                   .unsqueeze(0) \
                                   .repeat(beamSize, 1, 1)

        batchIdx = list(range(batchSize))
        remainingSents = batchSize
        for i in range(self.opt.max_sent_length):
            mask(padMask)

            # Prepare decoder input.
            input = torch.stack([b.getCurrentState() for b in beam
                                 if not b.done]).t().contiguous().view(1, -1)

            decOut, decStates, attn = model.decoder(
                Variable(input, volatile=True), decStates, context, decOut)

            # decOut: 1 x (beam*batch) x numWords
            decOut = decOut.squeeze(0)
            out = model.generator.forward(decOut)

            # batch x beam x numWords
            wordLk = out.view(beamSize, remainingSents, -1).transpose(0, 1).contiguous()
            attn = attn.view(beamSize, remainingSents, -1).transpose(0, 1).contiguous()

            active = []
            for b in range(batchSize):
                if beam[b].done:
                    continue

                idx = batchIdx[b]
                if not beam[b].advance(wordLk.data[idx], attn.data[idx]):
                    active += [b]

                for decState in decStates:  # iterate over h, c
                    # layers x beam*sent x dim
                    sentStates = decState.view(
                        -1, beamSize, remainingSents, decState.size(2))[:, :, idx]
                    sentStates.data.copy_(
                        sentStates.data.index_select(1, beam[b].getCurrentOrigin()))

            if not active:
                break

            # in this section, the sentences that are still active are
            # compacted so that the decoder is not run on completed sentences
            activeIdx = self.tt.LongTensor([batchIdx[k] for k in active])
            batchIdx = {beam: idx for idx, beam in enumerate(active)}

            def updateActive(t):
                # select only the remaining active sentences
                view = t.data.view(-1, remainingSents, rnnSize)
                newSize = list(t.size())
                newSize[-2] = newSize[-2] * len(activeIdx) // remainingSents
                return Variable(view.index_select(1, activeIdx).view(*newSize), volatile=True)

            decStates = (updateActive(decStates[0]), updateActive(decStates[1]))
            decOut = updateActive(decOut)
            context = updateActive(context)
            if useMasking:
                padMask = padMask.index_select(1, activeIdx)

            remainingSents = len(active)

        # (4) package everything up

        allHyp, allScores, allAttn = [], [], []
        n_best = self.opt.n_best

        for b in range(batchSize):
            scores, ks = beam[b].sortBest()

            allScores += [scores[:n_best]]
            hyps, attn = zip(*[beam[b].getHyp(k) for k in ks[:n_best]])
            allHyp += [hyps]
            if useMasking:
                valid_attn = srcBatch.data[:, b].ne(onmt.Constants.PAD).nonzero().squeeze(1)
                attn = [a.index_select(1, valid_attn) for a in attn]
            allAttn += [attn]

            if self.beam_accum:
                self.beam_accum["beam_parent_ids"].append(
                    [t.tolist()
                     for t in beam[b].prevKs])
                self.beam_accum["scores"].append([
                    ["%4f" % s for s in t.tolist()]
                    for t in beam[b].allScores][1:])
                self.beam_accum["predicted_ids"].append(
                    [[self.tgt_dict.getLabel(id)
                      for id in t.tolist()]
                     for t in beam[b].nextYs][1:])

        return allHyp, allScores, allAttn, goldScores

    def translate(self, srcBatch, goldBatch):
        #  (1) convert words to indexes
        dataset = self.buildData(srcBatch, goldBatch)
        src, tgt, indices = dataset[0]

        #  (2) translate
        # pred, predScore, attn, goldScore = self.translateBatch(src, tgt, model=self.model)
        pred, predScore, attn, goldScore = self.translateBatch(src, tgt)
        pred, predScore, attn, goldScore = list(
            zip(*sorted(zip(pred, predScore, attn, goldScore, indices), key=lambda x: x[-1])))[:-1]

        #  (3) convert indexes to words
        predBatch = []
        for b in range(src[0].size(1)):
            predBatch.append(
                [self.buildTargetTokens(pred[b][n], srcBatch[b], attn[b][n])
                 for n in range(len(pred[b]))]
            )

        return predBatch, predScore, goldScore

    def translateWithAdaptation(self, srcBatch, goldBatch, suggestions):

        #  (1) convert words to indexes [input and reference (if nay)]
        dataset = self.buildData(srcBatch, goldBatch)
        src, tgt, indices = dataset[0]

        # (1) convert words to indexes [suggestions]
        indexedTuningSrcBatch, indexedTuningTgtBatch = [], []
        for sugg in suggestions:
            indexedTuningSrcBatch += [self.getSourceDict().convertToIdx(sugg.source, onmt.Constants.UNK_WORD)]
            indexedTuningTgtBatch += [self.getTargetDict().convertToIdx(sugg.target, onmt.Constants.UNK_WORD, onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD)]

        # prepare data for training on the tuningBatch
        tuningDataset = {'train': {'src': indexedTuningSrcBatch, 'tgt': indexedTuningTgtBatch}, 'dicts': self.dicts}

        tuningTrainData = onmt.Dataset(tuningDataset['train']['src'],
                             tuningDataset['train']['tgt'], self.opt.batch_size, self.gpus)


        #
        ## make a copy of "static" model
        # print('copying model... START')
        # start_time = time.time()
        # model_copy = copy.deepcopy(self.model)
        # optim_copy = copy.deepcopy(self.optim)
        # print('copying model... END %.2fs' % (time.time() - start_time))

        # print('tuning model... START')

        # print 'def Translator:translateWithAdaptation id(self.model):', repr(id(self.model))
        # print 'def Translator:translateWithAdaptation id(self.model.encoder):', repr(id(self.model.encoder))
        # print 'def Translator:translateWithAdaptation id(self.optim):', repr(id(self.optim))
        # # print 'def Translator:translateWithAdaptation id(model_copy):', repr(id(model_copy))
        # print 'def Translator:translateWithAdaptation id(model_copy.encoder):', repr(id(model_copy.encoder))
        # print 'def Translator:translateWithAdaptation id(optim_copy):', repr(id(optim_copy))

        start_time = time.time()

        # model_state_dict = super(onmt.Models.NMTModel, self.model).state_dict()
        # print 'def Translator::translateWithAdaptation before  model_state_dict:', repr(model_state_dict)
        # for name, param in sorted(model_state_dict.items()):
        #         print ('def Translator::translateWithAdaptation before model_state_dict name',name)
        #         print ('def Translator::translateWithAdaptation before model_state_dict own_state[name]',model_state_dict[name])
        #
        #
        # print('tuning model... START')
        # model_copy = self.trainer.trainModel(self.model, tuningTrainData, None, tuningDataset, self.optim, save_all_epochs=False, save_last_epoch=False, epochs=self.opt.tuning_epochs, clone=True)
        # model_copy.eval()
        # print('tuning model... END %.2fs' % (time.time() - start_time))
        #
        # model_state_dict = super(onmt.Models.NMTModel, model_copy).state_dict()
        # print 'def Translator::translateWithAdaptation after  model_state_dict:', repr(model_state_dict)
        # for name, param in sorted(model_state_dict.items()):
        #         print ('def Translator::translateWithAdaptation after model_state_dict name',name)
        #         print ('def Translator::translateWithAdaptation after model_state_dict own_state[name]',model_state_dict[name])



        # model_state_dict = super(onmt.Models.NMTModel, self.model).state_dict()
        # print 'def Translator::translateWithAdaptation before  model_state_dict:', repr(model_state_dict)
        # for name, param in sorted(model_state_dict.items()):
        #         print ('def Translator::translateWithAdaptation before model_state_dict name',name)
        #         print ('def Translator::translateWithAdaptation before model_state_dict own_state[name]',model_state_dict[name])

        self.model = self.trainer.trainModel(self.model, tuningTrainData, None, tuningDataset, self.optim, save_all_epochs=False, save_last_epoch=False, epochs=self.opt.tuning_epochs, clone=False)


        # model_state_dict = super(onmt.Models.NMTModel, self.model).state_dict()
        # print 'def Translator::translateWithAdaptation after  model_state_dict:', repr(model_state_dict)
        # for name, param in sorted(model_state_dict.items()):
        #         print ('def Translator::translateWithAdaptation after model_state_dict name',name)
        #         print ('def Translator::translateWithAdaptation after model_state_dict own_state[name]',model_state_dict[name])

        # print('tuning model... END %.2fs' % (time.time() - start_time))




        #  (2) translate
        start_time = time.time()
        ###pred, predScore, attn, goldScore = self.translateBatch(src, tgt, model=model_copy)
        pred, predScore, attn, goldScore = self.translateBatch(src, tgt)
        pred, predScore, attn, goldScore = list(zip(*sorted(zip(pred, predScore, attn, goldScore, indices), key=lambda x: x[-1])))[:-1]

        #  (3) convert indexes to words
        predBatch = []
        for b in range(src[0].size(1)):
            predBatch.append(
                [self.buildTargetTokens(pred[b][n], srcBatch[b], attn[b][n])
                 for n in range(self.opt.n_best)]
            )


        return predBatch, predScore, goldScore


# Sentence is an array of strings
# Sentence = [ strings ]

# srcBatch and tgtBatch are Arrays of Sentences
# srcBatch = [ Sentences ]
# tgtBatch = [ Sentences ]


# functions translate and translateOnline return
# a tuple [predBatch, predScore, goldScore]
# predBatch is an Array containing the nbest output Sentence (after reaplacemente of unknown words, if active) for each input sentence
# predBatch = [ nbests ]
# nbest = [ Sentence ]
#
# predScore is an tuple containing the scores (as Tensors) of nbest output sentences for each input Sentence
# predScore = [ nbest_score ]
# nbest_score = float
#
# goldScore is an Array containing the BLEU score of the best hypothesis with respect to the gold standard, if provided
# goldScore = [ float ]

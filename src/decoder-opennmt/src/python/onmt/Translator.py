import copy
import time

import torch
import torch.nn as nn
from torch.autograd import Variable

import Trainer
import onmt


class Translator(object):
    def __init__(self, opt):
        self.opt = opt
        self.gpus = []
        if self.opt.gpu > -1:
            self.gpus = [ self.opt.gpu ]

        self.tt = torch.cuda if opt.cuda else torch

        checkpoint = torch.load(opt.model)

        model_opt = checkpoint['opt']
        self.dicts = checkpoint['dicts']
        self.optim = checkpoint['optim']

        encoder = onmt.Models.Encoder(model_opt, self.getSourceDict())
        decoder = onmt.Models.Decoder(model_opt, self.getTargetDict())
        model = onmt.Models.NMTModel(encoder, decoder)

        generator = nn.Sequential(
            nn.Linear(model_opt.rnn_size, self.dicts['tgt'].size()),
            nn.LogSoftmax())

        model.load_state_dict(checkpoint['model'])
        generator.load_state_dict(checkpoint['generator'])

        if opt.cuda:
            model.cuda()
            generator.cuda()
        else:
            model.cpu()
            generator.cpu()

        model.generator = generator

        self.model = model
        self.model.eval()

        self.trainer = Trainer.Trainer(model_opt)

    def buildData(self, srcBatch, goldBatch, volatile=True):
        srcData = [self.getSourceDict().convertToIdx(b,
                    onmt.Constants.UNK_WORD) for b in srcBatch]
        tgtData = None
        if goldBatch:
            tgtData = [self.getTargetDict().convertToIdx(b,
                       onmt.Constants.UNK_WORD,
                       onmt.Constants.BOS_WORD,
                       onmt.Constants.EOS_WORD) for b in goldBatch]

        return onmt.Dataset(srcData, tgtData,
                            self.opt.batch_size, self.opt.cuda, volatile)

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
        print "def translateBatch() model=", hex(id(model))
        print "def translateBatch() model.encoder=", hex(id(model.encoder))
        print "def translateBatch() model.decoder=", hex(id(model.decoder))
        print "def translateBatch() self.model=", hex(id(self.model))
        print "def translateBatch() self.model.encoder=", hex(id(self.model.encoder))
        print "def translateBatch() self.model.decoder=", hex(id(self.model.decoder))
        batchSize = srcBatch[0].size(1)
        beamSize = self.opt.beam_size

        #  (1) run the encoder on the src
        encStates, context = model.encoder(srcBatch)
        srcBatch = srcBatch[0]  # drop the lengths needed for encoder

        rnnSize = context.size(2)
        encStates = (model._fix_enc_hidden(encStates[0]),
                     model._fix_enc_hidden(encStates[1]))

        #  This mask is applied to the attention model inside the decoder
        #  so that the attention ignores source padding
        padMask = srcBatch.data.eq(onmt.Constants.PAD).t()

        def applyContextMask(m):
            if isinstance(m, onmt.modules.GlobalAttention):
                m.applyMask(padMask)

        # (2) if a target is specified, compute the 'goldScore'
        #  (i.e. log likelihood) of the target under the model
        goldScores = context.data.new(batchSize).zero_()
        if tgtBatch is not None:
            decStates = encStates
            decOut = model.make_init_decoder_output(context)
            model.decoder.apply(applyContextMask)
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

        padMask = srcBatch.data.eq(onmt.Constants.PAD).t().unsqueeze(0).repeat(beamSize, 1, 1)

        batchIdx = list(range(batchSize))
        remainingSents = batchSize
        for i in range(self.opt.max_sent_length):

            model.decoder.apply(applyContextMask)

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
                return Variable(view.index_select(1, activeIdx) \
                                .view(*newSize), volatile=True)

            decStates = (updateActive(decStates[0]), updateActive(decStates[1]))
            decOut = updateActive(decOut)
            context = updateActive(context)
            padMask = padMask.index_select(1, activeIdx)

            remainingSents = len(active)

        # (4) package everything up

        allHyp, allScores, allAttn = [], [], []
        n_best = self.opt.n_best

        for b in range(batchSize):
            scores, ks = beam[b].sortBest()

            allScores += [scores[:n_best]]
            valid_attn = srcBatch.data[:, b].ne(onmt.Constants.PAD).nonzero().squeeze(1)
            hyps, attn = zip(*[beam[b].getHyp(k) for k in ks[:n_best]])
            attn = [a.index_select(1, valid_attn) for a in attn]
            allHyp += [hyps]
            allAttn += [attn]

        return allHyp, allScores, allAttn, goldScores

    def translate(self, srcBatch, goldBatch):
        #  (1) convert words to indexes
        dataset = self.buildData(srcBatch, goldBatch)
        src, tgt, indices = dataset[0]

        print "def translate() src:", src
        print "def translate() self.opt.n_best:", self.opt.n_best

        #  (2) translate
        pred, predScore, attn, goldScore = self.translateBatch(src, tgt)
        pred, predScore, attn, goldScore = list(
            zip(*sorted(zip(pred, predScore, attn, goldScore, indices), key=lambda x: x[-1])))[:-1]

        #  (3) convert indexes to words
        predBatch = []
        print "def translate() src[0]:", src[0]
        for b in range(src[0].size(1)):

            print "def translate() b:", b, " pred[b]:", repr(pred[b])

            predBatch.append(
                [self.buildTargetTokens(pred[b][n], srcBatch[b], attn[b][n])
                 for n in range(len(pred[b]))]
            )

        # print of the nbest for each sentence of the batch
        for b in range(len(predBatch)):
            for n in range(len(predBatch[b])):
                print "def Translator::translate(self, text, suggestions=None) predScore[b][n]:", repr(predScore[b][n]), " predBatch[b][n]:", repr(predBatch[b][n])

        return predBatch, predScore, goldScore

    ###def translateOnline(self, srcBatch, goldBatch, tuningTrainData):
    ### def translateOnline(self, srcBatch, goldBatch, tuningBatch):
    def translateOnline(self, srcBatch, goldBatch, suggestions):

        #  (1) convert words to indexes [input and reference (if nay)]
        dataset = self.buildData(srcBatch, goldBatch)
        src, tgt, indices = dataset[0]

        print "def translateOnline() src:", src


        # (1) convert words to indexes [suggestions]
        indexedTuningSrcBatch, indexedTuningTgtBatch = [], []
        for sugg in suggestions:
            print "def translateOnline() sugg.source:", sugg.source
            print "def translateOnline() sugg.target:", sugg.target

            indexedTuningSrcBatch += [self.getSourceDict().convertToIdx(sugg.source, onmt.Constants.UNK_WORD)]
            indexedTuningTgtBatch += [self.getTargetDict().convertToIdx(sugg.target, onmt.Constants.UNK_WORD, onmt.Constants.BOS_WORD, onmt.Constants.EOS_WORD)]

        # prepare data for training on the tuningBatch
        tuningDataset = {'train': {'src': indexedTuningSrcBatch, 'tgt': indexedTuningTgtBatch}, 'dicts': self.dicts}

        tuningTrainData = onmt.Dataset(tuningDataset['train']['src'],
                             tuningDataset['train']['tgt'], self.opt.batch_size, self.gpus)

        # tuningSrcBatch, tuningTgtBatch = [], []
        # for sugg in suggestions:
        #     tuningSrcBatch.append(sugg.source)
        #     tuningTgtBatch.append(sugg.target)
        #
        # tuningTrainData = self.buildData(tuningSrcBatch, tuningTgtBatch)
        # tuningSrcBatch, tuningTgtBatch, indices = tuningTrainData[0]
        #
        # tuningDataset = { 'train': { 'src':tuningSrcBatch, 'tgt':tuningTgtBatch }, 'dicts':self.dicts }

        print('tuningDataset.train.src:', repr(tuningDataset['train']['src']))
        print('tuningDataset.train.tgt:', repr(tuningDataset['train']['tgt']))

        ### make a copy of "static" model
        print('copying model... START')
        start_time = time.time()
        model_copy = copy.deepcopy(self.model)
        optim_copy = copy.deepcopy(self.optim)
        print('copying model... END %.2fs' % (time.time() - start_time))

        print('tuning model... START')
        start_time = time.time()
        model_copy.train()
        self.trainer.trainModel(model_copy, tuningTrainData, None, tuningDataset, optim_copy, save_all_epochs=True, save_last_epoch=False, epochs=self.opt.tuning_epochs)
        model_copy.eval()
        print('tuning model... END %.2fs' % (time.time() - start_time))

        print "def translateOnline() model_copy=", hex(id(model_copy))
        print "def translateOnline() model_copy.encoder=", hex(id(model_copy.encoder))
        print "def translateOnline() model_copy.decoder=", hex(id(model_copy.decoder))
        print "def translateOnline() optim_copy=", hex(id(optim_copy))
        print "def translateOnline() self.model=", hex(id(self.model))
        print "def translateOnline() self.model.encoder=", hex(id(self.model.encoder))
        print "def translateOnline() self.model.decoder=", hex(id(self.model.decoder))
        print "def translateOnline() self.optim=", hex(id(self.optim))

        #  (2) translate
        pred, predScore, attn, goldScore = self.translateBatch(src, tgt, model=model_copy)
        pred, predScore, attn, goldScore = list(zip(*sorted(zip(pred, predScore, attn, goldScore, indices), key=lambda x: x[-1])))[:-1]

        #  (3) convert indexes to words
        predBatch = []
        for b in range(src[0].size(1)):
            predBatch.append(
                [self.buildTargetTokens(pred[b][n], srcBatch[b], attn[b][n])
                 for n in range(self.opt.n_best)]
            )

        # print of the nbest for each sentence of the batch
        for b in range(len(predBatch)):
            for n in range(len(predBatch[b])):
                print "def Translator::translate(self, text, suggestions=None) predScore[b][n]:", repr(predScore[b][n]), " predBatch[b][n]:", repr(predBatch[b][n])

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

import copy
import logging
import time

import torch
import torch.cuda.random as random
import torch.nn as nn
from torch.autograd import Variable

import onmt
import onmt.modules
from onmt import Trainer


class Translator(object):
    class Options:
        def __init__(self):
            self.gpu = -1  # the index of the GPU to use
            self.beam_size = 5
            self.batch_size = 30
            self.max_sent_length = 160
            self.replace_unk = False
            self.dump_beam = None  # File to dump beam information to
            self.n_best = 1
            self.tuning_epochs = 5
            self.seed = 3435
            self.tunable = True  # Enable fine tuning
            self.reset = True  # Reset model to the original model after each translation

        @property
        def cuda(self):
            return self.gpu > -1

    def __init__(self, model, opt):
        self.opt = opt

        if self.opt.seed >= 0:
            torch.manual_seed(self.opt.seed)

        self._reset_after_tuning = self.opt.tunable and self.opt.reset
        self._gpus = [self.opt.gpu] if self.opt.gpu > -1 else []

        self._logger = logging.getLogger('onmt.Translator')
        self._logger.info("Translator options:%s" % repr(self.opt))

        self.tt = torch.cuda if opt.cuda else torch
        self.beam_accum = None

        self._logger.info("Loading checkpoint... START")
        start_time = time.time()
        self.checkpoint = torch.load(model)
        self._logger.info("Loading checkpoint... END %.2fs" % (time.time() - start_time))

        self._logger.info("Creating dicts, model, and optimizer from checkpoint... S\TART")
        start_time = time.time()
        self.dicts, self.model, self.optim = self.create(self.checkpoint)
        self._logger.info(
            'Creating dicts, model, and optimizer from checkpoint... END %.2fs' % (time.time() - start_time))

        self.checkpoint_copy = {}
        if self._reset_after_tuning:
            self._logger.info('Deepcopying checkpoint... START')
            start_time = time.time()
            self.checkpoint_copy = copy.deepcopy(self.checkpoint)
            self._logger.info('Deepcopying checkpoint... END %.2fs' % (time.time() - start_time))

            self._logger.info("Creating dicts_copy, model_copy, and optimizer_copy from checkpoint... START")
            start_time = time.time()
            self.dicts_copy, self.model_copy, self.optim_copy = self.create(self.checkpoint)
            self._logger.info('Creating dicts_copy, model_copy, and optimizer_copy from checkpoint... END %.2fs' % (
                time.time() - start_time))

        self._logger.info("Building trainer... START")
        start_time = time.time()

        self.trainer = Trainer(self.model_opt)
        self._logger.info("Building trainer... END %.2fs" % (time.time() - start_time))

    def create(self, checkpoint):
        torch.manual_seed(self.opt.seed)
        random.manual_seed_all(self.opt.seed)

        self._logger.info("loading model options from checkpoint... START")
        start_time2 = time.time()

        self.model_opt = Trainer.Options()
        self.model_opt.load_state_dict(checkpoint['opt'])
        self.model_opt.min_epochs = self.model_opt.max_epochs = self.opt.tuning_epochs
        self.model_opt.min_perplexity_decrement = -1.

        self._logger.info("loadin model options from checkpoint... END %.2fs" % (time.time() - start_time2))
        self._logger.info("Model Options: %s" % repr(self.model_opt))

        self._logger.info("Building model... START")
        start_time = time.time()

        self._logger.info("getting dicts from checkpoint... START")
        start_time2 = time.time()
        dicts = checkpoint['dicts']
        self._logger.info("getting dicts from checkpoint... END %.2fs" % (time.time() - start_time2))

        self._logger.info(' Vocabulary size. source = %d; target = %d' % (dicts['src'].size(), dicts['tgt'].size()))
        self._logger.info(' Maximum batch size. %d' % self.opt.batch_size)

        self._type = self.model_opt.encoder_type

        if self._type == "text":
            self._logger.info("constructing encoder... START")
            start_time2 = time.time()
            encoder = onmt.Models.Encoder(self.model_opt, dicts['src'])
            self._logger.info("constructing encoder... END %.2fs" % (time.time() - start_time2))
        elif self._type == "img":
            raise NotImplementedError

        self._logger.info("constructing decoder... START")
        start_time2 = time.time()
        decoder = onmt.Models.Decoder(self.model_opt, dicts['tgt'])
        self._logger.info("constructing decoder... END %.2fs" % (time.time() - start_time2))

        self._logger.info("constructing model from encoder and decoder... START")
        start_time2 = time.time()
        model = onmt.Models.NMTModel(encoder, decoder)
        self._logger.info("constructing model from encoder and decoder... END %.2fs" % (time.time() - start_time2))

        self._logger.info("loading model from checkpoint... START")
        start_time2 = time.time()
        model.load_state_dict(checkpoint['model'])
        self._logger.info("loading model from checkpoint... END %.2fs" % (time.time() - start_time2))

        self._logger.info("constructing generator... START")
        start_time2 = time.time()
        generator = nn.Sequential(nn.Linear(self.model_opt.rnn_size, dicts['tgt'].size()), nn.LogSoftmax())
        self._logger.info("constructing generator... END %.2fs" % (time.time() - start_time2))

        self._logger.info("loading generator from checkpoint... START")
        start_time2 = time.time()
        generator.load_state_dict(checkpoint['generator'])
        self._logger.info("loading generator from checkpoint... END %.2fs" % (time.time() - start_time2))

        if self.opt.cuda:
            model.cuda()
            generator.cuda()
        else:
            model.cpu()
            generator.cpu()

        self._logger.info("setting generator for model... START")
        start_time2 = time.time()
        model.generator = generator
        self._logger.info("setting generator for model... END %.2fs" % (time.time() - start_time2))

        model.eval()

        self._logger.info("Building model... END %.2fs" % (time.time() - start_time))

        self._logger.info("loading optimizer from checkpoint... START")
        start_time = time.time()
        optim = checkpoint['optim']
        optim.set_parameters(model.parameters())
        optim.optimizer.load_state_dict(checkpoint['optim'].optimizer.state_dict())
        self._logger.info("loading optimizer from checkpoint... END %.2fs" % (time.time() - start_time))

        return dicts, model, optim

    def load(self, checkpoint, model, optim):
        torch.manual_seed(self.opt.seed)
        random.manual_seed_all(self.opt.seed)

        self._logger.info("loading model from checkpoint... START")
        start_time2 = time.time()

        model_state_dict = {k: v for k, v in sorted(checkpoint['model'].items()) if 'generator' not in k}
        model_state_dict.update({"generator." + k: v for k, v in sorted(checkpoint['generator'].items())})
        model.load_state_dict(model_state_dict)

        self._logger.info("disabling dropout")
        model.encoder.setDropout(0.0)
        model.decoder.setDropout(0.0)

        self._logger.info("loading model from checkpoint... END %.2fs" % (time.time() - start_time2))

        self._logger.info("loading optimizer from checkpoint... START")
        start_time = time.time()
        optim.set_parameters(model.parameters())
        optim.optimizer.load_state_dict(checkpoint['optim'].optimizer.state_dict())
        self._logger.info("loading optimizer from checkpoint... END %.2fs" % (time.time() - start_time))

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
            raise NotImplementedError

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
        self._logger.info('def Translator::translate translating without tuning')
        #  (1) convert words to indexes
        dataset = self.buildData(srcBatch, goldBatch)
        src, tgt, indices = dataset[0]

        #  (2) translate
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
        self._logger.info('def Translator::translateWithAdaptation translating with tuning')

        #  (1) convert words to indexes [input and reference (if nay)]
        dataset = self.buildData(srcBatch, goldBatch)
        src, tgt, indices = dataset[0]

        # (1) convert words to indexes [suggestions]
        indexedTuningSrcBatch, indexedTuningTgtBatch = [], []

        for sugg in suggestions:
            indexedTuningSrcBatch += [self.getSourceDict().convertToIdx(sugg.source, onmt.Constants.UNK_WORD)]
            indexedTuningTgtBatch += [
                self.getTargetDict().convertToIdx(sugg.target, onmt.Constants.UNK_WORD, onmt.Constants.BOS_WORD,
                                                  onmt.Constants.EOS_WORD)]

        # prepare data for training on the tuningBatch
        tuningDataset = {'train': {'src': indexedTuningSrcBatch, 'tgt': indexedTuningTgtBatch}, 'dicts': self.dicts}

        tuningTrainData = onmt.Dataset(tuningDataset['train']['src'],
                                       tuningDataset['train']['tgt'], self.opt.batch_size, self._gpus)

        if self._reset_after_tuning:
            self._logger.info('copying model... START')
            start_time = time.time()

            self._logger.info('creating copy... START')
            start_time2 = time.time()
            self.load(self.checkpoint_copy, self.model_copy, self.optim_copy)
            self._logger.info('creating copy... END %.2fs' % (time.time() - start_time2))

            self._logger.info('copying model... END %.2fs' % (time.time() - start_time))

        else:
            self.dicts_copy = self.dicts
            self.model_copy = self.model
            self.optim_copy = self.optim

        self._logger.info('tuning model... START')
        start_time = time.time()
        self.trainer.train_model(self.model_copy, tuningTrainData, None, tuningDataset, self.optim_copy, save_epochs=0)
        self._logger.info('tuning model... END %.2fs' % (time.time() - start_time))

        #  (2) translate
        self._logger.info('translation... START')
        start_time = time.time()
        pred, predScore, attn, goldScore = self.translateBatch(src, tgt, model=self.model_copy)
        pred, predScore, attn, goldScore = list(
            zip(*sorted(zip(pred, predScore, attn, goldScore, indices), key=lambda x: x[-1])))[:-1]
        self._logger.info('translation... END %.2fs' % (time.time() - start_time))

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

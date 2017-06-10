from __future__ import division

import onmt
import onmt.Markdown
import torch
import argparse
import math
import time

import logging
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger('opennmt.translate')

parser = argparse.ArgumentParser(description='translate.py')
onmt.Markdown.add_md_help_argument(parser)

parser.add_argument('-model', required=True,
                    help='Path to model .pt file')
parser.add_argument('-src',   required=True,
                    help='Source sequence to decode (one line per sequence)')
parser.add_argument('-src_img_dir',   default="",
                    help='Source image directory')
parser.add_argument('-tgt',
                    help='True target sequence (optional)')
parser.add_argument('-output', default='pred.txt',
                    help="""Path to output the predictions (each line will
                    be the decoded sequence""")
parser.add_argument('-beam_size',  type=int, default=5,
                    help='Beam size')
parser.add_argument('-batch_size', type=int, default=30,
                    help='Batch size')
parser.add_argument('-max_sent_length', type=int, default=100,
                    help='Maximum sentence length.')
parser.add_argument('-replace_unk', action="store_true",
                    help="""Replace the generated UNK tokens with the source
                    token that had the highest attention weight. If phrase_table
                    is provided, it will lookup the identified source token and
                    give the corresponding target token. If it is not provided
                    (or the identified source token does not exist in the
                    table) then it will copy the source token""")
# parser.add_argument('-phrase_table',
#                     help="""Path to source-target dictionary to replace UNK
#                     tokens. See README.md for the format of this file.""")
parser.add_argument('-verbose', action="store_true",
                    help='Print scores and predictions for each sentence')
parser.add_argument('-dump_beam', type=str, default="",
                    help='File to dump beam information to.')

parser.add_argument('-n_best', type=int, default=1,
                    help="""If verbose is set, will output the n_best
                    decoded sentences""")

parser.add_argument('-gpu', type=int, default=-1,
                    help="Device to run on")

#seed for generating random numbers
parser.add_argument('-seed',       type=int, default=3435,
                    help="Random seed for generating random numbers (-1 for un-defined the seed; default is 3435); ")



def reportScore(name, scoreTotal, wordsTotal):
    if wordsTotal != 0:
        logger.info("%s AVG SCORE: %.4f, %s PPL: %.4f" % (name, scoreTotal / wordsTotal,name, math.exp(-scoreTotal/wordsTotal)))
    else:
        logger.info("%s AVG SCORE: %s, %s PPL: %s" % (name, 'undef',name, 'undef'))

def addone(f):
    for line in f:
        yield line
    yield None

def main():
    opt = parser.parse_args()

    #Sets the seed for generating random numbers
    if (opt.seed>=0):
        torch.manual_seed(opt.seed)

    opt.cuda = opt.gpu > -1
    if opt.cuda:
        torch.cuda.set_device(opt.gpu)

    translator = onmt.Translator(opt)

    outF = open(opt.output, 'w')

    predScoreTotal, predWordsTotal, goldScoreTotal, goldWordsTotal = 0, 0, 0, 0

    srcBatch, tgtBatch = [], []

    count = 0

    tgtF = open(opt.tgt) if opt.tgt else None

    if opt.dump_beam != "":
        import json
        translator.initBeamAccum()


    logger.info('Translation... START')
    start_time = time.time()
    for line in addone(open(opt.src)):
        logger.info('translating: %s' % line)
        
        if line is not None:
            srcTokens = line.split()
            srcBatch += [srcTokens]
            if tgtF:
                tgtTokens = tgtF.readline().split() if tgtF else None
                tgtBatch += [tgtTokens]

            if len(srcBatch) < opt.batch_size:
                continue
        else:
            # at the end of file, check last batch
            if len(srcBatch) == 0:
                break

        predBatch, predScore, goldScore = translator.translate(srcBatch, tgtBatch)

        # print of the nbest for each sentence of the batch
        for b in range(len(predBatch)):
            for n in range(len(predBatch[b])):
                logger.info("b:%d n:%d predScore[b][n]:%g predBatch[b][n]:%s" % (b,n,predScore[b][n], repr(predBatch[b][n])))
        # logger.info("def translate::main() srcBatch" + repr(srcBatch))
        # logger.info("def translate::main() tgtBatch" + repr(tgtBatch))
        # logger.info("def translate::main() predBatch" + repr(predBatch))
        # logger.info("def translate::main() predScore" + repr(predScore))

        predScoreTotal += sum(score[0] for score in predScore)
        predWordsTotal += sum(len(x[0]) for x in predBatch)
        if tgtF is not None:
            goldScoreTotal += sum(goldScore)
            goldWordsTotal += sum(len(x) for x in tgtBatch)

        for b in range(len(predBatch)):
            count += 1
            outF.write(" ".join(predBatch[b][0]) + '\n')
            outF.flush()

            if opt.verbose:
                srcSent = ' '.join(srcBatch[b])
                if translator.getTargetDict().lower:
                    srcSent = srcSent.lower()
                logger.info('SENT %d: %s' % (count, srcSent))
                logger.info('PRED %d: SCORE: %.4f TRANSLATION:%s' % (count, predScore[b][0], " ".join(predBatch[b][0])))

                if tgtF is not None:
                    tgtSent = ' '.join(tgtBatch[b])
                    if translator.getTargetDict().lower:
                        tgtSent = tgtSent.lower()
                    logger.info('GOLD %d: SCORE: %.4f TRANSLATION:%s ' % (count, goldScore[b], tgtSent))

                if opt.n_best > 1:
                    logger.info('BEST HYP:')
                    for n in range(len(predBatch[b])):
                        logger.info("b:%g n:%g [%.4f] %s" % (b, n, predScore[b][n], " ".join(predBatch[b][n])))

                logger.info('')

        srcBatch, tgtBatch = [], []

    reportScore('PRED', predScoreTotal, predWordsTotal)
    if tgtF:
        reportScore('GOLD', goldScoreTotal, goldWordsTotal)

    if tgtF:
        tgtF.close()

    if opt.dump_beam:
        json.dump(translator.beam_accum, open(opt.dump_beam, 'w'))

    logger.info('Translation... END %.2fs' % (time.time() - start_time))

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()

import onmt
import torch.nn as nn
import torch
from torch.autograd import Variable

import time
import copy

from nmt import MMTDecoder
from onmt import Trainer, Translator
from onmt.Models import Encoder, Decoder


import argparse
parser = argparse.ArgumentParser(description='train.py')

parser.add_argument('-train_from_state_dict', default='', type=str,
                    help="""If training from a checkpoint then this is the
                    path to the pretrained model's state_dict.""")

class OpenNMTDecoder(MMTDecoder):
    def __init__(self, model_path):
        MMTDecoder.__init__(self, model_path)
        # TODO: stub implementation

        # TODO: how to create the opt object?
        ## Assuming that model_path is an actual model (and not is )
        checkpoint_path=model_path

        # parameters = "-train_from_state_dict " + checkpoint_path
        # parameters += " " + "-gpu " + str(0)
        # print 'class OpenNMTDecoder(Decoder) parameter:', parameters

        ###opt = parser.parse_args(args=parameters)
        opt = parser.parse_args(args="")
        opt.model = checkpoint_path
        opt.batch_size = 1
        opt.beam_size = 5
        opt.max_sent_length = 100
        opt.n_best = 1
        opt.replace_unk = True
        opt.verbose = False
        opt.tuning_epochs = 3

        opt.gpu = -1
        if opt.gpu > -1:
            opt.cuda = True
        else:
            opt.cuda = False


        opt.seed = 1234
        #Sets the seed for generating random numbers
        if (opt.seed>=0):
            torch.manual_seed(opt.seed)

        self.translator = Translator(opt)

    def translate(self, text, suggestions=None):
        # TODO: stub implementation

        #if (int(time.time()) % 2) == 0:
        #    raise ArithmeticError("fake exception")

        ###srcBatch = [ text ]

        srcBatch = []
        srcTokens = text
        srcBatch += srcTokens


        if len(suggestions) == 0:
            predBatch, predScore, goldScore = self.translator.translate(srcBatch, None)
        else:
            tuningSrcBatch, tuningTgtBatch = [], []
            for sugg in suggestions:
                tuningSrcBatch.append(sugg.source)
                tuningTgtBatch.append(sugg.target)

            tuningBatch = { 'src':tuningSrcBatch, 'tgt':tuningTgtBatch }
            predBatch, predScore, goldScore = self.translator.translateOnline(srcBatch, None, tuningBatch)

        output = predBatch[0][0]
        return output

    def _preferred_threads(self):
        # TODO: stub implementation (should be number of GPUs)
        return 4


#########
#### An example of input for
#### echo {"id":1, "source":"hello and goodbye", "suggestions":[{"source": "A", "target": "a", "score":"0.1"},{"source": "B", "target": "b", "score":"0.2"}]}
#########
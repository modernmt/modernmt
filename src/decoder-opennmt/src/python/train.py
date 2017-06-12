from __future__ import division

import onmt
import onmt.Markdown
import onmt.modules
import argparse
import torch
import torch.nn as nn
from torch import cuda
from torch.autograd import Variable
import time

from onmt.Trainer import Trainer

import logging
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger('opennmt.train')

parser = argparse.ArgumentParser(description='train.py')
onmt.Markdown.add_md_help_argument(parser)

## Data options

parser.add_argument('-data', required=True,
                    help='Path to the *-train.pt file from preprocess.py')
parser.add_argument('-save_model', default='model',
                    help="""Model filename (the model will be saved as
                    <save_model>_epochN_PPL.pt where PPL is the
                    validation perplexity""")
parser.add_argument('-train_from_state_dict', default='', type=str,
                    help="""If training from a checkpoint then this is the
                    path to the pretrained model's state_dict.""")
parser.add_argument('-train_from', default='', type=str,
                    help="""If training from a checkpoint then this is the
                    path to the pretrained model.""")

## Model options

parser.add_argument('-layers', type=int, default=2,
                    help='Number of layers in the LSTM encoder/decoder')
parser.add_argument('-rnn_size', type=int, default=500,
                    help='Size of LSTM hidden states')
parser.add_argument('-word_vec_size', type=int, default=500,
                    help='Word embedding sizes')
parser.add_argument('-input_feed', type=int, default=1,
                    help="""Feed the context vector at each time step as
                    additional input (via concatenation with the word
                    embeddings) to the decoder.""")
# parser.add_argument('-residual',   action="store_true",
#                     help="Add residual connections between RNN layers.")
parser.add_argument('-brnn', action='store_true',
                    help='Use a bidirectional encoder')
parser.add_argument('-brnn_merge', default='concat',
                    help="""Merge action for the bidirectional hidden states:
                    [concat|sum]""")

## Optimization options

parser.add_argument('-encoder_type', default='text',
                    help="Type of encoder to use. Options are [text|img].")
parser.add_argument('-batch_size', type=int, default=64,
                    help='Maximum batch size')
parser.add_argument('-max_generator_batches', type=int, default=32,
                    help="""Maximum batches of words in a sequence to run
                    the generator on in parallel. Higher is faster, but uses
                    more memory.""")
parser.add_argument('-epochs', type=int, default=13,
                    help='Number of training epochs')
parser.add_argument('-start_epoch', type=int, default=1,
                    help='The epoch from which to start')
parser.add_argument('-param_init', type=float, default=0.1,
                    help="""Parameters are initialized over uniform distribution
                    with support (-param_init, param_init)""")
parser.add_argument('-optim', default='sgd',
                    help="Optimization method. [sgd|adagrad|adadelta|adam]")
parser.add_argument('-max_grad_norm', type=float, default=5,
                    help="""If the norm of the gradient vector exceeds this,
                    renormalize it to have the norm equal to max_grad_norm""")
parser.add_argument('-dropout', type=float, default=0.3,
                    help='Dropout probability; applied between LSTM stacks.')
parser.add_argument('-curriculum', action="store_true",
                    help="""For this many epochs, order the minibatches based
                    on source sequence length. Sometimes setting this to 1 will
                    increase convergence speed.""")
parser.add_argument('-extra_shuffle', action="store_true",
                    help="""By default only shuffle mini-batch order; when true,
                    shuffle and re-assign mini-batches""")

#learning rate
parser.add_argument('-learning_rate', type=float, default=1.0,
                    help="""Starting learning rate. If adagrad/adadelta/adam is
                    used, then this is the global learning rate. Recommended
                    settings: sgd = 1, adagrad = 0.1, adadelta = 1, adam = 0.001""")
parser.add_argument('-learning_rate_decay', type=float, default=0.5,
                    help="""If update_learning_rate, decay learning rate by
                    this much if (i) perplexity does not decrease on the
                    validation set or (ii) epoch has gone past
                    start_decay_at""")
parser.add_argument('-start_decay_at', type=int, default=8,
                    help="""Start decaying every epoch after and including this
                    epoch""")

#pretrained word vectors

parser.add_argument('-pre_word_vecs_enc',
                    help="""If a valid path is specified, then this will load
                    pretrained word embeddings on the encoder side.
                    See README for specific formatting instructions.""")
parser.add_argument('-pre_word_vecs_dec',
                    help="""If a valid path is specified, then this will load
                    pretrained word embeddings on the decoder side.
                    See README for specific formatting instructions.""")

# GPU
parser.add_argument('-gpus', default=[], nargs='+', type=int,
                    help="Use CUDA on the listed devices.")

parser.add_argument('-log_interval', type=int, default=50,
                    help="Print stats at this interval.")

#seed for generating random numbers
parser.add_argument('-seed',       type=int, default=3435,
                    help="Random seed for generating random numbers (-1 for un-defined the seed; default is 3435); ")


def main():
    opt = parser.parse_args()

    #Sets the seed for generating random numbers
    if (opt.seed>=0):
        torch.manual_seed(opt.seed)

    if torch.cuda.is_available() and not opt.gpus:
        logger.warn("WARNING: You have a CUDA device, so you should probably run with -gpus 0")

    if opt.gpus:
        cuda.set_device(opt.gpus[0])


    logger.info('Options:%s' % repr(opt))

    logger.info("Loading data... START from '%s'" % opt.data)
    start_time = time.time()
    dataset = torch.load(opt.data)
    logger.info("Loading data... END %.2fs" % (time.time() - start_time))



    dict_checkpoint = opt.train_from if opt.train_from else opt.train_from_state_dict
    if dict_checkpoint:
        logger.info("Loading checkpoint... START from '%s'" % dict_checkpoint)
        start_time = time.time()
        checkpoint = torch.load(dict_checkpoint)
        dataset['dicts'] = checkpoint['dicts']
        logger.info("Loading checkpoint... END %.2fs" % (time.time() - start_time))

    logger.info("Creating Data... START")
    start_time = time.time()
    trainData = onmt.Dataset(dataset['train']['src'],
                             dataset['train']['tgt'], opt.batch_size, opt.gpus)
    validData = onmt.Dataset(dataset['valid']['src'],
                             dataset['valid']['tgt'], opt.batch_size, opt.gpus,
                             volatile=True)
    logger.info("Creating Data... END %.2fs" % (time.time() - start_time))

    dicts = dataset['dicts']
    logger.info(' Vocabulary size. source = %d; target = %d' % (dicts['src'].size(), dicts['tgt'].size()))
    logger.info(' Number of training sentences. %d' % len(dataset['train']['src']))
    logger.info(' Maximum batch size. %d' % opt.batch_size)

    logger.info('Building model... START')
    start_time = time.time()

    if opt.encoder_type == "text":
        encoder = onmt.Models.Encoder(opt, dicts['src'])
    elif opt.encoder_type == "img":
        encoder = onmt.modules.ImageEncoder(opt)
        assert("type" not in dataset or dataset["type"] == "img")
    else:
        logger.warn("Unsupported encoder type %s" % (opt.encoder_type))

    decoder = onmt.Models.Decoder(opt, dicts['tgt'])

    generator = nn.Sequential(nn.Linear(opt.rnn_size, dicts['tgt'].size()),nn.LogSoftmax())

    model = onmt.Models.NMTModel(encoder, decoder)

    if opt.train_from:
        logger.info('Loading model... START from checkpoint (opt.train_from) at %s' % opt.train_from)
        start_time2 = time.time()
        logger.debug('checkpoint: %s' % repr(checkpoint))

        chk_model = checkpoint['model']
        model_state_dict = {k: v for k, v in chk_model.state_dict().items() if 'generator' not in k}
        model.load_state_dict(model_state_dict)
        generator_state_dict = chk_model.generator.state_dict()
        generator.load_state_dict(generator_state_dict)
        opt.start_epoch = checkpoint['epoch'] + 1
        logger.info("Loading model... END %.2fs" % (time.time() - start_time2))

    if opt.train_from_state_dict:
        logger.info('Loading model... START from checkpoint (opt.train_from_state_dict) at %s' % opt.train_from_state_dict)
        start_time2 = time.time()
        logger.debug('checkpoint: %s' % repr(checkpoint))
        model.load_state_dict(checkpoint['model'])
        generator.load_state_dict(checkpoint['generator'])
        opt.start_epoch = checkpoint['epoch'] + 1
        logger.info("Loading model... END %.2fs" % (time.time() - start_time2))

    if len(opt.gpus) >= 1:
        model.cuda()
        generator.cuda()
    else:
        model.cpu()
        generator.cpu()

    if len(opt.gpus) > 1:
        model = nn.DataParallel(model, device_ids=opt.gpus, dim=1)
        generator = nn.DataParallel(generator, device_ids=opt.gpus, dim=0)

    model.generator = generator

    if not opt.train_from_state_dict and not opt.train_from:
        logger.info('Initializing model... START')
        start_time2 = time.time()
        for p in model.parameters():
            p.data.uniform_(-opt.param_init, opt.param_init)

        encoder.load_pretrained_vectors(opt)
        decoder.load_pretrained_vectors(opt)
        logger.info("Initializing model... END %.2fs" % (time.time() - start_time2))

        logger.info('Initializing optimizer... START')
        start_time2 = time.time()
        optim = onmt.Optim(
            opt.optim, opt.learning_rate, opt.max_grad_norm,
            lr_decay=opt.learning_rate_decay,
            start_decay_at=opt.start_decay_at
        )
        optim.set_parameters(model.parameters())
        logger.info("Initializing optimizer... END %.2fs" % (time.time() - start_time2))
    else:
        logger.info('Loading optimizer from checkpoint... START')
        start_time2 = time.time()
        optim = checkpoint['optim']
        optim.set_parameters(model.parameters())
        logger.info(optim)

        optim.optimizer.load_state_dict(checkpoint['optim'].optimizer.state_dict())
        logger.info("Loading optimizer... END %.2fs" % (time.time() - start_time2))

    logger.info("Building model... END %.2fs" % (time.time() - start_time))

    nParams = sum([p.nelement() for p in model.parameters()])
    logger.info(' Number of parameters: %d' % nParams)

    logger.info('Training model... START')
    start_time = time.time()
    trainer = Trainer(opt)
    trainer.trainModel(model, trainData, validData,  dataset, optim)
    logger.info('Training model... END %.2fs' % (time.time() - start_time))

if __name__ == "__main__":
    main()

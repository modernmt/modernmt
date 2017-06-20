import math
import time
import copy

import torch
import torch.nn as nn
from torch.autograd import Variable

import onmt
import logging


class Trainer(object):
    def __init__(self, opt):
        self.opt = opt
        # print 'Trainer::Trainer opt:', repr(opt)

        self._logger = logging.getLogger('onmt.Trainer')
        self._logger.info('Options:%s' % repr(self.opt))

    def NMTCriterion(self,vocabSize):
        opt=self.opt
        weight = torch.ones(vocabSize)
        weight[onmt.Constants.PAD] = 0
        crit = nn.NLLLoss(weight, size_average=False)
        if opt.gpus:
            crit.cuda()
        return crit

    def memoryEfficientLoss(self,outputs, targets, generator, crit, eval=False):
        opt=self.opt
        # compute generations one piece at a time
        num_correct, loss = 0, 0
        outputs = Variable(outputs.data, requires_grad=(not eval), volatile=eval)

        batch_size = outputs.size(1)
        outputs_split = torch.split(outputs, opt.max_generator_batches)
        targets_split = torch.split(targets, opt.max_generator_batches)

        for i, (out_t, targ_t) in enumerate(zip(outputs_split, targets_split)):
            out_t = out_t.view(-1, out_t.size(2))
            scores_t = generator(out_t)
            loss_t = crit(scores_t, targ_t.view(-1))
            pred_t = scores_t.max(1)[1]
            num_correct_t = pred_t.data.eq(targ_t.data).masked_select(targ_t.ne(onmt.Constants.PAD).data).sum()
            num_correct += num_correct_t
            loss += loss_t.data[0]
            if not eval:
                loss_t.div(batch_size).backward()

        grad_output = None if outputs.grad is None else outputs.grad.data
        return loss, grad_output, num_correct

    def eval(self, model, criterion, data):
        total_loss = 0
        total_words = 0
        total_num_correct = 0

        model.eval()
        for i in range(len(data)):
            batch = data[i][:-1] # exclude original indices
            outputs = model(batch)
            targets = batch[1][1:]  # exclude <s> from targets
            loss, _, num_correct = self.memoryEfficientLoss(
                outputs, targets, model.generator, criterion, eval=True)
            total_loss += loss
            total_num_correct += num_correct
            total_words += targets.data.ne(onmt.Constants.PAD).sum()

        model.train()
        return total_loss / total_words, float(total_num_correct) / total_words

    def trainModel(self, model_ori, trainData, validData, dataset, optim_ori, save_all_epochs=True, save_last_epoch=False, epochs=None, clone=False):

#        self._logger.info('def Trainer::trainModel trainData:%s' % repr(trainData))

        opt=self.opt

        if epochs:
            opt.epochs = epochs

        model = model_ori
        optim = optim_ori

#NIK: TOCHECK        generator_state_dict = model.generator.module.state_dict() if len(opt.gpus) > 1 else model.generator.state_dict()
#        self._logger.debug('trainModel begin generator_state_dict: %s' % (generator_state_dict))

        model.decoder.attn.applyMask(None) #set the mask to None; required when the same model is trained after a translation

        model.train()

        save_last_epoch = save_last_epoch and not save_all_epochs

        # define criterion of each GPU
        criterion = self.NMTCriterion(dataset['dicts']['tgt'].size())

        start_time = time.time()
        def trainEpoch(epoch):
            if opt.extra_shuffle and epoch > opt.curriculum:
                trainData.shuffle()

            # shuffle mini batch order
            batchOrder = torch.randperm(len(trainData))

            total_loss, total_words, total_num_correct = 0, 0, 0
            report_loss, report_tgt_words, report_src_words, report_num_correct = 0, 0, 0, 0
            start = time.time()
            for i in range(len(trainData)):

                batchIdx = batchOrder[i] if epoch > opt.curriculum else i
                batch = trainData[batchIdx][:-1] # exclude original indices
        
#                self._logger.info('def Trainer::trainEpoch batch:%s' % repr(batch))

                model.zero_grad()
                outputs = model(batch)
                targets = batch[1][1:]  # exclude <s> from targets
                loss, gradOutput, num_correct = self.memoryEfficientLoss(
                    outputs, targets, model.generator, criterion)

                outputs.backward(gradOutput)

                # update the parameters
                optim.step()

                num_words = targets.data.ne(onmt.Constants.PAD).sum()
                report_loss += loss
                report_num_correct += num_correct
                report_tgt_words += num_words
                report_src_words += batch[0][1].data.sum()
                total_loss += loss
                total_num_correct += num_correct
                total_words += num_words
                if i % opt.log_interval == -1 % opt.log_interval:
                    self._logger.info("trainEpoch epoch %2d, %5d/%5d; num_corr: %6.2f; %3.0f src tok; %3.0f tgt tok; acc: %6.2f; ppl: %6.2f; %3.0f src tok/s; %3.0f tgt tok/s; %6.0f s elapsed" %
                          (epoch, i+1, len(trainData),
                           report_num_correct,
                           report_src_words,
                           report_tgt_words,
                           (float(report_num_correct) / report_tgt_words) * 100,
                           math.exp(report_loss / report_tgt_words),
                           report_src_words/(time.time()-start),
                           report_tgt_words/(time.time()-start),
                           time.time()-start_time))

                    report_loss = report_tgt_words = report_src_words = report_num_correct = 0
                    start = time.time()

            return total_loss / total_words, float(total_num_correct) / total_words

        valid_acc, valid_ppl = None, None
        for epoch in range(opt.start_epoch, opt.epochs + 1):

            self._logger.info('Training epoch %g... START' % epoch)
            start_time_epoch = time.time()

            #  (1) train for one epoch on the training set
            train_loss, train_acc = trainEpoch(epoch)
            train_ppl = math.exp(min(train_loss, 100))
            self._logger.info('trainEpoch Epoch %g Train loss: %g perplexity: %g accuracy: %g' % (epoch, train_loss,train_ppl,(float(train_acc)*100)))

            if validData:
                #  (2) evaluate on the validation set
                valid_loss, valid_acc = self.eval(model, criterion, validData)
                valid_ppl = math.exp(min(valid_loss, 100))
                self._logger.info('trainModel Epoch %g Validation loss: %g perplexity: %g accuracy: %g' % (epoch, valid_loss,valid_ppl,(float(valid_acc)*100)))

                #  (3) update the learning rate
                optim.updateLearningRate(valid_loss, epoch)

                self._logger.info("trainModel Epoch %g Decaying learning rate to %g" % (epoch, optim.lr))

            # model_state_dict = model.module.state_dict() if len(opt.gpus) > 1 else model.state_dict()
            # model_state_dict = {k: v for k, v in model_state_dict.items() if 'generator' not in k}
            # generator_state_dict = model.generator.module.state_dict() if len(opt.gpus) > 1 else model.generator.state_dict()
            #
            # #  (4) drop a checkpoint
            # checkpoint = {
            #     'model': model_state_dict,
            #     'generator': generator_state_dict,
            #     'dicts': dataset['dicts'],
            #     'opt': opt,
            #     'epoch': epoch,
            #     'optim': optim
            # }


            if save_all_epochs or save_last_epoch:
                model_state_dict = model.module.state_dict() if len(opt.gpus) > 1 else model.state_dict()
                model_state_dict = {k: v for k, v in model_state_dict.items() if 'generator' not in k}
                generator_state_dict = model.generator.module.state_dict() if len(opt.gpus) > 1 else model.generator.state_dict()

                #  (4) drop a checkpoint
                checkpoint = {
                    'model': model_state_dict,
                    'generator': generator_state_dict,
                    'dicts': dataset['dicts'],
                    'opt': opt,
                    'epoch': epoch,
                    'optim': optim
                }

                generator_state_dict = model.generator.module.state_dict() if len(opt.gpus) > 1 else model.generator.state_dict()
                # self._logger.debug('trainModel Epoch:%g checkpoint.generator: %s' % (epoch, repr(checkpoint['generator'])))
                # self._logger.debug('trainModel Epoch:%g generator_state_dict: %s' % (epoch, generator_state_dict))

                if valid_acc is not None:
                    torch.save(checkpoint,'%s_acc_%.2f_ppl_%.2f_e%d.pt' % (opt.save_model, 100*valid_acc, valid_ppl, epoch))
                else:
                    torch.save(checkpoint,'%s_acc_NA_ppl_NA_e%d.pt' % (opt.save_model, epoch))


            self._logger.info('Training epoch %g... END %.2fs' % (epoch, time.time() - start_time_epoch))

        #
        # print 'def Trainer::trainModel END generator:', repr(generator_state_dict)
        # for name, param in sorted(generator_state_dict.items()):
        #         print ('def Trainer::trainModel END generator_state_dict name',name)
        #         print ('def Trainer::trainModel END generator_state_dict own_state[name]',generator_state_dict[name])
        #
        # print 'def Trainer::trainModel END model_state_dict:', repr(model_state_dict)
        # for name, param in sorted(model_state_dict.items()):
        #         print ('def Trainer::trainModel END model_state_dict name',name)
        #         print ('def Trainer::trainModel END model_state_dict own_state[name]',model_state_dict[name])


#        model_state_dict = model.module.state_dict() if len(opt.gpus) > 1 else model.state_dict()
#        model_state_dict = {k: v for k, v in model_state_dict.items() if 'generator' not in k}
#        generator_state_dict = model.generator.module.state_dict() if len(opt.gpus) > 1 else model.generator.state_dict()

        #  (4) drop a checkpoint
#        checkpoint = {
#                    'model': model_state_dict,
#                    'generator': generator_state_dict,
#                    'dicts': dataset['dicts'],
#                    'opt': opt,
#                    'epoch': epoch,
#                    'optim': optim
#                }

        # self._logger.debug('trainModel returning checkpoint.generator: %s' % (repr(checkpoint['generator'])))
#        self._logger.debug('trainModel returning generator_state_dict: %s' % (repr(generator_state_dict)))


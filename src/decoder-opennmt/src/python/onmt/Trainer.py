import math
import time

import torch
import torch.nn as nn
from torch.autograd import Variable

import onmt


class Trainer(object):
    def __init__(self, opt):
        self.opt = opt

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
        print 'def Trainer::memoryEfficientLoss eval:%s' % (repr(eval))
        outputs = Variable(outputs.data, requires_grad=(not eval), volatile=eval)

        batch_size = outputs.size(1)
        outputs_split = torch.split(outputs, opt.max_generator_batches)
        targets_split = torch.split(targets, opt.max_generator_batches)

        #print 'def Trainer::memoryEfficientLoss outputs_split:%s' % (repr(outputs_split))
        #print 'def Trainer::memoryEfficientLoss targets_split:%s' % (repr(targets_split))

        for i, (out_t, targ_t) in enumerate(zip(outputs_split, targets_split)):
            # print 'def Trainer::memoryEfficientLoss out_t:%s' % (repr(out_t))
            # print 'def Trainer::memoryEfficientLoss targ_t:%s' % (repr(targ_t))
            out_t = out_t.view(-1, out_t.size(2))
            # print 'def Trainer::memoryEfficientLoss out_t:%s' % (repr(out_t))
            scores_t = generator(out_t)
            # print 'def Trainer::memoryEfficientLoss scores_t:%s' % (repr(scores_t))
            loss_t = crit(scores_t, targ_t.view(-1))
            # print 'def Trainer::memoryEfficientLoss loss_t:%s' % (repr(loss_t))
            pred_t = scores_t.max(1)[1]
            # print 'def Trainer::memoryEfficientLoss pred_t:%s' % (repr(pred_t))
            num_correct_t = pred_t.data.eq(targ_t.data).masked_select(targ_t.ne(onmt.Constants.PAD).data).sum()
            num_correct += num_correct_t
            loss += loss_t.data[0]
            if not eval:
                loss_t.div(batch_size).backward()

        grad_output = None if outputs.grad is None else outputs.grad.data
        print 'def Trainer::memoryEfficientLoss loss:%s' % (repr(loss))
        print 'def Trainer::memoryEfficientLoss grad_output:%s' % (repr(grad_output))
        print 'def Trainer::memoryEfficientLoss num_correct:%s' % (repr(num_correct))
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
        return total_loss / total_words, total_num_correct / total_words

    def trainModel(self, model, trainData, validData, dataset, optim, save_all_epochs=True, save_last_epoch=False, epochs=None):
        opt=self.opt
        if epochs:
            opt.epochs = epochs

        print opt

        print(model)

        model.train()

        save_last_epoch = save_last_epoch and not save_all_epochs
        # print 'save_all_epochs:%s save_last_epoch:%s' % (save_all_epochs, save_last_epoch)
        # print 'opt.start_epoch:%s opt.epochs:%s' % (opt.start_epoch, opt.epochs)

        # define criterion of each GPU
        criterion = self.NMTCriterion(dataset['dicts']['tgt'].size())
        print 'def Trainer::trainModel criterion:%s' % repr(criterion)
        print 'def Trainer::trainModel len(trainData):%d' % len(trainData)

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
                print 'def Trainer::trainEpoch epoch:%d i:%d batchIdx:%d batch:%s' % (epoch, i, batchIdx, batch)

                model.zero_grad()
                outputs = model(batch)
                targets = batch[1][1:]  # exclude <s> from targets
                print 'def Trainer::trainEpoch epoch:%d  i:%d batchIdx:%d outputs:%s' % (epoch, i, batchIdx, repr(outputs))
                print 'def Trainer::trainEpoch epoch:%d  i:%d batchIdx:%d targets:%s' % (epoch, i, batchIdx, repr(targets))
                loss, gradOutput, num_correct = self.memoryEfficientLoss(
                    outputs, targets, model.generator, criterion)

                print 'def Trainer::trainEpoch loss:%s' % (repr(loss))
                print 'def Trainer::trainEpoch num_correct:%s' % (repr(num_correct))

                outputs.backward(gradOutput)
                #print 'def Trainer::trainEpaoch i:%d batchIdx:%d outputs after backwards:%s' % (i, batchIdx, repr(outputs))

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
                print("def Trainer::trainEpoch epoch %2d, %5d/%5d; acc: %6.2f; ppl: %6.2f; %3.0f src tok/s; %3.0f tgt tok/s; %6.0f s elapsed" %
                          (epoch, i+1, len(trainData),
                           (float(report_num_correct) / report_tgt_words) * 100,
                           math.exp(report_loss / report_tgt_words),
                           report_src_words/(time.time()-start),
                           report_tgt_words/(time.time()-start),
                           time.time()-start_time))
                print("def Trainer::trainEpoch epoch %2d, %5d/%5d; num_corr: %6.2f; %3.0f src tok; %3.0f tgt tok; %6.0f s elapsed" %
                          (epoch, i+1, len(trainData),
                           report_num_correct,
                           report_src_words,
                           report_tgt_words,
                           time.time()-start_time))
                if i % opt.log_interval == -1 % opt.log_interval:
                    print("def Trainer::trainEpoch epoch %2d, %5d/%5d; acc: %6.2f; ppl: %6.2f; %3.0f src tok/s; %3.0f tgt tok/s; %6.0f s elapsed" %
                          (epoch, i+1, len(trainData),
                           report_num_correct / report_tgt_words * 100,
                           math.exp(report_loss / report_tgt_words),
                           report_src_words/(time.time()-start),
                           report_tgt_words/(time.time()-start),
                           time.time()-start_time))

                    report_loss = report_tgt_words = report_src_words = report_num_correct = 0
                    start = time.time()

            return total_loss / total_words, total_num_correct / total_words

        print('def Trainer::trainModel Train opt.start_epoch: %g opt.epochs: %g' % (opt.start_epoch,opt.epochs))
        for epoch in range(opt.start_epoch, opt.epochs + 1):
            print('def Trainer::trainModel Train epoch: %g' % (epoch))
            # print('')

            #  (1) train for one epoch on the training set
            # print("Actual learning rate to %g" % optim.lr)
            train_loss, train_acc = trainEpoch(epoch)
            train_ppl = math.exp(min(train_loss, 100))
            print('def Trainer::trainModel Train loss: %g' % train_loss)
            print('def Trainer::trainModel Train perplexity: %g' % train_ppl)
            print('def Trainer::trainModel Train accuracy: %g' % (train_acc*100))

            print('def Trainer::trainModel optim.lr:', repr(optim.lr))

            if validData:
                #  (2) evaluate on the validation set
                valid_loss, valid_acc = self.eval(model, criterion, validData)
                valid_ppl = math.exp(min(valid_loss, 100))
                print('def Trainer::trainModel Validation loss: %g' % valid_loss)
                print('Validation perplexity: %g' % valid_ppl)
                print('Validation accuracy: %g' % (valid_acc*100))

                #  (3) update the learning rate
                #optim.updateLearningRate(valid_loss, epoch)
            else:
                #NIK REMOVE AFTER DEBUGGING
                valid_acc = 1.0
                valid_ppl = 1.0

            # valid_acc = 1.0
            # valid_ppl = 1.0

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

            if save_all_epochs or save_last_epoch:
                torch.save(checkpoint,'%s_acc_%.2f_ppl_%.2f_e%d.pt' % (opt.save_model, 100*valid_acc, valid_ppl, epoch))
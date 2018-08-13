import argparse
import os
import sys

import time
import tensorflow as tf
import logging

def finalize_model(str_checkpoints, model_dir, n_checkpoints=None, gpus=None):

    logging.basicConfig(format='%(asctime)-15s [%(levelname)s] - %(message)s',
                        level=logging.DEBUG)
    logger = logging.getLogger('finalize_model')

    # Get the checkpoints list from flags and run some basic checks.
    checkpoints = [c.strip() for c in str_checkpoints.split(",")]
    checkpoints = [c for c in checkpoints if c]

    print "checkpoints:%s" % (checkpoints)
    # Read variables from all checkpoints and average them.
    var_list = tf.contrib.framework.list_variables(checkpoints[0])
    var_values = {name: np.zeros(shape) for name, shape in var_list if not name.startswith("global_step")}

    for checkpoint in checkpoints:
        reader = tf.contrib.framework.load_checkpoint(checkpoint)

        for name in var_values:
            tensor = reader.get_tensor(name)
            var_values[name] += tensor

    for name in var_values:  # Average.
        var_values[name] /= len(checkpoints)

    if gpus is not None:
        logger.log(logging.INFO, "finalize_model gpus:%s" % (gpus))
        logger.log(logging.INFO, "finalize_model gpus.split(','):%s" % (gpus.split(',')))
        logger.log(logging.INFO, "finalize_model gpus.split(',')[0]:%s" % (gpus.split(',')[0]))

        device='/device:GPU:%d' % gpus.split(',')[0]\
    else:
        device='/cpu:0'
    print "finalize_model device:%s" % (device)
    with tf.device(device):
    # with tf.device('/device:GPU:%d' % gpus.split(',')[0] if gpus is not None else '/cpu:0'):
        tf_vars = [tf.get_variable(name, shape=var.shape, dtype=var.dtype) for name, var in var_values.iteritems()]
        placeholders = [tf.placeholder(v.dtype, shape=v.shape) for v in tf_vars]
        assign_ops = [tf.assign(v, p) for (v, p) in zip(tf_vars, placeholders)]
        tf.Variable(0, name="global_step", trainable=False, dtype=tf.int64)
    tf_vars = [tf.get_variable(name, shape=var.shape, dtype=var.dtype) for name, var in var_values.iteritems()]
    placeholders = [tf.placeholder(v.dtype, shape=v.shape) for v in tf_vars]
    assign_ops = [tf.assign(v, p) for (v, p) in zip(tf_vars, placeholders)]
    tf.Variable(0, name="global_step", trainable=False, dtype=tf.int64)
    saver = tf.train.Saver(tf.global_variables(), save_relative_paths=True)



def main(argv):
    train_dir=argv[1]
    output_dir=argv[2]
    n_checkpoints = argv[3]
    finalize_model(train_dir, output_dir, n_checkpoints, gpus)

if __name__ == '__main__':
    main(sys.argv)

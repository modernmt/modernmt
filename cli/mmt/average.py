import argparse
import os
import sys

import tensorflow as tf
import logging
import numpy as np
import six

def finalize_model(str_checkpoints, output_dir, n_checkpoints=None, gpus=None):

    logging.basicConfig(format='%(asctime)-15s [%(levelname)s] - %(message)s',
                        level=logging.DEBUG)
    logger = logging.getLogger('finalize_model')

    # Get the checkpoints list from flags and run some basic checks.
    checkpoints = [c.strip() for c in str_checkpoints.split(",")]
    checkpoints = [c for c in checkpoints if c]

    print "checkpoints:%s" % (checkpoints)
    # Read variables from all checkpoints and average them.
    var_list = tf.contrib.framework.list_variables(checkpoints[0])
    var_dtypes = {}
    var_values = {name: np.zeros(shape) for name, shape in var_list if not name.startswith("global_step")}

    for checkpoint in checkpoints:
        reader = tf.contrib.framework.load_checkpoint(checkpoint)

        for name in var_values:
            tensor = reader.get_tensor(name)
            var_dtypes[name] = tensor.dtype
            var_values[name] += tensor
        tf.logging.info("Read from checkpoint %s", checkpoint)

    for name in var_values:  # Average.
        var_values[name] /= len(checkpoints)

    if gpus is not None: # if gpu computation is allowed use the first
        gpu = gpus.split(',')[0]
        device = '/device:GPU:%s' % (gpu)
    else:
        gpu = None
        device = '/cpu:0'

    with tf.device(device):
        tf_vars = [ tf.get_variable(v, shape=var_values[v].shape, dtype=var_dtypes[name]) for v in var_values ]
        placeholders = [tf.placeholder(v.dtype, shape=v.shape) for v in tf_vars]
        assign_ops = [tf.assign(v, p) for (v, p) in zip(tf_vars, placeholders)]
        global_step = tf.Variable(0, name="global_step", trainable=False, dtype=tf.int64)
    saver = tf.train.Saver(tf.global_variables(), save_relative_paths=True)

    # Build a model consting only of variables, set them to the average values.
    # model_output_path = output_dir
    if not os.path.isdir(output_dir):
        os.makedirs(output_dir)

    session_config = tf.ConfigProto(allow_soft_placement=True)
    session_config.gpu_options.allow_growth = True
    if gpu is not None:
        session_config.gpu_options.force_gpu_compatible = True
        session_config.gpu_options.visible_device_list = str(gpu)

    sess = tf.Session(config=session_config)
    sess.run(tf.initialize_all_variables())
    for p, assign_op, (name, value) in zip(placeholders, assign_ops, six.iteritems(var_values)):
        sess.run(assign_op, {p: value})
    saver.save(sess, os.path.join(output_dir, 'model-avg'), global_step=global_step)

    # Use the built saver to save the averaged checkpoint.
    tf.logging.info("Averaged checkpoints saved in %s", output_dir)



def main(argv):
    train_dir=argv[1]
    output_dir=argv[2]
    n_checkpoints = argv[3]
    gpus = argv[4]
    finalize_model(train_dir, output_dir, n_checkpoints, gpus)

if __name__ == '__main__':
    main(sys.argv)

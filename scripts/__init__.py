import os

__author__ = 'Davide Caroselli'

__self_dir = os.path.dirname(os.path.realpath(__file__))

MMT_VERSION = '0.1-SNAPSHOT'
MMT_ROOT = os.path.abspath(os.path.join(__self_dir, os.pardir))
BIN_DIR = os.path.join(MMT_ROOT, 'bin')
ENGINES_DIR = os.path.join(MMT_ROOT, 'engines')
DATA_DIR = os.path.join(MMT_ROOT, 'data')
MMT_JAR = os.path.join(BIN_DIR, 'mmt-' + MMT_VERSION + ".jar")
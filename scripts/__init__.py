import os

__author__ = 'Davide Caroselli'

__self_dir = os.path.dirname(os.path.realpath(__file__))

MMT_VERSION = '0.1-SNAPSHOT'

MMT_ROOT = os.path.abspath(os.path.join(__self_dir, os.pardir))
ENGINES_DIR = os.path.join(MMT_ROOT, 'engines')
OPT_DIR = os.path.join(MMT_ROOT, 'opt')
LIB_DIR = os.path.join(MMT_ROOT, 'lib')
DATA_DIR = os.path.join(MMT_ROOT, 'data')
BUILD_DIR = os.path.join(MMT_ROOT, 'build')

BIN_DIR = os.path.join(OPT_DIR, 'bin')

MMT_JAR = os.path.join(BUILD_DIR, 'mmt-' + MMT_VERSION + ".jar")
MMT_LIBS = BUILD_DIR

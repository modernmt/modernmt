import sys
from cli.libs import shell, fileutils
from os.path import dirname, abspath, join

__author__ = 'David Madl'

MMT_HOME = abspath(dirname(dirname(__file__)))


def execute(cmd):
    shell.execute(cmd, stdout=sys.stdout)


def git_checkout(branch):
    execute('git checkout {}'.format(branch))
    execute('git submodule init')
    execute('git submodule update')


def build_all():
    # see INSTALL.md
    with fileutils.chdir(join(MMT_HOME, 'vendor')) as cd:
        # Create MMT submodules resources
        execute('make res')

        # Compile MMT submodules
        execute('make')
        execute('make install')

    with fileutils.chdir(join(MMT_HOME, 'src')) as cd:
        # Finally compile your MMT distribution
        execute('mvn clean install')

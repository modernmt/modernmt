import os
from subprocess import call

__author__ = 'Davide Caroselli'

root_folder = os.path.realpath(os.path.join(__file__, os.path.pardir))
java_folder = os.path.join(root_folder, 'src', 'RESTInterface', 'target')

# classpath = [os.path.join(libs_folder, jar) for jar in os.listdir(libs_folder) if jar.endswith('.jar')]
# classpath.append(os.path.join(java_folder, 'mmt-rest-0.1-SNAPSHOT.jar'))
# classpath = [os.path.join(java_folder, 'libs', '*'), os.path.join(java_folder, 'mmt-rest-0.1-SNAPSHOT.jar')]
classpath = [os.path.join(java_folder, 'mmt-0.1-SNAPSHOT.jar')]
sysprop = {
    'mmt.engines.path': os.path.join(root_folder, 'engines'),
    'mmt.tokenizer.models.path': os.path.join(root_folder, 'data', 'tokenizer', 'models'),
    'java.library.path': os.path.join(root_folder, 'src', 'Decoder', 'target'),
}

command = ['java', '-cp', ':'.join(classpath)]
for key, value in sysprop.iteritems():
    command.append('-D' + key + '=' + value)

command.append('eu.modernmt.rest.cli.Main')

command.append('-e')
command.append('worker-default')

call(command)

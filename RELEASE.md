# Changes since last release

## Changes to the APIs

* bla bla bla
* bla bla bla
* bla bla ble

# Current Known Bug and Limitations

- It only supports latin languages.
- context and query need to be tokenized before input.
- incremental training not implemented yet.

# Release 0.10.0

## Major Features and Improvements

### Tokenizers in XX languages 

* Merged Moses tokenizers (22 languages) and developed tokenizers for extra 20 languages adapting Lucene tokenizers to machine translation requirements.

* Packed all tokenizers into a single binary (tokenize.xxx).

### Distributed MMT

* Merged Moses tokenizers (22 languages) and developed tokenizers for extra 20 languages adapting Lucene tokenizers to machine translation requirements.

### Added Support for Mac OSX

* bla bla bla bla.

## Bug fixes

* Lots of fixes to documentation and tutorials, many contributed
  by the public.

* XX closed issues on github issues.

## Backwards-incompatible changes

* APIs.
* Requirements

# Release 0.9.0 - Deprecated

Initial release of MMT, done during the Co-Development Week in Trento. Jun 2014.

## Multi-engines support

Starting from release 0.10, MMT supports multiple engines running on the same machine: you can specify the name of the engine with option "-n" as follows:

./create-engine -i example/train-data -s en -t it -n example

To create the new engine called "example" and the command:

./server start example

to start the "example" engine. By default the "server" script will choose 3 avalilable random ports, if you want to specify custom ports you can use the following command:

./server start example 8000 8001 8002

If you omit the engine name, "default" will be used instead.

## MERT script

Starting from release 0.10 in MMT is available a custom "mert" script that optimizes the Moses weights using the context-string during translation process.
You can run the MERT process with the command:

./mert -i example/dev-data -s en -t it -n example

It will automatically replace the moses.ini file with the optimized one.
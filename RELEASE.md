# Changes since last release

## Changes to the APIs

* bla bla bla
* bla bla bla
* bla bla ble

# Current Known Bug and Limitations

- It only supports 42 languages. List here...
- context and query need to be tokenized before input.
- incremental training not implemented yet.

# Release 0.11

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

* 0.11 has a completely new paradigm and is no longer compatible with previous models and mmt script syntax.

# Release 0.10 - Deprecated

Initial release of MMT, done during the Co-Development Week in Trento. Jun 2014.

## Multi-engines support

MMT supports multiple engines running on the same machine: you can specify the name of the engine with option "-n".

## MERT script

Custom "mert" script that optimizes the Moses weights using the context-string during translation process.

You can run the MERT process with the command:

./mert -i example/dev-data -s en -t it -n example

It will automatically replace the moses.ini file with the optimized one.

# Release 0.9 - Deprecated

Original release after the co-development week in Trento.
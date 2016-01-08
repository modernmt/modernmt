# Changes since last release

Nothing.

# Current known bugs and limitations

- Incremental training not implemented yet.
- Rsyncing on distributed MMT disabled. Copy files manually.
- Distruted MMT still does not support fail-over if a node is down.

# Release 0.11

## Major Features and Improvements

### Tokenizers in 42 languages 

* Merged Moses tokenizers (22 languages) and developed tokenizers for extra 20 languages adapting public available tokenizers to machine translation requirements.

* Packed all tokenizers into a single Java binary.

### Distributed MMT

You can run MMT on multiple servers, load balancing supported.

## Bug fixes

* Many bug fixes in training process, tuning and context analysis

## Backwards-incompatible changes

* 0.11 has a completely new paradigm and is no longer compatible with previous models and mmt script syntax.

# Release 0.10 - Deprecated

## Major Features and Improvements

### Multi-engines support

MMT supports multiple engines running on the same machine: you can specify the name of the engine with option "-n".

### MERT script

Custom "mert" script that optimizes the Moses weights using the context-string during translation process.

You can run the MERT process with the command:

./mert -i example/dev-data -s en -t it -n example

It will automatically replace the moses.ini file with the optimized one.

# Release 0.9 - Deprecated

Original release after the co-development week in Trento - June 2015.

# MMT 0.9 - ALPHA RELEASE 

## About MMT
MMT is a context-aware, incremental and distributed general purpose Machine Translation technology.

MMT goal is to make MT easy to adopt and scale.

With MMT you don't need anymore to train multiple custom engines, you can push all your data to a single engine that will automatically and in real-time adapt to the context you provide.
MMT aims to deliver the quality of a custom engine and the low sparsity of your all data combined.

You can find more information on: http://www.modermmt.eu


## About this Release
This application is the binary version of MMT (open source distribution expected by 2016). 

This MMT release will allow you to create an MT engine, available via a REST API, given your training data (folder with line aligned text files)
Ex. domain1.en domain1.it domain2.en domain2.it 
In general:
<domain-id>.<2 letters iso lang code|5 letters RFC3066>

Note: domain-id must be [a-zA-Z0-9] only without spaces.

## Your first translation with MMT

Check the system requirements in the paragraph below.

### Create an Engine

```bash
# mmt start 
# mmt train --train_data example-data/
```

### Translate via API

http://localhost:8000/?text=party&context=President&source=en&target=fr

### Translate via Command Line

```bash
# mmt translate "<text to translate>" "<context>"
```

### To Stop an engine
```
# mmt stop
```

## Your distributed system

Let's distribute MMT to a second machine. Engine will be synced from the master instance and translation request will be load balanced.

```bash
# mmt start 
# mmt train --train_data example-data/
```

## How to prepare your data

MMT uses standard sentence aligned corpora, optionally divided into files by domain. 
Just put that bunch of files into a folder.

eg.
data/microsoft.en
data/microsoft.fr
data/europarl.en
data/europarl.fr
data/wmt10.en
data/wmt10.fr

In general:
<domain-id>.<2 letters iso lang code|5 letters RFC3066>

Note: domain-id must be [a-zA-Z0-9] only without spaces.

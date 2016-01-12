# MMT 0.11 - ALPHA RELEASE for Ubuntu 14.04 

## About MMT
MMT is a context-aware, incremental and distributed general purpose Machine Translation technology.

MMT goal is to make MT easy to adopt and scale.

With MMT you don't need anymore to train multiple custom engines for each of your domains/projects/customers, you can push all your data to a single engine that will automatically and in real-time adapt to the context you provide.

MMT aims to deliver the quality of a custom engine and the low sparsity of your all data combined.

You can find more information on: http://www.modermmt.eu


## About this Release

This application is the binary version of MMT (open source distribution expected by the end 2016). 

This MMT release will allow you to create an MT engine, available via a REST API, given your training data (folder with line aligned text files)

Intro video: http://87k.eu/lk9l

## Installation

Read [INSTALL.md](INSTALL.md)

## Your first translation with MMT

### Create an Engine

We included a very small dataset, just to verify that training works.

```bash
./mmt create en it examples/data/train
```

### Start an existing engine

```bash
./mmt start
```
use **stop** for stopping it.

### Translate via API

```
curl "http://localhost:8000/translate?q=world&context=computer" | python -mjson.tool
```

You will get:

```
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   107  100   107    0     0    624      0 --:--:-- --:--:-- --:--:--   625
{
    "context": [
        {
            "id": "ibm",
            "score": 0.050385196
        },
        {
            "id": "europarl",
            "score": 0.0074931374
        }
    ],
    "translation": "mondo"
}
```

## Increasing the quality

### Creating a large translation model

You can create a 1B words engine in around 4 hours of training using 16 Cores and 30GB of RAM.

If you want to try, you can download the [WMT 10 Corpus](http://www.statmt.org/wmt10/training-giga-fren.tar) corpus from here:

```
wget http://www.statmt.org/wmt10/training-giga-fren.tar
```

Untar the archive and place the unzipped giga-fren.release2.XX corpus in a training directory (eg. wmt-train-dir) and run:

```bash
./mmt create en fr wmt-train-dir
```

The corpus contains 575,799,111 source tokens and 1,247,735,635 total words.

Training statistics:
```
Speed          :  41,791 words/second
Total time     :  29,159s
  - Tokenization   :   5,801s (will improve in v0.12)
  - Cleaning       :   1,205s
  - Context Index  :      95s
  - Lang Model     :   8,180s
  - Model (Suffix) :  13,878s

```

#### More parallel data

If you need more data there is a good collection here:

http://opus.lingfil.uu.se


### MMT Tuning (Expert)

MMT quality can be increased by tuning the parameters providing unseen translation examples. 

```
./mmt tune examples/data/dev
```

This dev data used to tune the small engine created with the example data will take around 10 minutes. 
After the tuning translation requests will use the new parameters. No other action required.

Tuning speed depends on many factors:
 - Translation speed (bigger model, slower translations);
 - Number of sentences as dev set for tuning;
 - Luck. How close the random initial parameters are to the convergence.

Expect a few days for a 1B words model with 1000 sentences used for tuning.

## MMT distributed (Expert)

Let's distribute MMT to a second machine. 
Make sure port 8000 is open on the master and 5000 and 5001 on both the master and the slave.

Login into the new machine and run

```bash 
./mmt start --master ubuntu:pass123@3.14.15.92
```

Where *ubuntu* and *pass123* are your ssh credentials to the master machine (ip *3.14.15.92*).

If you're running your experiments on Amazon, copy your .pem file to the second machine and run the command as:

```
./mmt start --master ubuntu@3.14.15.92 --master-pem /path/to/master-credentials.pem
```

Query the master, the requests are load balanced across the istances:

```
curl "http://3.14.15.92:8000/translate?q=world&context=computer" | python -mjson.tool
```

**That's all folks!**

### Distributed MMT Notes

The engine files will be synced from the master and translation requests will be load balanced across the 2 instances.
Only the master will respond to the Translation API and distribute load.

If you updated the model on the master, just stop and start the slave and the model data will be rsynced again.

> **Note: rsyncing  of the models has been temporarily disabled in 0.11 and models files have to be copied manually. To test the release please contact davide.caroselli@translated.net**

## How to prepare your data

MMT uses standard sentence aligned corpora, optionally divided into files by domain. 

Example:
```
data/microsoft.en
data/microsoft.fr
data/europarl.en
data/europarl.fr
data/wmt10.en
data/wmt10.fr
```

In general:
```
domain-id.(2 letters iso lang code|5 letters RFC3066)
```

Note: domain-id must be [a-zA-Z0-9] only, without spaces.

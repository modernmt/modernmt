# MMT 0.14 - Release for Ubuntu 14.04 

## About MMT
MMT is a context-aware, incremental and distributed general purpose Machine Translation technology.

MMT is:
- Simple to use, fast to train, and easy to scale with respect to domains, data, and users.
- Trained by pooling all available domains/projects/customers data and translation memories in one folder.
- Queried by providing the sentence to be translated and optionally some context text.

MMT's goal is to deliver the quality of multiple custom engines by adapting on the fly to the provided context.

You can find more information on: http://www.modernmt.eu

## About this Release

This release allows you to create an MT engine, from a collection of line aligned parallel data or TMX files, 
that can be queried via a [REST API](https://github.com/ModernMT/MMT/wiki/API-Documentation).

Intro video: http://87k.eu/lk9l

## Your first translation with MMT

### Installation

Read [INSTALL.md](INSTALL.md)

The distribution includes a small dataset (folder `examples/data/train`) to train and test translations from 
English to Italian in three domains. 

### Create an engine

```bash
$ ./mmt create en it examples/data/train
```

### Start the engine

```bash
$ ./mmt start
```
You can stop it with the command `stop`.

### Start translating

Let's now use the command-line tool `mmt` to query the engine with the sentence *hello world* and context *computer*:
```
$ ./mmt translate --context computer "hello world"

ModernMT Translate command line
>> Context: ibm 87%, europarl 13%

>> hello mondo
```
Next, we are going to improve the partial translation `hello mondo`.

*Note:* You can query MMT directly via REST API, to learn more on how to do it, visit the [Translate API](https://github.com/ModernMT/MMT/wiki/API-Translate) page in the project Wiki.


### Improve translation quality with new data

Let's now add a contribution to te existing engine, **without** need for retraining, in order to improve the previous translation. We will use again the command-line tool `mmt`:
```
./mmt add ibm "hello Mike!" "ciao Mike!"
```
And now repeat the previous translation query: the engine has just learned a new word and the result is immediately visible.
```
$ ./mmt translate --context computer "hello world"

ModernMT Translate command line
>> Context: ibm 87%, europarl 13%

>> ciao mondo
```

## Evaluating quality

How is your engine performing vs the commercial state-of-the-art technologies?

Should I use Google Translate or ModernMT given this data? 

Evaluate helps you answer these questions.

Before training, MMT has removed sentences corresponding to 1% of the training set (or up to 1200 lines at most).
During evaluate these sentences are used to compute the BLUE Score and Matecat Post-Editing Score against the MMT and Google Translate.

With your engine running, just type:
```
./mmt evaluate
```
The typical output will be
```
Testing on 980 sentences...

Matecat Post-Editing Score:
  MMT              : 75.10 (Winner)
  Google Translate : 73.40 | API Limit Exeeded | Connection Error

BLEU:
  MMT              : 37.50 (Winner)
  Google Translate : 36.10 | API Limit Exeeded | Connection Error

Translation Speed:
  MMT              :  1.75s per sentence
  Google Translate :  0.76s per sentence
  
```

If you want to test on a different test-set just type:
```
./mmt evaluate --path path/to/your/test-set
```

*Notes:* To run Evaluate you need internet connection for Google Translate API and the Matecat Post-Editing Score API.
MMT comes with a limited Google Translate API key. 

Matecat kindly provides unlimited-fair-usage, access to their API to MMT users.

You can select your Google Translate API Key by typing:
```
./mmt evaluate --gt-key YOUR_GOOGLE_TRANSLATE_API_KEY
```

If you don't want to use Google Translate just type a random key.

## What's next?

#### Create an engine from scratch
Following this README you have learned the basic usage of ModernMT. Most users would be interested in creating their own engine with their own data, you can find more info in the Wiki [Create an engine from scratch](https://github.com/ModernMT/MMT/wiki/Create-an-engine-from-scratch)

#### See API Documentation
ModernMT comes with built-in REST API that allows the user to control every single feature of MMT via a simple and powerful interface. You can find the [API Documentation](https://github.com/ModernMT/MMT/wiki/API-Documentation) in the [ModernMT Wiki](https://github.com/ModernMT/MMT/wiki).

#### Run ModernMT cluster
You can setup a cluster of MMT nodes in order to load balancing translation requests. In fact also tuning and evaluation can be drastically speed-up if runned on an MMT cluster.

You can learn more on the Wiki page [MMT Cluster](https://github.com/ModernMT/MMT/wiki/MMT-Cluster).

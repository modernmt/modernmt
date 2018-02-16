# MMT 2.4 - Neural Adaptive Machine Translation
We think that artificial intelligence is going to be the next big thing in our nearby future. It will bring humanity to a new era of access and organization of information.
Language translation is probably the most complex of the human tasks for a machine to learn but it is also the one with the greatest potential to make the world a single family.

With this project we want to give our contribution to the evolution of machine translation toward singularity.
We want to consolidate the current state of the art into a single easy to use product, evolve it and keeping it an open to integrate the greatest opportunities in machine intelligence like deep learning.

To achieve our goals we need a better MT technology that is able to extract more from data, adapt to context and easy to deploy. We know that the challenge is big, but the reward is potentially so big that we think it is worth trying hard.

## About MMT
MMT is a context-aware, incremental and distributed general purpose Neural Machine Translation technology. MMT is:
- Simple to use, fast to train, and easy to scale with respect to domains, data, and users.
- Trained by pooling all available projects/customers data and translation memories in one folder.
- Queried by providing the sentence to be translated and optionally some context text.

MMT's goal is to deliver the quality of multiple custom engines by adapting on the fly to the provided context.

You can find more information on: http://www.modernmt.eu

## About this Release
Release for Ubuntu 16.04

This release strongly improves our **Adaptive Neural MT engine**. 
In addition to the fast Phrase-Based engine, you can now create  a neural engine with the same simplicity, starting from a collection of line aligned parallel data or TMX files; the engine will be accessible via a [REST API](https://github.com/ModernMT/MMT/wiki/API-Documentation).
It also lets you create an MT cluster where identical engines on different machines cooperate in order to increase the system speed and fault tolerance.

## Your first translation with MMT

### Installation

Read [INSTALL.md](INSTALL.md)

The distribution includes a small dataset (folder `examples/data/train`) to train and test translations from 
English to Italian in three domains. 

### Create an engine

In order to create a *phrase-based* engine you can simply:

```bash
$ ./mmt create en it examples/data/train
```

*Note:* if you wish to create an *neural* engine, please add more data to the training set and specify the `--neural` flag:
```bash
$ ./mmt create en it examples/data/train --neural
```
Neural Engine on this tiny example data does not give any resonable output. We reccommend to complete the tutorial and then add more data to try neural.

### Start the engine

```bash
$ ./mmt start
```
You can stop it with the command `stop`.

### Start translating

Let's now use the command-line tool `mmt` to query the engine with the sentence *hello world* and context *computer*:
```
$ ./mmt translate --context "programming language tutorial" "hello world"

ModernMT Translate command line
>> Context: ibm 64%, microsoft 27%, europarl 9%

>> hello mondo
```
Next, we are going to improve the partial translation `hello mondo`.

*Note:* You can query MMT directly via REST API, to learn more on how to do it, visit the [Translate API](https://github.com/ModernMT/MMT/wiki/API-Translate) page in the project Wiki.


### Improve translation quality with new data

Let's now add a contribution to the existing engine, **without** need for retraining, in order to improve the previous translation. We will use again the command-line tool `mmt`:
```
./mmt add ibm "hello Mike!" "ciao Mike!"
```
And now repeat the previous translation query: the engine has just learned a new word and the result is immediately visible.
```
$ ./mmt translate --context "programming language tutorial" "hello world"

ModernMT Translate command line
>> Context: ibm 64%, microsoft 27%, europarl 9%

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

#### Use advanced configurations
If you need to customize the properties and behaviour of your engines, you can specify advanced settings in their configuration files. 
You can learn how on the Wiki page [Advanced Configurations](https://github.com/ModernMT/MMT/wiki/Advanced-Configurations)

# Enterprise Edition

MMT Community is free, is open source and welcomes contributions and donations.
MMT Community is sponsored by its funding members (Translated, FBK, UEDIN and TAUS) and the European Commission. 

We also have an **MMT Enterprise Edition**, managed by the MMT company and not available on GitHub, with some extra features:
- Pre-trained generic and custom models in 9 language pairs (and more to come!) on multiple billion words of premium data.
- Support for cluster of servers for higher throughput, load balancing and high availability. 
- Support for 69 files formats without format loss (Office, Adobe, Localization, etc).
- Enterprise Customer Support via Video Conference Call, Phone and Email on business hours (CET) and optionally 24x7.
- Custom developments billed per hour of work.

MMT Enterprise is avaialble in two cloud-based operating modes:
- **Private**  Privacy of data, adaptivity, low cost for excellent throughput and maintenance, customer support.
- **Confidential** Higher privacy of data, adaptivity, higher cost for throughput and maintenance, customer support.

We can provide support for large **On-Premise** installations, too. For any information please email Marcello Federico, marcello@modenrmt.eu

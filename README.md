<div>
<img src="https://user-images.githubusercontent.com/1674891/43786026-f2043344-9a67-11e8-8434-27c324c37214.png" width="60%"></img>
<h1>Simple. Adaptive. Neural.</h1>
<br>
<div/>

We think that artificial intelligence is going to be the next big thing in our nearby future. It will bring humanity to a new era of access and organization of information.
Language translation is probably the most complex of the human tasks for a machine to learn but it is also the one with the greatest potential to make the world a single family.

With this project we want to give our contribution to the evolution of machine translation toward singularity.
We want to consolidate the current state of the art into a single easy to use product, evolve it and keeping it an open to integrate the greatest opportunities in machine intelligence like deep learning.

To achieve our goals we need a better MT technology that is able to extract more from data, adapt to context and be easy to deploy. We know that the challenge is big, but the reward is potentially so big that we think it is worth trying hard.

## About ModernMT
ModernMT is a context-aware, incremental and distributed general purpose Neural Machine Translation technology based on **Fairseq Transformer model**. ModernMT is:
- Easy to use and scale with respect to domains, data, and users.
- Trained by pooling all available projects/customers data and translation memories in one folder.
- Queried by providing the sentence to be translated and optionally some context text.

ModernMT goal is to deliver the quality of multiple custom engines by adapting on the fly to the provided context.

You can find more information on: http://www.modernmt.eu/

## Your first translation with ModernMT

### Installation

Read [INSTALL.md](INSTALL.md)

The distribution includes a small dataset (folder `examples/data/train`) to train and test translations from 
English to Italian. 

### Create an engine

We will now demonstrate how easy it is to train your first engine with ModernMT. *Please notice* however that the provided training set is tiny and exclusively intended for this demo. If you wish to train a proper engine please follow the instructions provided in this guide: [Create an engine from scratch](https://github.com/ModernMT/MMT/wiki/Create-an-engine-from-scratch).

Creating an engine in ModernMT is this simple:
```bash
$ ./mmt create en it examples/data/train/ --train-steps 10000
```

This command will start a fast training process that will last approximately 20 minutes; **not enough to achieve good translation performance**, but enough to demonstrate its functioning. Please consider that a real training will require much more time and parallel data.

### Start the engine

```bash
$ ./mmt start

Starting engine "default"...OK
Loading models...OK

Engine "default" started successfully

You can try the API with:
	curl "http://localhost:8045/translate?q=world&source=en&target=it&context=computer" | python -mjson.tool

```
You can check the status of the engine with the `status` command like this:

```bash
$ ./mmt status

[Engine: "default"]
    REST API:   running - 8045/translate
    Cluster:    running - port 5016
    Binary log: running - localhost:9092
    Database:   running - localhost:9042
```

and finally, you can stop a running engine with the `stop` command.

### Start translating

Let's now use the command-line tool `mmt` to query the engine with the sentence *This is an example*:
```
$ ./mmt translate "this is an example"
ad esempio, è un esempio
```

Why this translation? An engine trained with so little data, and for so little time is not able to output nothing more than gibberish. Follow these instructions to create a proper engine: [Create an engine from scratch](https://github.com/ModernMT/MMT/wiki/Create-an-engine-from-scratch)

*Note:* You can query ModernMT directly via REST API, to learn more on how to do it, visit the [Translate API](https://github.com/modernmt/modernmt/wiki/API-Translate) page in this project Wiki.


### How to import a TMX file

Importing a TMX file is very simple and fast. We will use again the command-line tool `mmt`:
```
$ ./mmt memory import -x  /path/to/example.tmx
Importing example... [========================================] 100.0% 00:35
IMPORT SUCCESS
```

## Evaluating quality

How is your engine performing compared to the commercial state-of-the-art technologies?
Should I use Google Translate or ModernMT given this data? 

Evaluate helps you answer these questions.

During engine training, ModernMT has automatically removed a subset of sentences corresponding to 1% of the training set (or up to 1200 lines at most).
With `evaluate` command, these sentences are used to compute the BLEU Score and Matecat Post-Editing Score against the ModernMT and Google Translate engines.

With your engine running, just type:

```bash
./mmt evaluate
```

The typical output will be like the following:

```
============== EVALUATION ==============

Testing on 980 lines:

(1/5) Translating with ModernMT...                               DONE in 1m 27s
(2/5) Translating with Google Translate...                       DONE in 1m 3s
(3/5) Preparing data for scoring...                              DONE in 0s
(4/5) Scoring with Matecat Post-Editing Score...                 DONE in 3s
(5/5) Scoring with BLEU Score...                                 DONE in 0s

=============== RESULTS ================

Matecat Post-Editing Score:
  ModernMT            : 57.2 (Winner)
  Google Translate    : 53.9

BLEU Score:
  ModernMT            : 35.4 (Winner)
  Google Translate    : 33.1

Translation Speed:
  Google Translate    : 0.07s per sentence
  ModernMT            : 0.09s per sentence
```

If you want to test on a different test-set just type:
```bash
./mmt evaluate --path path/to/your/test-set
```

*Notes:* To run `evaluate` you need internet connection for Google Translate API and the Matecat Post-Editing Score API.
ModernMT comes with a limited Google Translate API key, Matecat kindly provides unlimited-fair-usage, access to their API to ModernMT users.

You can select your own Google Translate API key by typing:
```bash
./mmt evaluate --gt-key YOUR_GOOGLE_TRANSLATE_API_KEY
```

## What's next?

#### Create an engine from scratch
Following this README you have learned the basic usage of ModernMT. You are now ready to create your engine with your own data; you can find more info in the Wiki [Create an engine from scratch](https://github.com/modernmt/modernmt/wiki/Create-an-engine-from-scratch)

#### See API Documentation
ModernMT comes with built-in REST API that allows the user to control every single feature of the tool via a simple and powerful interface. You can find the [API Documentation](https://github.com/modernmt/modernmt/wiki/API-Documentation) in the [ModernMT Wiki](https://github.com/modernmt/modernmt/wiki).

#### Run ModernMT cluster
You can setup a cluster of ModernMT nodes in order to load balancing translation requests.
You can learn more on the Wiki page [ModernMT Cluster](https://github.com/modernmt/modernmt/wiki/MMT-Cluster).

#### Use advanced configurations
If you need to customize the properties and behaviour of your engines, you can specify advanced settings in their configuration files. 
You can learn how on the Wiki page [Advanced Configurations](https://github.com/modernmt/modernmt/wiki/Advanced-Configurations)

# ModernMT Enterprise Edition

**ModernMT is free and Open Source**, and it welcomes contributions and donations. ModernMT is sponsored by its funding members (Translated, FBK, UEDIN and TAUS) and the European Commission.

**ModernMT Enterprise Edition** is our cloud solution for professional translators and enterprises. It is proprietary, and it includes an improved adaptation algorithm, "crafted" with months of optimization and fine-tuning of the system. Moreover, our Enterprise Edition comes with top-quality baseline models trained on billions of high-quality training data.

In a nutshell **ModernMT Enterprise Edition** offers:
- Higher quality. A top-notch adaptation algorithm refined with our inner knowledge of the tool.
- Designed for intensive datacenter usage. 4x cheaper per MB of text translated.
- Pre-trained generic and custom models in 60 language pairs on multiple billion words of premium data.
- Support for cluster of servers for higher throughput, load balancing and high availability. 
- Support for 71 files formats without format loss (Office, Adobe, Localization, etc).
- Enterprise Customer Support via Video Conference Call, Phone and Email on business hours (CET) and optionally 24x7.
- Custom developments billed per hour of work.

For any information please email us at info@modernmt.eu

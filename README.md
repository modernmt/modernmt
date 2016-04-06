# MMT 0.12 - Release for Ubuntu 14.04 

## About MMT
MMT is a context-aware, incremental and distributed general purpose Machine Translation technology.

MMT is simple to use, fast to train, and easy to scale with respect to domains, data, and users.

MMT is trained by pooling all available domains/projects/customers data and translation memories in one folder.

MMT is queried by providing the sentence to be translated and some context text.

MMT's goal is to deliver the quality of multiple custom engines by adapting on the fly to the provided context.

You can find more information on: http://www.modermmt.eu


## About this Release

This MMT release will allow you to create an MT engine, available via a REST API, given your training data (folder with line aligned text files)

Intro video: http://87k.eu/lk9l

## Your first translation with MMT

### Installation

Read [INSTALL.md](INSTALL.md)

### Create an engine

We included a very small dataset (in folder ./examples) to test translations from English to Italian in different domains. 
You create an engine with the command: 

```bash
> ./mmt create en it examples/data/train
```

### Start the engine

You start the engine with the command:
```bash
> ./mmt start
```
when you are done, you can stop it with the command **stop**.

### Start translating via API

```
> curl "http://localhost:8045/translate?q=world&context=computer" | python -mjson.tool
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

#### Input format

MMT support XML input type for translations. XML tags are extracted from the source text, and re-inserted in the translation in the right position, based on the translation alignments.

During the pre-processing:

* XML Tags are identified and extracted from the text.
* Pure text is then de-escaped: XML entities are replaced with the actual literal (e.g. **&amp;lt;** is replaced with char **&lt;**).

The text is then translated by the decoder. During the post-processing step:

* Text is then escaped following the XML conventions. Characters **&quot;**, **&apos;**, **&lt;**, **&gt;** and **&amp;** are escaped in **&amp;quot;**, **&amp;apos;**, **&amp;lt;**, **&amp;gt;** and **&amp;amp;**.
* XML Tags are positioned in the translation based on the alignments. Tag's content is kept untouched.

See the following example:

* **Input**:         ```You&apos;ll see the <div id="example-div">example</div>!```
* **Preprocessed**:  ```You 'll see the example !```
* **Translation**:   ```Vedrai l' esempio !```
* **Postprocessed**: ```Vedrai l&apos;<div id="example-div">esempio</div>!```

### Evaluating Quality

How is your engine performing vs the commercial state-of-the-art technologies?

Should I use Google Translate, Bing or MMT given this data? 

Evaluate helps you answer these questions.

Before training, MMT has removed sentences corresponding to 1% of the training set and up to 1200.
During evaluate this sentences are used to compute the BLUE Score and Matecat Post-Editing Effort against the MMT and Google Translate.

With your engine running, just type:
```
./mmt evaluate
```
The typical output will be
```
Testing on 980 sentences...

Matecat Post-Editing Effort:
  MMT              : 75.10 (Winner)
  Google Translate : 73.40 | API Limit Exeeded | Connection Error
  Bing Translator  : Coming in next MMT release

BLEU:
  MMT              : 37.50 (Winner)
  Google Translate : 36.10 | API Limit Exeeded | Connection Error
  Bing Translator  : Coming in next MMT release

Translation Speed:
  MMT              :  12 words/s
  Google Translate :  100 words/s
  
```

If you want to test on a different Test Set just type:
```
./mmt evaluate --path your-folder/your-test-set
```

Notes:
To run Evaluate you need internet connection for Google Translate API and the Matecat post-editing Effort API.
MMT comes with a limited Google Translate API key. 

Matecat kindly provides unlimited-fair-usage, access to their API to MMT users.

You can select your Google Translate API Key by typing:
```
./mmt evaluate --gt-key YOUR_GOOGLE_TRANSLATE_API_KEY
```

If you don't want to use Google Translate just type a random key.


## Increasing the quality

### How to prepare your data

The easy way to increase the quality is to add more in-domain data.

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

#### Get more parallel data

If you need more data there is a good collection here:

http://opus.lingfil.uu.se

#### Add monolingual data

The MMT language model is created with the target of the parallel data and extra monolingual data provided by the user.

To add monolingual data just add a LM-NAME.target_lang to the train folder.

Example:
```
data/my_monolingual_data.fr
data/microsoft.en
data/microsoft.fr
data/europarl.en
data/europarl.fr
data/wmt10.en
data/wmt10.fr
```

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
  - Tokenization   :   5,801s
  - Cleaning       :   1,205s
  - Context Index  :      95s
  - Lang Model     :   8,180s
  - Model (Suffix) :  13,878s

```

### MMT Tuning (Optional)

MMT quality can be increased by tuning the parameters providing unseen translation examples. 

```
./mmt tune --path examples/data/dev
```

This dev data used to tune the small engine created with the example data will take around 10 minutes. 
After the tuning translation requests will use the new parameters. No other action required.

Tuning speed depends on many factors:
 - Translation speed (bigger model, slower translations);
 - Number of sentences as dev set for tuning;
 - Luck. How close the random initial parameters are to the convergence.

Expect a few days for a 1B words model with 1000 sentences used for tuning.

Tuning and evaluate runs also on a distributed MMT.  

## MMT distributed (Optional)

Translation, Tuning and Evaluate can run on a MMT cluster to drastically reduce the time they take.
Training cannot run on an MMT cluster.

Let's distribute MMT to a second machine. 
Make sure port 8045 is open on the master and 5016 and 5017 on both the master and the slave.

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
curl "http://3.14.15.92:8045/translate?q=world&context=computer" | python -mjson.tool
```

**That's all folks!**

### Distributed MMT Notes

The engine files will be synced from the master and translation requests will be load balanced across the 2 instances.
Only the master will respond to the Translation API and distribute load.

If you updated the model on the master, just stop and start the slave and the model data will be rsynced again.

### API documentation

You can find an exhaustive description of all the available APIs in the document [docs/README.md](docs/README.md).

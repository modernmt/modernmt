# MMT 0.12 - Release for Ubuntu 14.04 

## About MMT
MMT is a context-aware, incremental and distributed general purpose Machine Translation technology.

MMT is:
- Simple to use, fast to train, and easy to scale with respect to domains, data, and users.
- Trained by pooling all available domains/projects/customers data and translation memories in one folder.
- Queried by providing the sentence to be translated and some context text.

MMT's goal is to deliver the quality of multiple custom engines by adapting on the fly to the provided context.

You can find more information on: http://www.modernmt.eu


## About this Release

This release allows you to create an MT engine, from a collection of line aligned parallel data or TMX files, 
that can be queried via a REST API.

Intro video: http://87k.eu/lk9l

## Your first translation with MMT

### Installation

Read [INSTALL.md](INSTALL.md)

The distribution includes a small dataset (folder ./examples/data/train) to train and test translations from 
English to Italian in three domains. 

### Create an engine

```bash
> ./mmt create en it examples/data/train
```

### Start the engine

```bash
> ./mmt start
```
You can stop it with the command **stop**.

### Start translating via API

Let us query MMT with the word *world* in the context *computer*:
```
> curl "http://localhost:8045/translate?q=world&context=computer" | python -mjson.tool
```

MMT will return a json structure showing the translation and the context similarity scores for each domain: 

```
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   145  100   145    0     0  14777      0 --:--:-- --:--:-- --:--:-- 16111
{
    "data": {
        "context": [
            {
                "id": "ibm",
                "score": 0.048597444
            },
            {
                "id": "europarl",
                "score": 0.007114725
            }
        ],
        "decodingTime": 1,
        "translation": "mondo"
    },
    "status": 200
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

Should I use Google Translate or ModernMT given this data? 

Evaluate helps you answer these questions.

Before training, MMT has removed sentences corresponding to 1% of the training set and up to 1200.
During evaluate this sentences are used to compute the BLUE Score and Matecat Post-Editing Score against the MMT and Google Translate.

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

If you want to test on a different Test Set just type:
```
./mmt evaluate --path your-folder/your-test-set
```

Notes:
To run Evaluate you need internet connection for Google Translate API and the Matecat Post-Editing Score API.
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
./mmt tune
```

This dev data used to tune the small engine created with the example data will take around 10 minutes. 
After the tuning translation requests will use the new parameters. No other action required.

Tuning speed depends on many factors:
 - Translation speed (bigger model, slower translations);
 - Number of sentences as dev set for tuning;
 - Luck. How close the random initial parameters are to the convergence.

Expect a few hours for a 1B words model with 1000 sentences used for tuning.

Tuning and evaluate runs also on a distributed MMT.  

## MMT distributed (Optional)

Translation, Tuning and Evaluate can run on a MMT cluster to drastically reduce the time they take.
Training cannot run on an MMT cluster.

Let's distribute MMT to a second machine. First, make sure ports 5016, 5017 and 8045 are open on both machines

Login into the new machine and run

```bash 
./mmt start --join 172.31.40.212
```

Where *172.31.40.212* is the IP address of the first MMT machine - the one that was already running.

If you're running your experiments on *Amazon*, **you must use your machine's private ip**, not the public one (nor elastic ip if present) otherwise you won't be able to connect the two instances.

You can query the REST API on both machines, the requests are load balanced across the whole cluster:

```
curl "http://172.31.40.212:8045/translate?q=world&context=computer" | python -mjson.tool
curl "http://localhost:8045/translate?q=world&context=computer" | python -mjson.tool
```

### Distributed MMT Notes

The engine files will be synced from one instance to the other and translation requests will be load balanced across the whole cluster.

If you updated the model on a machine, just stop and start the the nodes specifying the up-to-date host in the *--join* option and the model data will be rsynced again.

### API documentation

You can find an exhaustive description of all the available APIs in the document [docs/README.md](docs/README.md).

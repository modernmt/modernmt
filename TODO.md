# Why?

We think that artificial intelligence is going to be the next big thing in our nearby future. It will bring humanity to a new era of access and organization of information.

Language translation is probably the most complex of the human tasks for a machine to learn but it is also the one with the greatest potential to make the world a single family.

With this project we want to give our contribution to the evolution of machine translation toward singularity. 

We want to consolidate the current state of the art into a single easy to use product, evolve it and keeping it an open to integrate the next greatest opportunities in machine intelligence like deep learning.

To achieve our goals we need a better MT technology that is able to extract more from data, adapt to context and easy to deploy. As every AI, it needs data and we are working to create the tools to make all the world translated information available to all. 

We know that the challenge is big, but the reward is potentially so big that we think it is worth trying hard. 

# How?

We aggregated most of the people that created the current state of the art machine translation technology, added great engineers and a few challengers to rethink the problem.

If you fell you can contribute you are welcome to join.

# What? - MMT Dirty Hands Todo

This documents is a recap of the most important activities.

The purpose is keeping vision and consensus on the strategy within the team and inform the users of what to expect next.

Items should be in order of priority.

## Quality

Goal: with 2 billion words perform better than commercially available technology.

- 0.12 - Quality measure against Google Translate and Microsoft integrated in the training process. Measure first!
- 0.12 - Allowing monolingual LM data input
- 0.12 - Porting Matecat/Moses tag management to MMT.

- 0.14 - Adding more languages and quality to tokenization, now 45.
- 0.14 - More accurate context score and suffix sampling.  

- 0.15 - Adding Optional Human Evaluation and Tuning (BLEU or PEE are very imprecise metrics, few man hours of evaluation can deliver better results). 10 Euro for evaluation via A/B test vs Google Translate, 100 Euro for human tuning via nbest selection.

## Speed

#### Training

Goal: Initial training to stay below 8 hours for each 1B word (36 cores). Make incremental training available.

- Better multi-treads tokenization. +15% in total training speed
- Better LM parallelization. +10% in total training speed.

#### Translate

Goal: with 2 billion words stay below 400ms for the average sentence length (15 words).

- Pruning of models. 
- Better caching.
- Fine tuning the max number LM requests per translation and other parameters.
- With 2B word model, translation time is not below 400ms yet: distribute LM across multiple servers.

## Product Market Fit

### Research

Need Marcello and Philipp input.

### Industry

- Provide more data to the users as baseline. Large enterprises want to customize MT with their data but still having a base model. MS translation Hub does this well. 
- Add support TMX input.

# Donate

MMT is free, is open source and welcomes contributions and donations.

MMT is currently sponsored by its funding members (Translated, FBK, UEDIN and TAUS) and the European Commission. 

For donations, customizations and words of encouragement contact alessandro@translated.net

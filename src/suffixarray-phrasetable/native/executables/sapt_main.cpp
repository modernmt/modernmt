#include <iostream>

#include <mmt/sentence.h>
#include <sapt/PhraseTable.h>
#include <suffixarray/UpdateBatch.h>
#include <suffixarray/SuffixArray.h>

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

void add(UpdateBatch &batch, int index, int size) {
    vector<wid_t> sourceSentence;
    vector<wid_t> targetSentence;
    alignment_t alignment;

    for (int i = 0; i < size; i++) {
        sourceSentence.push_back((unsigned int) ((1000 * index) + i));
        targetSentence.push_back((unsigned int) ((1000 * index) + i + 100));

        if (index % 2 == 0)
            alignment.push_back(make_pair(i, i));
        else
            alignment.push_back(make_pair(i, size - i - 1));
    }

    batch.Add(1, sourceSentence, targetSentence, alignment);
}

void print(sample_t &sample) {
    for (auto word = sample.source.begin(); word != sample.source.end(); ++word) cout << *word << " ";
    cout << "||| ";
    for (auto word = sample.target.begin(); word != sample.target.end(); ++word) cout << *word << " ";
    cout << "||| ";
    for (auto a = sample.alignment.begin(); a != sample.alignment.end(); ++a) cout << a->first << "-" << a->second << " ";
    cout << endl;
}

int main() {
    Options options;
    options.update_max_delay = 1;

    PhraseTable pt("/home/ubuntu/sapt", options);
    SuffixArray *index = (SuffixArray *) pt.__GetSuffixArray();

    // Insert
    UpdateBatch batch(1000, vector<seqid_t>());

    add(batch, 1, 4);
    add(batch, 1, 3);
    add(batch, 2, 5);
    add(batch, 3, 2);

    index->PutBatch(batch);


    // Query
    vector<wid_t> phrase;
    phrase.push_back(1001);
    phrase.push_back(1002);

    vector<sample_t> samples;
    index->GetRandomSamples(1, phrase, 1000, samples);

    for (auto sample = samples.begin(); sample != samples.end(); ++sample)
        print(*sample);

    return 0;
}
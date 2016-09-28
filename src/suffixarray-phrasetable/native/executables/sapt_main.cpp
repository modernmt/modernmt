#include <iostream>

#include <mmt/sentence.h>
#include <sapt/PhraseTable.h>
#include <sapt/CorpusStorage.h>

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

void add(PhraseTable &pt, int index, int size) {
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

    pt.Add(updateid_t(1, (seqid_t) index), 1, sourceSentence, targetSentence, alignment);
}

void print(CorpusStorage &storage, int offset) {
//    vector<wid_t> sourceSentence;
//    vector<wid_t> targetSentence;
//    alignment_t alignment;
//
//    storage.Retrieve(offset, &sourceSentence, &targetSentence, &alignment);
//
//    for (auto word = sourceSentence.begin(); word != sourceSentence.end(); ++word) cout << *word << " ";
//    cout << "||| ";
//    for (auto word = targetSentence.begin(); word != targetSentence.end(); ++word) cout << *word << " ";
//    cout << "||| ";
//    for (auto a = alignment.begin(); a != alignment.end(); ++a) cout << a->first << "-" << a->second << " ";
//    cout << endl;
}

int main() {
    Options options;
    options.update_max_delay = 1;

    PhraseTable pt("/home/ubuntu/sapt", options);

//    add(pt, 1, 4);
//    add(pt, 2, 5);
//    add(pt, 3, 2);
//
//    sleep(2);

    

    return 0;
}
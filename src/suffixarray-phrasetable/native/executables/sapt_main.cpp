#include <iostream>

#include <mmt/sentence.h>
#include <sapt/PhraseTable.h>
#include <sapt/CorpusStorage.h>

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

void add(CorpusStorage &storage, int size, int m) {
    vector<wid_t> sourceSentence;
    vector<wid_t> targetSentence;
    alignment_t alignment;

    for (int i = 0; i < size; i++) {
        sourceSentence.push_back((unsigned int) ((1000 * m) + i));
        targetSentence.push_back((unsigned int) ((1000 * m) + i + 100));

        if (m % 2 == 0)
            alignment.push_back(make_pair(i, i));
        else
            alignment.push_back(make_pair(i, size - i - 1));
    }

    cout << "m=" << m << " offset=" << storage.Append(sourceSentence, targetSentence, alignment) << endl;
}

void print(CorpusStorage &storage, int offset) {
    vector<wid_t> sourceSentence;
    vector<wid_t> targetSentence;
    alignment_t alignment;

    storage.Retrieve(offset, &sourceSentence, &targetSentence, &alignment);

    for (auto word = sourceSentence.begin(); word != sourceSentence.end(); ++word) cout << *word << " ";
    cout << "||| ";
    for (auto word = targetSentence.begin(); word != targetSentence.end(); ++word) cout << *word << " ";
    cout << "||| ";
    for (auto a = alignment.begin(); a != alignment.end(); ++a) cout << a->first << "-" << a->second << " ";
    cout << endl;
}

int main() {
    CorpusStorage storage("/Users/davide/Desktop/sapt/corpus.bin");

//    add(storage, 4, 1);
//    add(storage, 5, 2);
//    add(storage, 2, 3);
//
//    cout << "File size: " << storage.Flush() << endl;
//    cout << endl;

    print(storage, 0);
    print(storage, 60);
    print(storage, 132);
//    print(storage, 192);
//    print(storage, 264);

    return 0;
}
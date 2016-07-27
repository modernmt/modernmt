//
// Created by Davide Caroselli on 26/07/16.
//

#include <vocabulary/Vocabulary.h>
#include <iostream>
#include <util/TokenSource.h>
#include <dirent.h>
#include <vocabulary/VocabularyBuilder.h>

using namespace std;

class UniqueOperator : public Operator {
public:
    virtual void Apply(vector<string> &sentence) override {
        words.insert(sentence.begin(), sentence.end());
    }

    virtual void Collapse(Operator *_other) override {
        UniqueOperator *other = (UniqueOperator *) _other;
        words.insert(other->words.begin(), other->words.end());
    }

    const unordered_set<string> &GetWords() const {
        return words;
    }

private:
    unordered_set<string> words;
};

class UniqueOperatorFactory : public OperatorFactory {
public:
    virtual Operator *NewOperator(size_t index) override {
        return new UniqueOperator();
    }
};

int main(int argc, const char *argv[]) {
    if (argc != 5) {
        cerr << "USAGE: buildvb <model_path> <input_folder> <source_lang> <target_lang>" << endl;
        exit(1);
    }

    string modelPath = argv[1];
    string inputDir = argv[2];
    string sourceLang = argv[3];
    string targetLang = argv[4];

    VocabularyBuilder vocabularyBuilder;
    UniqueOperatorFactory factory;

    DIR *dir = opendir(inputDir.data());
    dirent *dp;

    while ((dp = readdir(dir)) != NULL) {
        string filename = dp->d_name;
        string extension = filename.substr(filename.find_last_of(".") + 1);
        string path = inputDir + '/' + filename;

        if (extension == sourceLang || extension == targetLang) {
            TokenSource source(path, factory);
            UniqueOperator *collapsed = (UniqueOperator *) source.Process();

            vocabularyBuilder.Put(collapsed->GetWords());

            delete collapsed;
        }
    }

    closedir(dir);

    cout << "Vocabulary Size: " << vocabularyBuilder.GetSize() << endl;
    vocabularyBuilder.Flush(modelPath);

    return 0;
}
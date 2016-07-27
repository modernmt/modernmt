//
// Created by Davide Caroselli on 27/07/16.
//

#ifndef MOSESDECODER_TOKENSOURCE_H
#define MOSESDECODER_TOKENSOURCE_H

#include <string>
#include <vector>

using namespace std;

class Operator {
public:
    virtual void Apply(vector<string> &sentence) = 0;

    virtual void Collapse(Operator *other) = 0;
};

class OperatorFactory {
public:
    virtual Operator *NewOperator(size_t index) = 0;
};

class TokenSource {
public:
    TokenSource(string path, OperatorFactory &operatorFactory, size_t threads = 0) :
            path(path), operatorFactory(operatorFactory), threads(threads) {};

    Operator *Process();

private:
    size_t threads;
    string path;
    OperatorFactory &operatorFactory;

    Operator *Map(char *buffer, size_t size);
};


#endif //MOSESDECODER_TOKENSOURCE_H

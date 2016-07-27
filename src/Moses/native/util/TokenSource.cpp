//
// Created by Davide Caroselli on 27/07/16.
//

#include <thread>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <omp.h>
#include <sstream>
#include <iostream>
#include "TokenSource.h"

struct membuf : streambuf {
    membuf(char *begin, size_t size) {
        this->setg(begin, begin, begin + size);
    }
};

void WhitespaceTokenize(string line, vector<string> &output) {
    istringstream iss(line);
    string word;

    while (getline(iss, word, ' ')) {
        output.push_back(word);
    }
}

Operator *TokenSource::Process() {
    struct stat st;
    stat(path.c_str(), &st);
    size_t filesize = (size_t) st.st_size;

    int fd = open(path.c_str(), O_RDONLY, 0);
    if (fd == -1)
        return nullptr;

    char *buffer = (char *)mmap(NULL, filesize, PROT_READ, MAP_PRIVATE | MAP_POPULATE, fd, 0);

    Operator *result = Map(buffer, filesize);

    munmap(buffer, filesize);

    return result;

//    FILE *file = fopen(path.c_str(), "rb");
//    if (file == NULL)
//        return nullptr;
//
//    fseek(file, 0, SEEK_END);
//    size_t size = (size_t) ftell(file);
//    rewind(file);
//
//    char *buffer = new char[size];
//    size_t copySize = fread(buffer, 1, size, file);
//
//    if (copySize != size) {
//        delete buffer;
//        return nullptr;
//    }
//
//    Operator *result = Map(buffer, size);
//    delete buffer;
//
//    return result;
}

Operator *TokenSource::Map(char *buffer, size_t size) {
    size_t threads = this->threads == 0 ? thread::hardware_concurrency() : this->threads;

    size_t pageSize = size / threads;
    vector<Operator *> operators(threads);

    for (size_t i = 0; i < threads; i++)
        operators[i] = operatorFactory.NewOperator(i);

#pragma omp parallel num_threads(threads)
    {
        int ithread = omp_get_thread_num();

        size_t begin = pageSize * ithread;
        size_t end = pageSize * (ithread + 1);

        if (ithread > 0) {
            for (; begin < size; begin++) {
                if (buffer[begin] == '\n') {
                    begin = begin + 1;
                    break;
                }
            }
        }

        if (ithread == threads - 1) {
            end = size;
        } else {
            for (; end < size; end++) {
                if (buffer[end] == '\n') {
                    end = end + 1;
                    break;
                }
            }
        }

        vector<string> tokens;
        membuf page(&buffer[begin], end - begin);
        std::istream in(&page);
        std::string line;

        while (std::getline(in, line)) {
            WhitespaceTokenize(line, tokens);
            operators[ithread]->Apply(tokens);
            tokens.clear();
        }
    }

    for (size_t i = 1; i < threads; i++) {
        operators[0]->Collapse(operators[i]);
        delete operators[i];
    }

    return operators[0];
}

//
// Created by Davide  Caroselli on 16/02/17.
//

#ifndef SAPT_INDEX_EXCEPTION_H
#define SAPT_INDEX_EXCEPTION_H

#include <string>

namespace mmt {
    namespace sapt {

        class index_exception : public std::exception {
        public:
            index_exception(const std::string &msg) : message(msg) {};

            virtual const char *what() const throw() override {
                return message.c_str();
            }

        private:
            std::string message;
        };
    }
}


#endif //SAPT_INDEX_EXCEPTION_H

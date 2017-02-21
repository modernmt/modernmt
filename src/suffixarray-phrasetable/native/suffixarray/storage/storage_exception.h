//
// Created by Davide  Caroselli on 15/02/17.
//

#ifndef SAPT_STORAGE_EXCEPTION_H
#define SAPT_STORAGE_EXCEPTION_H

#include <exception>
#include <string>

namespace mmt {
    namespace sapt {

        class storage_exception : public std::exception {
        public:
            storage_exception(const std::string &msg) : message(msg) {};

            virtual const char *what() const throw() override {
                return message.c_str();
            }

        private:
            std::string message;
        };

    }
}

#endif //SAPT_STORAGE_EXCEPTION_H

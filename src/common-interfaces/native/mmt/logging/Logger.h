//
// Created by david on 25.11.16.
//

#ifndef MMT_LOGGING_LOGGER_H
#define MMT_LOGGING_LOGGER_H

#include <string>
#include <iostream>
#include <sstream>
#include <climits>
#include <jni.h>
#include <unordered_map>

using namespace std;

namespace mmt {
    namespace logging {

        /**
         * Ordered in a way that numeric values can be compared with operator<(),
         * higher priority means more important (reverse ordering compared to old moses1 loglevels!)
        */
        enum Level {
            OFF = 100,
            TRACE = 1,
            DEBUG = 2,
            INFO = 3,
            WARN = 4,
            ERROR = 5,
            FATAL = 6,
            ALL = 0
        };

        class Logger {
        public:

            static void Initialize(JNIEnv *env);

            static Logger *Get(const string &name);

            const string &GetName() const {
                return name;
            }

            const inline bool IsLevelEnabled(const Level level) {
                return level >= this->level;
            }

            const inline bool IsTraceEnabled() {
                return Level::TRACE >= this->level;
            }

            const inline bool IsDebugEnabled() {
                return Level::DEBUG >= this->level;
            }

            const inline bool IsInfoEnabled() {
                return Level::INFO >= this->level;
            }

            const inline bool IsWarnEnabled() {
                return Level::WARN >= this->level;
            }

            const inline bool IsErrorEnabled() {
                return Level::ERROR >= this->level;
            }

            const inline bool IsFatalEnabled() {
                return Level::FATAL >= this->level;
            }

            const inline void _Log(const Level level, const string &message) {
                if (level >= this->level)
                    this->WriteLog(level, message);
            }

            const inline void Trace(const string &message) {
                if (Level::TRACE >= this->level)
                    this->WriteLog(Level::TRACE, message);
            }

            const inline void Debug(const string &message) {
                if (Level::DEBUG >= this->level)
                    this->WriteLog(Level::DEBUG, message);
            }

            const inline void Info(const string &message) {
                if (Level::INFO >= this->level)
                    this->WriteLog(Level::INFO, message);
            }

            const inline void Warning(const string &message) {
                if (Level::WARN >= this->level)
                    this->WriteLog(Level::WARN, message);
            }

            const inline void Error(const string &message) {
                if (Level::ERROR >= this->level)
                    this->WriteLog(Level::ERROR, message);
            }

            const inline void Fatal(const string &message) {
                if (Level::FATAL >= this->level)
                    this->WriteLog(Level::FATAL, message);
            }

        private:
            struct jlogger_t;
            static jlogger_t *jlogger;

            const string name;
            const Level level;

            Logger(const string &name, const Level level);

            const void WriteLog(const Level level, const string &message);

        };

#define IsLogLevelEnabled(prio) (true)

#define Log(prio, msg) do {} while(0)

#define Logd(msg) Log(DEBUG, msg)
#define Logi(msg) Log(INFO, msg)

    }
}

#endif //MMT_LOGGING_LOGGER_H

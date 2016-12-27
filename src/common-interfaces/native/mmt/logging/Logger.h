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

#define LogTrace(logger) mmt::logging::LogStream(logger, mmt::logging::Level::TRACE)
#define LogDebug(logger) mmt::logging::LogStream(logger, mmt::logging::Level::DEBUG)
#define LogInfo(logger) mmt::logging::LogStream(logger, mmt::logging::Level::INFO)
#define LogWarn(logger) mmt::logging::LogStream(logger, mmt::logging::Level::WARN)
#define LogError(logger) mmt::logging::LogStream(logger, mmt::logging::Level::ERROR)
#define LogFatal(logger) mmt::logging::LogStream(logger, mmt::logging::Level::FATAL)

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
            friend class LogStream;

        public:

            static void Initialize(JNIEnv *env);

            Logger(const string &name);

            const string &GetName() const {
                return name;
            }

            const inline bool IsLevelEnabled(const Level level) const {
                return level >= this->level;
            }

            const inline bool IsTraceEnabled() const {
                return Level::TRACE >= this->level;
            }

            const inline bool IsDebugEnabled() const {
                return Level::DEBUG >= this->level;
            }

            const inline bool IsInfoEnabled() const {
                return Level::INFO >= this->level;
            }

            const inline bool IsWarnEnabled() const {
                return Level::WARN >= this->level;
            }

            const inline bool IsErrorEnabled() const {
                return Level::ERROR >= this->level;
            }

            const inline bool IsFatalEnabled() const {
                return Level::FATAL >= this->level;
            }

        private:
            struct jlogger_t;
            static jlogger_t *jlogger;

            const string name;
            const Level level;

            static Level GetLevelForLogger(const string &name);

            inline void Log(const Level level, const string &message) const {
                if (level >= this->level)
                    this->WriteLog(level, message);
            }

            void WriteLog(const Level level, const string &message) const;

        };

        class LogStream : public virtual ostringstream {
        public:
            LogStream(const Logger &logger, Level level);

            virtual ~LogStream();

        private:
            const Logger &logger;
            const Level level;
        };

    }
}

#endif //MMT_LOGGING_LOGGER_H

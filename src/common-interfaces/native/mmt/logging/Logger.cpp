//
// Created by david on 25.11.16.
//

#include "Logger.h"
#include <jni.h>

using namespace mmt;
using namespace mmt::logging;

void _logger_default_handler() {
    Logger logger("terminate");

    exception_ptr eptr = current_exception();

    try {
        if (eptr)
            std::rethrow_exception(eptr);
    } catch (const std::exception &e) {
        LogFatal(logger) << "Unexpected exception: " << e.what();
    }

    LogFatal(logger) << "Terminating process with call abort()";
    abort();
}

struct Logger::jlogger_t {
    JavaVM *jvm;
    jclass _class;
    jmethodID createLoggerMethod;
    jmethodID logMethod;
};

Logger::jlogger_t *Logger::jlogger = nullptr;

void Logger::Initialize(void *_env) {
    JNIEnv *env = (JNIEnv *)_env;

    jlogger_t *jlogger = new jlogger_t();

    jlogger->_class = (jclass) env->NewGlobalRef(env->FindClass("eu/modernmt/logging/NativeLogger"));
    jlogger->createLoggerMethod = env->GetStaticMethodID(jlogger->_class, "createLogger", "(Ljava/lang/String;)I");
    jlogger->logMethod = env->GetStaticMethodID(jlogger->_class, "log", "(Ljava/lang/String;ILjava/lang/String;)V");
    env->GetJavaVM(&jlogger->jvm);

    Logger::jlogger = jlogger;

    set_terminate(_logger_default_handler);
}

Level Logger::GetLevelForLogger(const string &name) {
    if (jlogger == nullptr)
        return Level::OFF;

    JNIEnv *env;
    jlogger->jvm->AttachCurrentThread((void **) &env, NULL);

    jstring jname = env->NewStringUTF(name.c_str());
    jint level = env->CallStaticIntMethod(jlogger->_class, jlogger->createLoggerMethod, jname);
    env->DeleteLocalRef(jname);

    jlogger->jvm->DetachCurrentThread();

    return (Level) level;
}

Logger::Logger(const string &name) : name(name), level(GetLevelForLogger(name)) {
}

void Logger::WriteLog(const Level level, const string &message) const {
    if (jlogger) {
        JNIEnv *env;
        jlogger->jvm->AttachCurrentThread((void **) &env, NULL);

        jstring jname = env->NewStringUTF(name.c_str());
        jstring jmessage = env->NewStringUTF(message.c_str());

        env->CallStaticVoidMethod(jlogger->_class, jlogger->logMethod, jname, (jint) level, jmessage);

        env->DeleteLocalRef(jname);
        env->DeleteLocalRef(jmessage);

        jlogger->jvm->DetachCurrentThread();
    }
}

LogStream::LogStream(const Logger &logger, Level level) : basic_ostringstream(), logger(logger), level(level) {
}

LogStream::~LogStream() {
    this->logger.Log(this->level, this->str());
}

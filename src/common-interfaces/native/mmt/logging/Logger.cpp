//
// Created by david on 25.11.16.
//

#include "Logger.h"

using namespace mmt;
using namespace mmt::logging;

struct Logger::jlogger_t {
    JavaVM *jvm;
    jclass _class;
    jmethodID createLoggerMethod;
    jmethodID logMethod;
};

Logger::jlogger_t *Logger::jlogger = nullptr;

void Logger::Initialize(JNIEnv *env) {
    jlogger_t *jlogger = new jlogger_t();

    jlogger->_class = (jclass) env->NewGlobalRef(env->FindClass("eu/modernmt/logging/NativeLogger"));
    jlogger->createLoggerMethod = env->GetStaticMethodID(jlogger->_class, "createLogger", "(Ljava/lang/String;)I");
    jlogger->logMethod = env->GetStaticMethodID(jlogger->_class, "log", "(Ljava/lang/String;ILjava/lang/String;)V");
    env->GetJavaVM(&jlogger->jvm);

    Logger::jlogger = jlogger;
}

Logger *Logger::Get(const string &name) {
    if (Logger::jlogger == nullptr)
        return new Logger(name, Level::OFF);

    JNIEnv *env;
    jlogger->jvm->AttachCurrentThread((void **) &env, NULL);

    jstring jname = env->NewStringUTF(name.c_str());
    jint level = env->CallIntMethod(nullptr, jlogger->createLoggerMethod, jname);
    env->DeleteLocalRef(jname);

    jlogger->jvm->DetachCurrentThread();

    return new Logger(name, (Level) level);
}

Logger::Logger(const string &name, const Level level) : name(name), level(level) {
}

const void Logger::WriteLog(const Level level, const string &message) {
    if (jlogger) {
        JNIEnv *env;
        jlogger->jvm->AttachCurrentThread((void **) &env, NULL);

        jstring jname = env->NewStringUTF(name.c_str());
        jstring jmessage = env->NewStringUTF(message.c_str());

        env->CallVoidMethod(nullptr, jlogger->logMethod, jname, (jint) level, jmessage);

        env->DeleteLocalRef(jname);
        env->DeleteLocalRef(jmessage);

        jlogger->jvm->DetachCurrentThread();
    }
}

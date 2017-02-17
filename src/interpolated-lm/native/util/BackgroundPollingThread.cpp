//
// Created by Davide  Caroselli on 16/02/17.
//

#include "BackgroundPollingThread.h"

using namespace std;
using namespace mmt;
using namespace mmt::ilm;

BackgroundPollingThread::BackgroundPollingThread(double timeout) : waitTimeout(timeout), running(false) {
}

void BackgroundPollingThread::AwakeBackgroundThread(bool wait) {
    awakeCondition.notify_one();

    if (wait) {
        unique_lock<mutex> lock(awakeMutex);
        awakeCondition.wait(lock);
    }
}

void BackgroundPollingThread::RunInBackground() {
    auto timeout = std::chrono::milliseconds((int64_t) (waitTimeout * 1000.));

    while (running) {
        unique_lock<mutex> lock(awakeMutex);
        awakeCondition.wait_for(lock, timeout);

        if (running)
            BackgroundThreadRun();

        lock.unlock();
        awakeCondition.notify_one();
    }
}

void BackgroundPollingThread::Start() {
    if (!running) {
        running = true;
        backgroundThread = new boost::thread(boost::bind(&BackgroundPollingThread::RunInBackground, this));
    }
}

void BackgroundPollingThread::Stop() {
    if (running) {
        running = false;
        AwakeBackgroundThread(false);

        backgroundThread->join();
        delete backgroundThread;
    }
}

//
// Created by Davide  Caroselli on 14/02/17.
//

#include "BackgroundPollingThread.h"

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

BackgroundPollingThread::BackgroundPollingThread(double timeout) : waitTimeout(timeout), stop(false) {
    backgroundThread = new boost::thread(boost::bind(&BackgroundPollingThread::RunInBackground, this));
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

    while (!stop) {
        unique_lock<mutex> lock(awakeMutex);
        awakeCondition.wait_for(lock, timeout);

        if (!stop)
            BackgroundThreadRun();

        lock.unlock();
        awakeCondition.notify_one();
    }
}

void BackgroundPollingThread::Stop() {
    stop = true;
    AwakeBackgroundThread(false);

    backgroundThread->join();
    delete backgroundThread;
}

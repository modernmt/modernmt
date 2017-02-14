//
// Created by Davide  Caroselli on 14/02/17.
//

#ifndef SAPT_BACKGROUNDPOLLINGTHREAD_H
#define SAPT_BACKGROUNDPOLLINGTHREAD_H

#include <mutex>
#include <condition_variable>
#include <boost/thread.hpp>

namespace mmt {
    namespace sapt {

        class BackgroundPollingThread {
        protected:
            BackgroundPollingThread(double timeout);

            void AwakeBackgroundThread(bool wait);

            virtual void BackgroundThreadRun() = 0;

            inline bool IsRunning() {
                return !stop;
            }

            void Stop();

        private:
            boost::thread *backgroundThread;
            std::mutex awakeMutex;
            std::condition_variable awakeCondition;
            double waitTimeout;
            bool stop;

            void RunInBackground();
        };

    }
}


#endif //SAPT_BACKGROUNDPOLLINGTHREAD_H

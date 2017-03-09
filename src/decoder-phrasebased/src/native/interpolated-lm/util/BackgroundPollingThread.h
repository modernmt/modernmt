//
// Created by Davide  Caroselli on 16/02/17.
//

#ifndef ILM_BACKGROUNDPOLLINGTHREAD_H
#define ILM_BACKGROUNDPOLLINGTHREAD_H

#include <mutex>
#include <condition_variable>
#include <boost/thread.hpp>

namespace mmt {
    namespace ilm {

        class BackgroundPollingThread {
        protected:
            BackgroundPollingThread(double timeout);

            void AwakeBackgroundThread(bool wait);

            virtual void BackgroundThreadRun() = 0;

            inline bool IsRunning() {
                return running;
            }

            void Start();

            void Stop();

        private:
            boost::thread *backgroundThread;
            std::mutex awakeMutex;
            std::condition_variable awakeCondition;
            double waitTimeout;
            bool running;

            void RunInBackground();
        };

    }
}

#endif //ILM_BACKGROUNDPOLLINGTHREAD_H

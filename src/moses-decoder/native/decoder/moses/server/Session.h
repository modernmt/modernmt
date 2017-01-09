// -*- mode: c++; indent-tabs-mode: nil; tab-width: 2 -*-
#pragma once
#include "moses/Util.h"
#include "moses/ContextScope.h"
#include "moses/parameters/AllOptions.h"
#include <sys/time.h>
#include <boost/unordered_map.hpp>

#ifdef WITH_THREADS
#include <boost/thread/shared_mutex.hpp>
#include <boost/thread/locks.hpp>
#endif
namespace MosesServer{
  
  struct Session
  {
    uint64_t const id;
    time_t start_time;
    mutable time_t last_access;
    boost::shared_ptr<Moses::ContextScope> scope; // stores local info

    Session(uint64_t const session_id) 
      : id(session_id)
    { 
      last_access = start_time = time(NULL); 
    }

    bool is_new() const { return last_access == start_time; }

    void setup(std::map<std::string, xmlrpc_c::value> const& params);
  };

  class SessionCache
  {
    mutable boost::shared_mutex m_lock;
    uint64_t m_session_counter;
    boost::unordered_map<uint64_t,Session> m_cache;
  public:

    SessionCache() : m_session_counter(1) {}

    Session&
    operator[](uint32_t id)
    {
      boost::upgrade_lock<boost::shared_mutex> lock(m_lock);
      if (id > 1 || id == 0)
      {
        boost::unordered_map<uint64_t, Session>::iterator m = m_cache.find(id);
        if (m != m_cache.end())
        {
          m->second.last_access = time(NULL);
          return m->second;
        }
      }
      boost::upgrade_to_unique_lock<boost::shared_mutex> xlock(lock);
      if(id != 0)
        id = ++m_session_counter;
      std::pair<uint64_t, Session> foo(id, Session(id));
      return m_cache.insert(foo).first->second;
    }

    Session const&
    at(uint32_t id) {
      boost::upgrade_lock<boost::shared_mutex> lock(m_lock);
      if (id > 1 || id == 0)
      {
        boost::unordered_map<uint64_t, Session>::iterator m = m_cache.find(id);
        if (m != m_cache.end())
        {
          m->second.last_access = time(NULL);
          return m->second;
        }
      }
      assert(0 && "Session.at() failed - wrong session id");
      return *reinterpret_cast<const Session *>(NULL);
    }

    void
    erase(uint32_t const id)
    {
      boost::unique_lock<boost::shared_mutex> lock(m_lock);
      m_cache.erase(id);
    }


  };


}

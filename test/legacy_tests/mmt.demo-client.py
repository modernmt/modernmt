#!/usr/bin/env python

import os
import sys
import threading
import time
from argparse import ArgumentParser

sys.path.insert(0, os.path.abspath(os.path.join(__file__, os.pardir, os.pardir)))
from cli.mmt.cluster import MMTApi

server = None

class Job:
   def __init__(self,session,sentence_id,line):
      self.session = session
      self.line = line
      self.sentence_id = sentence_id
      self.thread = threading.Thread(target=self.run)
      self.thread.daemon = True
      self.thread.start()
      return

   def run(self):
      self.start_time = time.time()
      self.result = self.session.translate(self.line)
      self.end_time = time.time()
      return
   
   def join(self):
      while self.thread.is_alive():
         self.thread.join(timeout=0.1)
      return
      

class Session:
   def __init__(self, host, context=None, document=None, args=None, handler=None):
      self.mmt      = MMTApi(host)
      self.context  = context
      self.args     = args
      self.document = document
      self.handler  = handler
      if context:
         C = self.mmt.get_context_s(context)
         S = self.mmt.create_session(C)
         self.session = S['id']
      else:
         self.session = None
      return
   
   def __del__(self):
      if self.session != None:
         self.mmt.close_session(self.session)
      return

   def translate(self,line):
      return self.mmt.translate(line,session=self.session, nbest=None)

# class job:
#    '''Wrapper around translation request.'''
#    def __init__(self,sid,param):
#       self.sid = sid
#       self.param = param
#       self.thread = threading.Thread(target=self.run)
#       self.thread.daemon = True
#       self.thread.start()
#       return
   
#    def run(self):
#       self.attempts = 1
#       while self.attempts < 3:
#          self.time_of_submission = time.time()
#          try:
#             self.server_response = requests.get(server,self.param)
#             J = self.server_response.json()
#             status = int(J['status'])
#             if status == 200:
#                self.time_of_response = time.time()
#                D = J['data']
#                self.decoding_time = int(D['decodingTime'])/1000.
#                self.translation = D['translation']
#                return
#          except requests.ConnectionError as e:
#             print >>sys.stderr, "SERVER ERROR:", e
#             pass
#          print >>sys.stderr,"Sentence %d, attempt %d: status"%\
#             (self.sid, self.attempts), status
#          print >>sys.stderr, self.server_response.json()
#          self.attempts += 1
#          self.time_of_response = 0
#          pass
#       return

#    def join(self):
#       # thread.join() can't be interrupted. We loop over a join with a timeout
#       # so that we don't have to wait for the thread to finish, e.g. after issuing
#       # a keyboard interrupt
#       while self.thread.is_alive():
#          self.thread.join(timeout=0.1)
   
#    def turnaround(self):
#       self.join()
#       return self.time_of_response - self.time_of_submission

#    pass # end of definition of class job

start_time = time.time()
def print_job(j):
   j.join()
   print "%4d %6.2f %5.2f %5.2f | %s"%\
      (j.sentence_id, time.time() - start_time, j.end_time - j.start_time,
       int(j.result['decodingTime'])/1000., j.result['translation'].encode('utf-8'))
   # print j.result
   return



if __name__ == "__main__":

   text = [line.strip() for line in sys.stdin]

   parser = ArgumentParser()
   parser.add_argument("server")
   parser.add_argument("-d", "--debug", default=True,
                       action="store_true", help="debug")
   args = parser.parse_args(sys.argv[1:])

   session = Session(args.server, context = " ".join(text[:50]))
   jobs = []
   for i in xrange(len(text)):
      while len(jobs) == 250 or (len(jobs) and not jobs[0].thread.is_alive()):
         print_job(jobs.pop(0))
         pass
      jobs.append(Job(session,i,text[i]))
      
   for j in jobs:
      print_job(j)
      pass
   
   

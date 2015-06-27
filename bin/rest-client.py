#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import time
import BaseHTTPServer
import xmlrpclib
import datetime
import urlparse
import json

PORT_NUMBER = int(sys.argv[1])
RPC_PORT = int(sys.argv[2])

rpcProxy = xmlrpclib.ServerProxy("http://localhost:" + `RPC_PORT` + "/RPC2")

class MyHandler(BaseHTTPServer.BaseHTTPRequestHandler):
	def do_GET(s):
		query = urlparse.urlparse(s.path).query
		query = urlparse.parse_qs(query)
		params = {"text": query['text'][0], "context": query['context'][0]}

		result = rpcProxy.translate(params)
		content = json.dumps({"translation": result['text'].strip()})

		s.send_response(200)
		s.send_header("Content-type", "application/json; charset=utf-8")
		s.end_headers()
		s.wfile.write(content.encode("utf-8"))


if __name__ == '__main__':
	server_class = BaseHTTPServer.HTTPServer
	httpd = server_class(('', PORT_NUMBER), MyHandler)
	print time.asctime(), "Server Starts on port %s" % (PORT_NUMBER)
	try:
		httpd.serve_forever()
	except KeyboardInterrupt:
		pass
	httpd.server_close()
	print time.asctime(), "Server Stops on port %s" % (PORT_NUMBER)
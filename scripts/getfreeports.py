#!/usr/bin/env python
# -*- coding: utf-8 -*-

import socket

def getfreeport():
	sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	sck.bind(('', 0))
	addr = sck.getsockname()
	port = addr[1]
	sck.close()

	return port

def isfreeport(port):
	sck = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	error = sck.connect_ex(('', port))
	sck.close()

	return error != 0

ports = []

while (not ports):
	port = getfreeport();
	
	if isfreeport(port + 1) and isfreeport(port + 2):
		ports = [str(port), str(port + 1), str(port + 2)]

print ' '.join(ports)
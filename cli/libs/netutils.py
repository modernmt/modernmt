import socket

__author__ = 'Davide Caroselli'


def get_free_tcp_port():
    tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tcp.bind(('', 0))
    addr, port = tcp.getsockname()
    tcp.close()
    return port


def is_free(port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    result = s.connect_ex(('127.0.0.1', port))

    if result == 0:
        return False
    else:
        s.close()
        return True


def is_ip(target_format, address):
    try:
        socket.inet_pton(target_format, address)
        return True
    except socket.error:
        return False


def is_ipv4(address):
    return is_ip(socket.AF_INET, address)


def is_ipv6(address):
    return is_ip(socket.AF_INET6, address)


def resolve_ip(hostname):
    try:
        return socket.gethostbyname(hostname)
    except socket.gaierror:
        return None

import argparse

from cli import ensure_engine_exists, ensure_node_running, ensure_node_not_running
from cli.mmt.engine import EngineNode, Engine


def parse_args_start(argv=None):
    parser = argparse.ArgumentParser(description='Start the ModernMT server on this machine', prog='mmt start')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')
    parser.add_argument('-v', '--verbosity', dest='verbosity', help='log verbosity (0 = only severe errors, '
                                                                    '3 = finest logging)', default=None)
    parser.add_argument('-p', '--api-port', dest='api_port', metavar='API_PORT',
                        help='the public REST Api port. (default is 8045)', default=None, type=int)
    parser.add_argument('--cluster-port', dest='cluster_port', metavar='CLUSTER_PORT',
                        help='the network port used internally by the cluster for communication between '
                             'Cluster nodes. (default is 5016)', default=None, type=int)
    parser.add_argument('--binlog-port', '--datastream-port', dest='binlog_port', metavar='BINLOG_PORT',
                        help='the network port used by BinaryLog, currently implemented with Kafka '
                             '(default is 9092)', default=None, type=int)
    parser.add_argument('--db-port', dest='db_port', metavar='DB_PORT',
                        help='the network port used by the DB, currently implemented with Cassandra '
                             '(default is 9042)', default=None, type=int)
    parser.add_argument('--join-leader', dest='leader', metavar='NODE_IP', default=None,
                        help='use this option to join this node to an existent cluster. '
                             'NODE is the IP of the remote host to connect to.')
    parser.add_argument('-d', '--remote-debug', action='store_true', dest='remote_debug',
                        help='setting this option allows Java to connect for remote debug '
                             '(intended only for development purpose)')
    parser.add_argument('--log-file', dest='log_file', default=None, help='custom location for node log file')

    return parser.parse_args(argv)


def parse_args_stop(argv=None):
    parser = argparse.ArgumentParser(description='Stop the local instance of ModernMT server', prog='mmt stop')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')
    parser.add_argument('-f', '--forced', action='store_true', dest='forced', default=False,
                        help='forced stop. By default ModernMT will run a graceful shutdown trying to minimize the '
                             'impact on the operations (i.e. fulfilling all pending translation requests before '
                             'stopping the system). If this flag is specified the system will be forcefully halted.')

    return parser.parse_args(argv)


def parse_args_status(argv=None):
    parser = argparse.ArgumentParser(description='Show the ModernMT server status', prog='mmt status')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name', default=None)

    return parser.parse_args(argv)


def main_start(argv=None):
    args = parse_args_start(argv)

    engine = Engine(args.engine)
    ensure_engine_exists(engine)
    node = EngineNode(engine)
    ensure_node_not_running(node)

    success = False

    try:
        # start the ClusterNode
        print('Starting engine "%s"...' % engine.name, end='', flush=True)
        node.start(api_port=args.api_port,
                   cluster_port=args.cluster_port,
                   binlog_port=args.binlog_port,
                   db_port=args.db_port,
                   leader=args.leader,
                   verbosity=args.verbosity,
                   remote_debug=args.remote_debug,
                   log_file=args.log_file)
        node.wait('JOINED')
        print('OK', flush=True)

        print('Loading models...', end='', flush=True)
        node.wait('RUNNING')
        print('OK', flush=True)

        # the node has started
        print('\nEngine "%s" started successfully\n' % engine.name)

        if node.api is not None:
            print('You can try the API with:\n'
                  '\tcurl "%s/translate?q=world&source=en&target=it&context=computer"'
                  ' | python -mjson.tool\n' % node.api.base_path)
        success = True
    except Exception:
        print('FAIL', flush=True)
        raise
    finally:
        if not success:
            node.stop()


def main_stop(argv=None):
    args = parse_args_stop(argv)

    engine = Engine(args.engine)
    ensure_engine_exists(engine)
    node = EngineNode(engine)
    ensure_node_running(node)

    try:
        print('Halting engine "%s"...' % engine.name, end='', flush=True)
        node.stop(force=args.forced)
        print('OK', flush=True)
    except Exception:
        print('FAIL', flush=True)
        raise


def main_status(argv=None):
    args = parse_args_status(argv)

    if args.engine is None:
        engines = Engine.list()
    else:
        engine = Engine(args.engine)
        ensure_engine_exists(engine)

        engines = [engine]

    if len(engines) == 0:
        print('No engine found.')

    for engine in engines:
        node = EngineNode(engine)
        node_running = node.running
        node_state = node.state

        rest_api_s = ('running - %s/translate' % node_state.api_port) \
            if node_running else 'stopped'
        cluster_s = ('running - port %d' % node_state.cluster_port) \
            if node_running else 'stopped'
        binlog_s = ('running - %s:%d' % (node_state.binlog_host, node_state.binlog_port)) \
            if node_running else 'stopped'
        database_s = ('running - %s:%d' % (node_state.database_host, node_state.database_port)) \
            if node_running else 'stopped'

        print('[Engine: "%s"]' % engine.name)
        print('    REST API:   %s' % rest_api_s)
        print('    Cluster:    %s' % cluster_s)
        print('    Binary log: %s' % binlog_s)
        print('    Database:   %s' % database_s)

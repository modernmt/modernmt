import argparse
import os
import sys
import time

from cli import ensure_engine_exists, ensure_node_running, ensure_node_has_api, CLIArgsException
from cli.mmt.engine import Engine, EngineNode
from cli.utils.progressbar import Progressbar


def _load_node(engine_name):
    engine = Engine(engine_name)
    ensure_engine_exists(engine)

    node = EngineNode(engine)
    ensure_node_running(node)
    ensure_node_has_api(node)

    return node


def main_list(argv=None):
    parser = argparse.ArgumentParser(description='List all memories', prog='mmt memory list')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')

    args = parser.parse_args(argv)

    node = _load_node(args.engine)
    memories = sorted(node.api.get_all_memories(), key=lambda m: m['id'])

    for memory in memories:
        print('%d: %s' % (memory['id'], memory['name']))
    print()
    print('Total: %s memories' % len(memories))


def main_create(argv=None):
    parser = argparse.ArgumentParser(description='Create a new empty memory', prog='mmt memory create')
    parser.add_argument('name', help='the name of the new memory')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')

    args = parser.parse_args(argv)

    node = _load_node(args.engine)
    memory = node.api.create_memory(args.name)

    print('%d: %s' % (memory['id'], memory['name']))


def main_delete(argv=None):
    parser = argparse.ArgumentParser(description='Delete a memory by id', prog='mmt memory delete')
    parser.add_argument('id', type=int, help='the id of the memory to delete')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')

    args = parser.parse_args(argv)

    node = _load_node(args.engine)
    node.api.delete_memory(args.id)


def main_rename(argv=None):
    parser = argparse.ArgumentParser(description='Rename a memory by id', prog='mmt memory rename')
    parser.add_argument('id', type=int, help='the id of the memory to rename')
    parser.add_argument('name', help='the new memory name')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')

    args = parser.parse_args(argv)

    node = _load_node(args.engine)
    memory = node.api.rename_memory(args.id, args.name)

    print('%d: %s' % (memory['id'], memory['name']))


def main_add(argv=None):
    parser = argparse.ArgumentParser(description='Add contribution to an existent memory', prog='mmt memory add')

    parser.add_argument('memory', help='the id of the memory', type=int)
    parser.add_argument('source', metavar='SOURCE_SENTENCE', help='the source sentence of the contribution')
    parser.add_argument('target', metavar='TARGET_SENTENCE', help='the target sentence of the contribution')
    parser.add_argument('-s', '--source', dest='source_lang', metavar='SOURCE_LANGUAGE', default=None,
                        help='the source language (ISO 639-1), can be omitted if engine is monolingual')
    parser.add_argument('-t', '--target', dest='target_lang', metavar='TARGET_LANGUAGE', default=None,
                        help='the target language (ISO 639-1), can be omitted if engine is monolingual')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')

    args = parser.parse_args(argv)

    node = _load_node(args.engine)

    # Infer default arguments
    if args.source_lang is None or args.target_lang is None:
        if len(node.engine.languages) > 1:
            raise CLIArgsException(parser,
                                   'Missing language. Options "-s" and "-t" are mandatory for multilingual engines.')
        args.source_lang, args.target_lang = node.engine.languages[0]

    node.api.append_to_memory(args.source_lang, args.target_lang, args.memory, args.source, args.target)

    print('Contribution added to memory %d' % args.memory)


def main_import(argv=None):
    parser = argparse.ArgumentParser(description='Import content, TMX or Parallel files, into a new or existing memory')
    parser.add_argument('-x', '--tmx-file', dest='tmx', metavar='TMX_FILE', help='TMX file to import', default=None)
    parser.add_argument('-p', '--parallel-files', dest='parallel_file', default=None, nargs=2,
                        help='source and target file (file extension must be source and target languages)')
    parser.add_argument('-e', '--engine', dest='engine', help='the engine name, \'default\' will be used if absent',
                        default='default')
    parser.add_argument('--id', type=int, default=None, dest='memory',
                        help='the optional destination memory id (by default, a new Memory is created)')

    args = parser.parse_args(argv)

    if args.tmx is None and args.parallel_file is None:
        raise CLIArgsException(parser, 'missing one of the following options: "-x" or "-p"')

    node = _load_node(args.engine)
    corpus_name = os.path.splitext(os.path.basename(args.tmx or args.parallel_file[0]))[0]

    new_memory = None
    if args.memory is None:
        new_memory = node.api.create_memory(corpus_name)
        args.memory = new_memory['id']

    progressbar = Progressbar(label='Importing %s' % corpus_name)
    progressbar.start()

    try:
        if args.tmx is not None:
            job = node.api.import_into_memory(args.memory, tmx=args.tmx)
        else:
            src_file, tgt_file = args.parallel_file
            src_lang, tgt_lang = os.path.splitext(src_file)[1][1:], os.path.splitext(tgt_file)[1][1:]

            job = node.api.import_into_memory(args.memory, source_file=src_file, target_file=tgt_file,
                                              source_lang=src_lang, target_lang=tgt_lang)

        progressbar.set_progress(job['progress'])

        while job['progress'] != 1.0:
            time.sleep(1)
            job = node.api.get_import_job(job['id'])
            progressbar.set_progress(job['progress'])

        progressbar.complete()
        print('IMPORT SUCCESS')
    except BaseException as e:
        if new_memory is not None:
            try:
                node.api.delete_memory(new_memory['id'])
            except:
                pass

        progressbar.abort(repr(e))
        print('IMPORT FAILED')

        raise


def main(argv=None):
    argv = argv or sys.argv[1:]

    actions = {
        'list': main_list,
        'create': main_create,
        'delete': main_delete,
        'rename': main_rename,
        'add': main_add,
        'import': main_import,
    }

    parser = argparse.ArgumentParser(description='Manage memory resources', prog='mmt memory', add_help=False)
    parser.add_argument('action', metavar='ACTION', choices=actions.keys(), help='{%(choices)s}', nargs='?')
    parser.add_argument('-h', '--help', dest='help', action='store_true', help='show this help message and exit')

    if len(argv) == 0:
        parser.print_help()
        exit(1)

    command = argv[0]
    args = argv[1:]

    if command in actions:
        actions[command](args)
    else:
        parser.print_help()
        exit(1)

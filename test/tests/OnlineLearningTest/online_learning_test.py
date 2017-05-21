import json
from time import sleep

from utils import log
from utils.mmt import *
from utils.test_assert import *

MMT_HOME = os.path.abspath(os.path.join(__file__, os.pardir, os.pardir, os.pardir, os.pardir))
RES = os.path.abspath(os.path.join(__file__, os.pardir, 'res'))


# Utils ================================================================================================================

def copy_engine(engine_from, engine_to):
    source_folder = os.path.join(MMT_HOME, 'engines', engine_from)
    dest_folder = os.path.join(MMT_HOME, 'engines', engine_to)
    shutil.rmtree(dest_folder, ignore_errors=True)

    shutil.copytree(source_folder, dest_folder)


def copy_engine_model(model, engine_from, engine_to):
    if model == 'context':
        model = 'context'
    elif model == 'ilm':
        model = os.path.join('decoder', 'lm')
    else:
        model = os.path.join('decoder', 'sapt')

    source_folder = os.path.join(MMT_HOME, 'engines', engine_from, 'models', model)
    dest_folder = os.path.join(MMT_HOME, 'engines', engine_to, 'models', model)

    shutil.rmtree(dest_folder, ignore_errors=True)
    shutil.copytree(source_folder, dest_folder)


# Tests ================================================================================================================

def test_single_contribution():
    copy_engine('_test_base', 'default')
    init_size = mmt_engine_size()

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
    assert_equals(mmt_api_translate('hello world'), 'hello mondo')
    mmt_api_append(2, 'hello Mike!', 'ciao Mike!')
    assert_equals(mmt_api_translate('hello world'), 'ciao mondo')
    assert_equals(mmt_api_count_domains(), 3)

    mmt_stop()
    new_size = mmt_engine_size()
    assert_size_increased(new_size, init_size)

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(0, 1))
    sleep(2)
    mmt_stop()

    assert_equals(mmt_engine_size(), new_size)


def test_upload_domain():
    copy_engine('_test_base', 'default')
    init_size = mmt_engine_size()

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
    assert_equals(mmt_api_translate('Hello world!'), 'Mondo Hello!')
    mmt_api_import(os.path.join(RES, 'NewDomain.tmx'))
    assert_equals(mmt_api_translate('Hello world!'), 'Ciao mondo!')
    assert_equals(mmt_api_count_domains(), 4)

    mmt_stop()
    new_size = mmt_engine_size()
    assert_size_increased(new_size, init_size)

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(4, 0))
    sleep(5)
    mmt_stop()

    assert_equals(mmt_engine_size(), new_size)


def test_updating_from_scratch_all():
    copy_engine('_test_4C_2TM', 'default')
    copy_engine_model('context', '_test_base', 'default')
    copy_engine_model('ilm', '_test_base', 'default')
    copy_engine_model('sapt', '_test_base', 'default')

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
    sleep(5)
    assert_equals(mmt_api_count_domains(), 5)
    mmt_stop()

    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
    sleep(5)
    mmt_stop()
    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))


def test_updating_from_scratch_context():
    copy_engine('_test_4C_2TM', 'default')
    copy_engine_model('context', '_test_base', 'default')

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
    sleep(5)
    assert_equals(mmt_api_count_domains(), 5)
    mmt_stop()

    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
    sleep(5)
    mmt_stop()
    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))


def test_updating_from_scratch_ilm():
    copy_engine('_test_4C_2TM', 'default')
    copy_engine_model('ilm', '_test_base', 'default')

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
    sleep(5)
    assert_equals(mmt_api_count_domains(), 5)
    mmt_stop()

    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
    sleep(5)
    mmt_stop()
    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))


def test_updating_from_scratch_sapt():
    copy_engine('_test_4C_2TM', 'default')
    copy_engine_model('sapt', '_test_base', 'default')

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(0, 0))
    sleep(5)
    assert_equals(mmt_api_count_domains(), 5)
    mmt_stop()

    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
    sleep(5)
    mmt_stop()
    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))


def test_updating_from_partial_all():
    copy_engine('_test_4C_2TM', 'default')
    copy_engine_model('context', '_test_2C_1TM', 'default')
    copy_engine_model('ilm', '_test_2C_1TM', 'default')
    copy_engine_model('sapt', '_test_2C_1TM', 'default')

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(4, 2))
    sleep(5)
    assert_equals(mmt_api_count_domains(), 5)
    mmt_stop()

    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
    sleep(5)
    mmt_stop()
    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))


def test_updating_from_partial_context():
    copy_engine('_test_4C_2TM', 'default')
    copy_engine_model('context', '_test_2C_1TM', 'default')

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(4, 2))
    sleep(5)
    assert_equals(mmt_api_count_domains(), 5)
    mmt_stop()

    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
    sleep(5)
    mmt_stop()
    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))


def test_updating_from_partial_ilm():
    copy_engine('_test_4C_2TM', 'default')
    copy_engine_model('ilm', '_test_2C_1TM', 'default')

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(4, 2))
    sleep(5)
    assert_equals(mmt_api_count_domains(), 5)
    mmt_stop()

    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
    sleep(5)
    mmt_stop()
    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))


def test_updating_from_partial_sapt():
    copy_engine('_test_4C_2TM', 'default')
    copy_engine_model('sapt', '_test_2C_1TM', 'default')

    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(4, 2))
    sleep(5)
    assert_equals(mmt_api_count_domains(), 5)
    mmt_stop()

    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))
    mmt_start()
    assert_equals(mmt_stream_status(), StreamsStatus(8, 4))
    sleep(5)
    mmt_stop()
    assert_equals(mmt_engine_size(), mmt_engine_size('_test_4C_2TM'))


# Main =================================================================================================================

test_run = {}
test_run_count = 0
test_run_success = 0


def run(test_name):
    global test_run_count, test_run_success

    name = test_name.__name__
    test_run_count += 1

    try:
        log('> Running test "%s"' % name)
        mmt_stop()
        test_name()
        test_run_success += 1
        log('SUCCESS\n')

        test_run[name] = True
    except AssertionError as e:
        log('FAIL: ' + str(e))
        test_run[name] = False


if __name__ == '__main__':
    log('=== Creating test resources')
    mmt_create_example()
    mmt_start()
    mmt_stop()

    copy_engine('default', '_test_base')

    mmt_start()
    mmt_api_append(2, 'hello Mike! 1', 'ciao Mike! 1')
    mmt_api_append(2, 'hello Mike! 2', 'ciao Mike! 2')
    mmt_api_import(os.path.join(RES, 'NewDomain.tmx'))
    mmt_stop()

    copy_engine('default', '_test_2C_1TM')

    mmt_start()
    mmt_api_append(2, 'hello Mike! 3', 'ciao Mike! 3')
    mmt_api_append(2, 'hello Mike! 4', 'ciao Mike! 4')
    mmt_api_import(os.path.join(RES, 'NewDomain.tmx'), name='NewDomain2')
    mmt_stop()

    copy_engine('default', '_test_4C_2TM')
    log('=== Test resources created, running tests\n')

    # Running tests

    run(test_single_contribution)
    run(test_upload_domain)

    run(test_updating_from_scratch_all)
    run(test_updating_from_scratch_ilm)
    run(test_updating_from_scratch_sapt)
    run(test_updating_from_scratch_context)

    run(test_updating_from_partial_all)
    run(test_updating_from_partial_ilm)
    run(test_updating_from_partial_sapt)
    run(test_updating_from_partial_context)

    result = {
        'passed': (test_run_success == test_run_count),
        'results': {
            'test_count': test_run_count,
            'success_test_count': test_run_success,
            'tests': {k: ('SUCCESS' if v else 'FAIL') for k, v in test_run.iteritems()}
        }
    }

    print json.dumps(result)

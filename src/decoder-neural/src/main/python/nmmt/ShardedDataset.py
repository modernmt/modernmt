import logging

import mmap
import os
import random
import shutil
import struct

import math
import torch

from onmt import Dataset


class _HeapIndex:
    _ENTRY_SIZE = 14

    @staticmethod
    def _serialize(word_count, pointer, data_size):
        return struct.pack('H', word_count) + struct.pack('Q', pointer) + struct.pack('I', data_size)

    @staticmethod
    def _deserialize(raw):
        if not raw:
            return None

        word_count = struct.unpack('H', raw[0:2])[0]
        pointer = struct.unpack('Q', raw[2:10])[0]
        data_size = struct.unpack('I', raw[10:14])[0]

        return word_count, pointer, data_size

    def __init__(self, path):
        self._logger = logging.getLogger('nmmt._HeapIndex')
        self._log_level = logging.INFO

        self._path = path
        self._output_stream = None
        self._input_stream = None
        self._mmap = None

        if os.path.isfile(path):
            self._entry_count = os.path.getsize(path) / self._ENTRY_SIZE
        else:
            self._entry_count = 0

    def __len__(self):
        return self._entry_count

    def _open_for_write(self):
        if self._input_stream is not None:
            self._input_stream.close()
            self._mmap.close()

        self._input_stream = None
        self._mmap = None

        if self._output_stream is None:
            self._output_stream = open(self._path, 'wb')

    def _open_for_read(self):
        if self._output_stream is not None:
            self.flush()

        if self._input_stream is None:
            self._input_stream = open(self._path, 'r')
            self._mmap = mmap.mmap(self._input_stream.fileno(), 0, access=mmap.ACCESS_READ)

    def read_all(self):
        self._open_for_read()
        pointer = 0

        while pointer < len(self._mmap):
            yield self._deserialize(self._mmap[pointer:pointer + self._ENTRY_SIZE])
            pointer += self._ENTRY_SIZE

    def read(self, index, length):
        self._open_for_read()

        pointer = index * self._ENTRY_SIZE
        result = []
        for i in xrange(length):
            raw = self._mmap[pointer:pointer + self._ENTRY_SIZE]
            pointer += self._ENTRY_SIZE

            result.append(self._deserialize(raw))
            if pointer >= len(self._mmap):
                break

        return result

    def append(self, word_count, pointer, data_size):
        if self._output_stream is None:
            self._output_stream = open(self._path, 'ab')
        self._output_stream.write(self._serialize(word_count, pointer, data_size))
        self._entry_count += 1

    def sort(self, max_ram_in_mb):
        ram_limit = max_ram_in_mb * 1024 * 1024
        line_limit = max(int((ram_limit / self._ENTRY_SIZE) * .9), 1)

        shard_count = 0
        word_counts, pointers, data_sizes = [], [], []

        for word_count, pointer, data_size in self.read_all():
            word_counts.append(word_count)
            pointers.append(pointer)
            data_sizes.append(data_size)

            if len(word_counts) >= line_limit:
                self._store_shard(shard_count, word_counts, pointers, data_sizes)
                word_counts, pointers, data_sizes = [], [], []
                shard_count += 1

        if len(word_counts) > 0:
            self._store_shard(shard_count, word_counts, pointers, data_sizes)
            word_counts, pointers, data_sizes = [], [], []
            shard_count += 1

        self._open_for_write()

        shards = [open(self._path + ('.%u' % shard), 'r') for shard in range(shard_count)]

        with open(self._path, 'wb') as out:
            head = [self._deserialize(shard.read(self._ENTRY_SIZE)) for shard in shards]
            head = [x for x in head if x is not None]

            while len(head) > 0:
                min_size = 0
                idx_min_size = -1

                for i in range(len(head)):
                    size = head[i][0]

                    if size < min_size or idx_min_size < 0:
                        idx_min_size = i
                        min_size = size

                word_count, pointer, data_size = head[idx_min_size]
                out.write(self._serialize(word_count, pointer, data_size))

                head[idx_min_size] = self._deserialize(shards[idx_min_size].read(self._ENTRY_SIZE))
                if head[idx_min_size] is None:
                    del head[idx_min_size]
                    shards[idx_min_size].close()
                    del shards[idx_min_size]

        for shard in shards:
            shard.close()
        for shard in range(shard_count):
            os.remove(self._path + ('.%u' % shard))

    def _store_shard(self, shard, word_counts, pointers, data_sizes):
        perm = torch.randperm(len(word_counts))

        word_counts = [word_counts[idx] for idx in perm]
        pointers = [pointers[idx] for idx in perm]
        data_sizes = [data_sizes[idx] for idx in perm]

        _, perm = torch.sort(torch.Tensor(word_counts))

        with open(self._path + ('.%u' % shard), 'w') as out:
            for idx in perm:
                out.write(self._serialize(word_counts[idx], pointers[idx], data_sizes[idx]))

    def flush(self):
        if self._output_stream is not None:
            self._output_stream.flush()
            self._output_stream.close()
            self._output_stream = None


class _HeapData:
    @staticmethod
    def _deserialize(raw):
        data = struct.unpack('%uI' % (len(raw) // 4), raw)

        source_len = data[0]
        source = list(data[1:1 + source_len])
        target = list(data[2 + source_len:])

        return source, target

    def __init__(self, path):
        self._logger = logging.getLogger('nmmt._HeapData')
        self._log_level = logging.INFO

        self._path = path
        self._output_stream = None
        self._input_stream = None
        self._mmap = None

        if os.path.isfile(path):
            self._tail_pointer = os.path.getsize(path)
        else:
            self._tail_pointer = 0

    def _open_for_write(self):
        if self._input_stream is not None:
            self._input_stream.close()
            self._mmap.close()

        self._input_stream = None
        self._mmap = None

        if self._output_stream is None:
            self._output_stream = open(self._path, 'wb')

    def _open_for_read(self):
        if self._output_stream is not None:
            self._output_stream.close()
            self._output_stream = None

        if self._input_stream is None:
            self._input_stream = open(self._path, 'r')
            self._mmap = mmap.mmap(self._input_stream.fileno(), 0, access=mmap.ACCESS_READ)

    def append(self, source, target):
        self._open_for_write()

        word_count = len(source)

        entry = [word_count] + source + [len(target)] + target
        data = struct.pack('%uI' % len(entry), *entry)

        data_size = len(data)
        pointer = self._tail_pointer

        self._tail_pointer += data_size
        self._output_stream.write(data)

        return word_count, pointer, data_size

    def read(self, pointer, data_size):
        self._open_for_read()

        raw = self._mmap[pointer:(pointer + data_size)]
        return self._deserialize(raw)

    def read_batch(self, indexes):
        self._open_for_read()

        result = []
        for _, pointer, data_size in indexes:
            raw = self._mmap[pointer:(pointer + data_size)]
            result.append(self._deserialize(raw))

        return result

    def flush(self):
        if self._output_stream is not None:
            self._output_stream.flush()
            self._output_stream.close()
            self._output_stream = None


class _Heap:
    def __init__(self, path):
        self._logger = logging.getLogger('nmmt._Heap')
        self._log_level = logging.INFO

        self._idx = _HeapIndex(os.path.join(path, 'heap.idx'))
        self._data = _HeapData(os.path.join(path, 'heap.dat'))

    def __len__(self):
        return len(self._idx)

    def writer(self):
        class _Writer:
            def __init__(self, idx, data):
                self._idx = idx
                self._data = data

            def write(self, source, target):
                word_count, pointer, data_size = self._data.append(source, target)
                self._idx.append(word_count, pointer, data_size)

            def close(self):
                self._idx.flush()
                self._data.flush()

        return _Writer(self._idx, self._data)

    def sort(self, ram_limit_mb):
        self._idx.sort(ram_limit_mb)

    def read_all(self):
        for word_count, pointer, data_size in self._idx.read_all():
            yield self._data.read(pointer, data_size)

    def read(self, index, length):
        return self._data.read_batch(self._idx.read(index, length))


class ShardedDataset(object):
    class Builder(object):
        def __init__(self, path):
            # shutil.rmtree(path, ignore_errors=True)
            # os.makedirs(path)
            if not os.path.exists(path):
                os.makedirs(path)

            self._heap = _Heap(path)
            self._heap_writer = None

        def add(self, sources, targets):
            if self._heap_writer is None:
                self._heap_writer = self._heap.writer()

            for source, target in zip(sources, targets):
                self._heap_writer.write(source, target)

        def build(self, ram_limit_mb=1024):
            self._heap_writer.close()
            self._heap.sort(ram_limit_mb)

    @staticmethod
    def load(file_path, batch_size, cuda, volatile=False):
        return ShardedDataset(_Heap(file_path), batch_size, cuda, volatile)

    def __init__(self, heap, batch_size, cuda, volatile=False):
        self._logger = logging.getLogger('nmmt.ShardedDataset')
        self._log_level = logging.INFO

        self._heap = heap
        self._batch_size = batch_size
        self._batch_count = int(math.ceil(float(len(heap)) / batch_size))

        self._dataset_impl = Dataset([], [], batch_size, cuda, volatile=volatile, data_type="text")
        self._dataset_impl.numBatches = 1

    def __len__(self):
        return self._batch_count

    def __getitem__(self, index):
        if index < 0 or index >= self._batch_count:
            raise IndexError('dataset index out of bound')

        batch = self._heap.read(index * self._batch_size, self._batch_size)

        self._dataset_impl.src = [torch.LongTensor(x[0]) for x in batch]
        self._dataset_impl.tgt = [torch.LongTensor(x[1]) for x in batch]

        return self._dataset_impl.__getitem__(0)


def _test_build(path, size=1000000):
    builder = ShardedDataset.Builder(path)
    for i in xrange(size):
        base = [x + i for x in xrange(random.randint(1, 20))]

        source = [0] + base + [0]
        target = [1] + [x + 1 for x in base] + [1]

        builder.add([source], [target])

    return builder.build(5)


def _test_dump(path, do_print=False):
    dataset = ShardedDataset.load(path, 64, None)

    prev_len = 0

    for source, translation in dataset._heap.read_all():
        assert source[-1] == source[0] == 0
        assert translation[-1] == translation[0] == 1
        assert prev_len <= len(source)
        prev_len = len(source)

        source_base = [x for x in source[1:-1]]
        target_base = [x - 1 for x in translation[1:-1]]

        assert source_base == target_base

        if do_print:
            print source, translation

import math
import mmap
import os
import random
import shutil
import struct

import torch

from onmt import Dataset
from IDataset import IDataset
from torch_utils import torch_is_using_cuda


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


class MMapDataset(IDataset):
    class Builder(object):
        def __init__(self, path):
            shutil.rmtree(path, ignore_errors=True)
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

            return MMapDataset(self._heap)

    @staticmethod
    def load(file_path):
        return MMapDataset(_Heap(file_path))

    def __init__(self, heap):
        self._heap = heap

    def __len__(self):
        return len(self._heap)

    def iterator(self, batch_size, shuffle=True, volatile=False, start_position=0, loop=False, random_seed=1):
        return _Iterator(self._heap, batch_size,
                         shuffle=shuffle, volatile=volatile, start_position=start_position, loop=loop,
                         random_seed=random_seed)


class _Iterator(IDataset.Iterator):
    def __init__(self, heap, batch_size, shuffle=True, volatile=False, start_position=0, loop=False, random_seed=1):
        self._heap = heap
        self._batch_size = batch_size
        self._batch_count = int(math.ceil(float(len(heap)) / batch_size))
        self._shuffle = shuffle
        self._random_seed = random_seed
        self._loop = loop

        self._dataset = Dataset([], [], batch_size, torch_is_using_cuda(), volatile=volatile, data_type="text")
        self._dataset.numBatches = 1

        self._current_batch_order = None
        self._current_position = None
        self._reset(start_position)

    def _reset(self, position=None):
        if position is not None:
            self._current_position = position

        self._current_batch_order = range(self._batch_count)

        if self._shuffle:
            epoch = int(self._current_position / self._batch_count) + self._random_seed
            random.Random(epoch).shuffle(self._current_batch_order)

    def __len__(self):
        return self._batch_count

    def __getitem__(self, index):
        if index < 0 or index >= self._batch_count:
            raise IndexError('dataset index out of bound')

        batch = self._heap.read(index * self._batch_size, self._batch_size)

        self._dataset.src = [torch.LongTensor(x[0]) for x in batch]
        self._dataset.tgt = [torch.LongTensor(x[1]) for x in batch]

        return self._dataset.__getitem__(0)

    def __iter__(self):
        return self

    def next(self):
        if self._current_position > 0 and (self._current_position % self._batch_count) == 0:
            if self._loop:
                self._reset()
            else:
                raise StopIteration

        i = self._current_position % self._batch_count
        i = self._current_batch_order[i]

        self._current_position += 1

        return self._current_position - 1, self.__getitem__(i)

    def position(self):
        return self._current_position

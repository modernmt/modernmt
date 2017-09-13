class IDataset(object):
    def __len__(self):
        raise NotImplementedError

    def iterator(self, batch_size, shuffle=True, volatile=False, start_position=0, loop=False, random_seed=1):
        raise NotImplementedError


class DatasetWrapper(IDataset):
    def __init__(self, dataset):
        self._dataset = dataset

    def iterator(self, batch_size, shuffle=True, volatile=False, start_position=0, loop=False, random_seed=1):
        class _Iterator(object):
            def __init__(self, dataset):
                self._dataset = dataset
                self._i = 0

            def __iter__(self):
                return self

            def next(self):
                if self._i >= len(self._dataset):
                    raise StopIteration
                else:
                    self._i += 1
                    return self._i - 1, self._dataset[self._i - 1]

        return _Iterator(self._dataset)

    def __len__(self):
        return len(self._dataset)

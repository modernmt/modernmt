class IDataset(object):
    class Iterator(object):
        def __iter__(self):
            raise NotImplementedError

        def next(self):
            raise NotImplementedError

        def position(self):
            raise NotImplementedError

    def __len__(self):
        raise NotImplementedError

    def iterator(self, batch_size, shuffle=True, volatile=False, start_position=0, loop=False, random_seed=1):
        raise NotImplementedError


class DatasetWrapper(IDataset):
    def __init__(self, dataset):
        self._dataset = dataset

    def iterator(self, batch_size, shuffle=True, volatile=False, start_position=0, loop=False, random_seed=1):
        class _Iterator(IDataset.Iterator):
            def __init__(self, dataset):
                self._dataset = dataset
                self._current_position = 0
                self._shuffle = shuffle
                self._loop = loop

            def __iter__(self):
                return self

            def __len__(self):
                return self._dataset.batchSize

            def _reset(self, position=None):
                # TODO: shuffle?
                if position is not None:
                    self._current_position = position

            def next(self):
                if self._current_position > 0 and (self._current_position % len(self._dataset)) == 0:
                    if self._loop:
                        self._reset()
                    else:
                        raise StopIteration

                i = self._current_position % len(self._dataset)

                self._current_position += 1

                return self._current_position - 1, self._dataset[i]

            def position(self):
                return self._current_position

        return _Iterator(self._dataset)

    def __len__(self):
        return len(self._dataset)

import torch

_torch_gpus = None


class CudaNotAvailableError(Exception):
    def __init__(self):
        super(CudaNotAvailableError, self).__init__('CUDA not found, please install CUDA drivers and/or GPU hardware')


class InvalidGpuError(Exception):
    def __init__(self, i):
        super(InvalidGpuError, self).__init__('Invalid GPU specified: %d. Value must be between 0 and %d'
                                              % (i, torch.cuda.device_count() - 1))


class MultiGpuNotSupportedError(Exception):
    def __init__(self):
        super(MultiGpuNotSupportedError, self).__init__(
            'Invalid GPUs specified: training is currently limited to single-GPU')


def torch_setup(gpus=None, random_seed=None):
    global _torch_gpus

    if torch.cuda.is_available():
        if gpus is None:
            gpus = range(torch.cuda.device_count()) if torch.cuda.is_available() else None

            if gpus is not None and len(gpus) > 1:  # Current version only supports single GPU
                gpus = gpus[:1]
        elif len(gpus) == 0:
            gpus = None
        else:
            max_device_index = torch.cuda.device_count() - 1

            # Identify indexes of GPUs which are not valid,
            # because larger than the number of available GPU or smaller than 0
            for i in gpus:
                if i < 0 or i > max_device_index:
                    raise InvalidGpuError(i)

            if len(gpus) == 0:
                gpus = None
    else:
        if gpus is not None and len(gpus) > 0:
            raise CudaNotAvailableError()

        gpus = None

    if gpus is not None and len(gpus) > 1:
        raise MultiGpuNotSupportedError()

    if random_seed is not None:
        torch.manual_seed(random_seed)
        if torch.cuda.is_available():
            torch.cuda.random.manual_seed_all(random_seed)

    if gpus is not None and len(gpus) > 0:
        torch.cuda.set_device(gpus[0])

    _torch_gpus = gpus


def torch_get_gpus():
    global _torch_gpus
    return _torch_gpus


def torch_is_using_cuda():
    global _torch_gpus
    return _torch_gpus is not None and len(_torch_gpus) > 0


def torch_is_multi_gpu():
    global _torch_gpus
    return _torch_gpus is not None and len(_torch_gpus) > 1

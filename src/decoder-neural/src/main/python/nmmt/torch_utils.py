import torch

_torch_gpus = None


def torch_setup(gpus=None, random_seed=None):
    global _torch_gpus

    if torch.cuda.is_available():
        if gpus is None:
            gpus = range(torch.cuda.device_count()) if torch.cuda.is_available() else None
        else:
            # remove indexes of GPUs which are not valid,
            # because larger than the number of available GPU or smaller than 0
            gpus = [x for x in gpus if x < torch.cuda.device_count() or x < 0]
            if len(gpus) == 0:
                gpus = None
    else:
        gpus = None

    if random_seed is not None:
        torch.manual_seed(random_seed)
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

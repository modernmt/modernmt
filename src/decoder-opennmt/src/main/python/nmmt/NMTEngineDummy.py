class NMTEngineDummy:

    @staticmethod
    def load_from_checkpoint(checkpoint_path, using_cuda):
        return NMTEngineDummy(None, None, None, None, None, checkpoint_path, using_cuda)

    def __init__(self, params, src_dict, trg_dict, model, optim, checkpoint, using_cuda):
        return

    def _ensure_model_loaded(self):
        return

    def reset_model(self):
        return

    def tune(self, src_batch, trg_batch, epochs):
        return

    def translate(self, text, beam_size=5, max_sent_length=160, replace_unk=False, n_best=1):
        return text


def load_from_checkpoint(engine_model_file, using_cuda):
    return None
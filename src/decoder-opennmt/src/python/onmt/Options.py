import torch

class Options(object):
    def __init__(self):
        self.save_model = None  # Set by train

        self.seed = 3435
        self.gpus = range(torch.cuda.device_count()) if torch.cuda.is_available() else 0
        self.log_interval = 50

        # Model options --------------------------------------------------------------------------------------------
        self.encoder_type = "text" # type fo encoder (either "text" or "img"
        self.layers = 2  # Number of layers in the LSTM encoder/decoder
        self.rnn_size = 500  # Size of LSTM hidden states
        self.word_vec_size = 500  # Word embedding sizes
        self.input_feed = 1  # Feed the context vector at each time step as additional input to the decoder
        self.brnn = True  # Use a bidirectional encoder
        self.brnn_merge = 'sum'  # Merge action for the bidirectional hidden states: [concat|sum]

        # Optimization options -------------------------------------------------------------------------------------
        self.batch_size = 64  # Maximum batch size
        self.max_generator_batches = 32  # Maximum batches of words in a seq to run the generator on in parallel.
        self.epochs = 30  # Number of training epochs
        self.start_epoch = 1  # The epoch from which to start
        self.param_init = 0.1  # Parameters are initialized over uniform distribution with support
        self.optim = 'sgd'  # Optimization method. [sgd|adagrad|adadelta|adam]
        self.max_grad_norm = 5  # If norm(gradient vector) > max_grad_norm, re-normalize
        self.dropout = 0.3  # Dropout probability; applied between LSTM stacks.
        self.curriculum = False
        self.extra_shuffle = False  # Shuffle and re-assign mini-batches

        # Learning rate --------------------------------------------------------------------------------------------
        self.learning_rate = 1.0
        self.learning_rate_decay = 0.9
        self.start_decay_at = 10

        # Pre-trained word vectors ---------------------------------------------------------------------------------
        self.pre_word_vecs_enc = None
        self.pre_word_vecs_dec = None

    def state_dict(self):
        return self.__dict__

    def load_state_dict(self, d):
       self.__dict__ = d
       # we force the encoder type to "text";
       # this trick makes the models build with an old version of the software compatible with the new version
       self.encoder_type = "text" # type fo encoder (either "text" or "img"

    def __repr__(self):
        return repr(self.__dict__)

from fairseq.data import Dictionary

PAD = "<PAD>_"
EOS = "<EOS>_"
UNK = "<UNK>_"

RESERVED_TOKENS = ["<Lua_Heritage>", PAD, EOS, UNK]
PAD_ID = RESERVED_TOKENS.index(PAD)  # Normally 1
EOS_ID = RESERVED_TOKENS.index(EOS)  # Normally 2
UNK_ID = RESERVED_TOKENS.index(UNK)  # Normally 3


class FactorDictionary(Dictionary):

    def __init__(self):
        # super().__init__()  - DO NOT CALL
        self.pad_word, self.eos_word, self.unk_word = PAD, EOS, UNK
        self.pad_index, self.eos_index, self.unk_index = PAD_ID, EOS_ID, UNK_ID

        self.symbols = []
        self.indices = {}
        self.count = None

        self.pad_index = RESERVED_TOKENS.index(PAD)
        self.eos_index = RESERVED_TOKENS.index(EOS)
        self.unk_index = RESERVED_TOKENS.index(UNK)
        self.nspecial = len(RESERVED_TOKENS)


        self.default_factor = '0'
        self.add_symbol(self.default_factor)

    # This function returns True if the factor should be skipped for the sake of recovering original tokens
    def skip(self, factor):
        return False

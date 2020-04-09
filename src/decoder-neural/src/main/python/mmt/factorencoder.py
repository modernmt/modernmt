from fairseq.data import Dictionary

PAD = "<PAD>_"
EOS = "<EOS>_"
UNK = "<UNK>_"

RESERVED_TOKENS = ["<Lua_Heritage>", PAD, EOS, UNK]

class FactorDictionary(Dictionary):

    def __init__(self):
        # super().__init__()  # DO NOT CALL
        self.pad_word, self.eos_word, self.unk_word = PAD, EOS, UNK

        self.symbols = RESERVED_TOKENS
        self.nspecial = len(RESERVED_TOKENS)
        self.count = []

        self.default_factor = '0'
        if self.default_factor not in self.symbols:
            self.symbols.append(self.default_factor)

        self.indices = {s: i for i, s in enumerate(self.symbols) if s}

        self.pad_index = self.indices[PAD]
        self.eos_index = self.indices[EOS]
        self.unk_index = self.indices[UNK]

    def save(self, f):
        if isinstance(f, str):
            with open(f, 'w', encoding='utf-8') as fd:
                return self.save(fd)

        for symbol in self.symbols:
            print("'{}'".format(symbol), file=f)

    # This function returns True if the factor should be skipped for the sake of recovering original tokens
    def skip(self, factor):
        return False

    # The generation of the factors for the source side can depend from the target side as well
    def generate_factors(self, src_line, tgt_line=None):
        src_line = src_line.strip()

        if len(src_line) == 0:
            return None, None

        tokens = src_line.split()
        factors = [ self.default_factor for i in range(len(tokens)) ]

        return ' '.join(tokens) + '\n', ' '.join(factors) + '\n'
import locale
import os

from fairseq import options
from fairseq.data import data_utils
from fairseq.models import register_model_architecture
from fairseq.models.transformer import transformer_vaswani_wmt_en_de_big, transformer_iwslt_de_en
from fairseq.tasks import register_task
from fairseq.tasks.translation import TranslationTask

from mmt.textencoder import SubwordDictionary

if locale.getpreferredencoding().lower() != 'utf-8':
    raise UnicodeError('python locale preferred encoding is "%s", UTF-8 expected' % locale.getpreferredencoding())


@register_task('mmt_translation')
class MMTTranslationTask(TranslationTask):
    def __init__(self, args, subword_dict):
        super().__init__(args, subword_dict, subword_dict)
        self._subword_dict = subword_dict

    @classmethod
    def load_dictionary(cls, filename):
        return SubwordDictionary.load(filename)

    @classmethod
    def build_dictionary(cls, filenames, workers=1, threshold=-1, nwords=-1, padding_factor=8):
        raise NotImplementedError

    @classmethod
    def setup_task(cls, args, **kwargs):
        args.left_pad_source = options.eval_bool(args.left_pad_source)
        args.left_pad_target = options.eval_bool(args.left_pad_target)

        # find language pair automatically
        if args.source_lang is None or args.target_lang is None:
            args.source_lang, args.target_lang = data_utils.infer_language_pair(args.data[0])

        # load dictionary
        subword_dict = SubwordDictionary.load(os.path.join(args.data[0], 'model.vcb'))

        return cls(args, subword_dict)


@register_model_architecture('transformer', 'transformer_mmt_big')
def transformer_mmt_big(args):
    transformer_vaswani_wmt_en_de_big(args)


@register_model_architecture('transformer', 'transformer_mmt_base')
def transformer_mmt_base(args):
    transformer_iwslt_de_en(args)


@register_model_architecture('transformer', 'transformer_mmt_tiny')
def transformer_mmt_tiny(args):
    args.encoder_embed_dim = getattr(args, 'encoder_embed_dim', 512)
    args.encoder_ffn_embed_dim = getattr(args, 'encoder_ffn_embed_dim', 1024)
    args.encoder_attention_heads = getattr(args, 'encoder_attention_heads', 2)
    args.encoder_layers = getattr(args, 'encoder_layers', 4)
    args.decoder_embed_dim = getattr(args, 'decoder_embed_dim', 512)
    args.decoder_ffn_embed_dim = getattr(args, 'decoder_ffn_embed_dim', 1024)
    args.decoder_attention_heads = getattr(args, 'decoder_attention_heads', 2)
    args.decoder_layers = getattr(args, 'decoder_layers', 4)
    args.dropout = getattr(args, 'dropout', 0.3)
    transformer_mmt_base(args)


@register_model_architecture('transformer', 'transformer_mmt_unit_testing')
def transformer_mmt_unit_testing(args):
    args.encoder_embed_dim = getattr(args, 'encoder_embed_dim', 8)
    args.encoder_ffn_embed_dim = getattr(args, 'encoder_ffn_embed_dim', 8)
    args.encoder_attention_heads = getattr(args, 'encoder_attention_heads', 1)
    args.encoder_layers = getattr(args, 'encoder_layers', 1)
    args.decoder_embed_dim = getattr(args, 'decoder_embed_dim', 8)
    args.decoder_ffn_embed_dim = getattr(args, 'decoder_ffn_embed_dim', 8)
    args.decoder_attention_heads = getattr(args, 'decoder_attention_heads', 1)
    args.decoder_layers = getattr(args, 'decoder_layers', 1)
    args.dropout = getattr(args, 'dropout', 0.3)
    transformer_mmt_base(args)

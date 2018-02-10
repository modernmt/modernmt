/**
 *  Source: https://unicode-table.com/
 *
 *  1100-11FF: Hangul Jamo (korean)
 *
 *  2E80-2EFF: CJK Radicals Supplement (chinese, japanese, vietnamese, korean)
 *  2F00-2FDF: Kangxi Radicals (chinese)
 *  2FE0-2FEF: ---
 *  2FF0-2FFF: Ideographic Description Characters
 *  3000-303F: CJK Symbols and Punctuation
 *  3040-309F: Hiragana (japanese)
 *  30A0-30FF: Katakana (japanese)
 *  3100-312F: Bopomofo (chinese)
 *  3130-318F: Hangul Compatibility Jamo (chinese)
 *  3190-319F: Kanbun (chinese)
 *  31A0-31BF: Bopomofo Extended
 *  31C0-31EF: CJK Strokes (chinese)
 *  31F0-31FF: Katakana Phonetic Extensions (japanese)
 *  3200-32FF: Enclosed CJK Letters and Months (chinese)
 *  3300-33FF: CJK Compatibility (chinese, japanese, korean, vietnamese)
 *  3400-4DBF: CJK Unified Ideographs Extension A (chinese, japanese, korean, vietnamese)
 *  4DC0-4DFF: Yijing Hexagram Symbols
 *  4E00-9FFF: CJK Unified Ideographs (chinese, japanese, korean, vietnamese)
 *  A000-A48F: Yi Syllables (yi)
 *  A490-A4CF: Yi Radicals (yi)
 *
 *  A840-A87F: Phags-pa (mongolian, sanskrit, tibetan, chinese, uyghur)
 *
 *  AC00-D7AF: Hangul Syllables (korean)
 *  D7B0-D7FF: Hangul Jamo Extended-B
 *  D800-DB7F: High Surrogates (approximation: surrogates are considered CJKV)
 *  DB80-DBFF: High Private Use Surrogates (approximation: surrogates are considered CJKV)
 *  DC00-DFFF: Low Surrogates (approximation: surrogates are considered CJKV)
 *
 *  F900-FAFF: CJK Compatibility Ideographs
 *
 *  FE30-FE4F: CJK Compatibility Forms
 *
 *  FF65-FFDC: Halfwidth and Fullwidth Forms
 */
CJKV       = [\u1100-\u11FF\u2E80-\uA4CF\uA840-\uA87F\uAC00-\uDFFF\uF900-\uFAFF\uFE30-\uFE4F\uFF65-\uFFDC]
_          = " "
Letter     = !(![:letter:]|{CJKV})

UrlPart    = ({Letter}|[[:digit:]\-\_])+
Number     = ((\-|\+)?[0-9]+)?([\.,][0-9]+)*

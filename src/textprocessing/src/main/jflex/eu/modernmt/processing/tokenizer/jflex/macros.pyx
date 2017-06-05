CJK        = [\u4E00-\u62FF\u6300-\u77FF\u7800-\u8CFF\u8D00-\u9FFF]
_          = " "
Letter     = !(![:letter:]|{CJK})

UrlPart    = [[:letter:][:digit:]\-\_]+
Number     = ((\-|\+)?[0-9]+)?([\.,][0-9]+)*
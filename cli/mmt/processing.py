import html
import re


class XMLEncoder:
    __TAG_NAME = '([a-zA-Z]|_|:)([a-zA-Z]|[0-9]|\\.|-|_|:|)*'
    __TAG_REGEX = re.compile('(<(' + __TAG_NAME + ')[^>]*/?>)|(<!(' + __TAG_NAME + ')[^>]*[^/]>)|(</(' +
                             __TAG_NAME + ')[^>]*>)|(<!--)|(-->)')

    @staticmethod
    def is_xml_tag(string):
        return XMLEncoder.__TAG_REGEX.match(string) is not None

    @staticmethod
    def has_xml_tag(string):
        for _ in XMLEncoder.__TAG_REGEX.finditer(string):
            return True
        return False

    @staticmethod
    def escape(string):
        escaped = html.unescape(string)
        return escaped \
            .replace('&', '&amp;') \
            .replace('<', '&lt;') \
            .replace('>', '&gt;')

    @staticmethod
    def unescape(string):
        return html.unescape(string)

    @staticmethod
    def encode(string):
        result = []
        index = 0

        for match in XMLEncoder.__TAG_REGEX.finditer(string):
            start = match.start()
            end = match.end()

            result.append(XMLEncoder.escape(string[index:start]))
            result.append(match.group())

            index = end

        if index < len(string):
            result.append(XMLEncoder.escape(string[index:]))

        return ''.join(result)

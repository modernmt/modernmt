class Translation(object):
    def __init__(self, text, alignment=None):
        self.text = text
        self.alignment = alignment


class Suggestion(object):
    def __init__(self, source, target, score):
        self.source = source
        self.target = target
        self.score = score

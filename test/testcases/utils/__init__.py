class Namespace(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def __getattr__(self, key):
        return self.__dict__[key] if key in self.__dict__ else None

    def __setattr__(self, key, value):
        self.__dict__[key] = value

    def __repr__(self):
        return 'Namespace' + str(self.__dict__)

    def __str__(self):
        return repr(self)

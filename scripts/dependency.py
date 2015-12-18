from ConfigParser import ConfigParser
import re

__author__ = 'Davide Caroselli'

_global_section = 'global'


def argparse_group(parser, clazz, name=None):
    if not hasattr(clazz, 'injectable_fields') or not hasattr(clazz, 'injector_section'):
        return None

    if name is None:
        name = re.sub(r'([a-z](?=[A-Z])|[A-Z](?=[A-Z][a-z]))', r'\1 ', clazz.__name__) + ' arguments'

    section = clazz.injector_section
    group = parser.add_argument_group(name)

    for field, (desc, t, default) in clazz.injectable_fields.iteritems():
        choices = None
        if isinstance(t, (list, tuple)):
            choices = t[1]
            t = t[0]
            desc += ' {%(choices)s}'

        if t == basestring:
            t = None

        group.add_argument('--' + section + '.' + field, dest=section + '___' + field, help=desc, default=default,
                           type=t, choices=choices, metavar=field.upper())


class Injector:
    @staticmethod
    def _get_definitions(obj):
        if not hasattr(obj, 'injectable_fields'):
            return None, None

        fields = obj.injectable_fields
        section = _global_section if not hasattr(obj, 'injector_section') else obj.injector_section

        return section, fields

    def __init__(self, *classes):
        self._definitions = {}
        self._params = {}

        for clazz in classes:
            section, fields = self._get_definitions(clazz)
            if section is not None:
                self._definitions[section] = fields

    def read_args(self, args):
        self._params = {}

        for section, fields in self._definitions.iteritems():
            self._params[section] = {}

            for field in fields:
                var = section + '___' + field
                self._params[section][field] = getattr(args, var, None)

    def read_config(self, config):
        self._params = {}

        for section, fields in self._definitions.iteritems():
            self._params[section] = {}

            for field, (desc, ftype, defval) in fields.iteritems():
                if isinstance(ftype, tuple):
                    ftype = ftype[0]

                value = config.get(section, field)

                if ftype is bool:
                    value = (value == 'True')
                elif ftype is not basestring:
                    value = ftype(value)

                self._params[section][field] = value

    def _get_actual_param(self, param_name, section=None):
        if section is None:
            section = _global_section

        if section in self._params and param_name in self._params[section]:
            return self._params[section][param_name]
        elif section in self._definitions and param_name in self._definitions[section]:
            definition = self._definitions[section][param_name]
            return definition[2] if len(definition) > 2 else None
        else:
            raise Exception('Unknown param "' + param_name + '" of section "' + section + '"')

    def inject(self, instance):
        section, fields = self._get_definitions(instance)

        if section is not None and fields is not None:
            for field in fields:
                setattr(instance, '_' + field, self._get_actual_param(field, section))

        _on_fields_injected = getattr(instance, '_on_fields_injected', None)
        if callable(_on_fields_injected):
            _on_fields_injected(self)

        return instance

    def to_config(self):
        config = ConfigParser()

        for section, fields in self._definitions.iteritems():
            config.add_section(section)

            for field in fields:
                config.set(section, field, self._get_actual_param(field, section))

        return config

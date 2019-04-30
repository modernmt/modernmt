def file_readlines(file_path):
    with open(file_path, 'r', encoding='utf-8') as stream:
        return [line.rstrip('\n') for line in stream.readlines()]

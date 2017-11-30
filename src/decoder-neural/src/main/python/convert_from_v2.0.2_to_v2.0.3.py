import sys
import os
import shutil
import torch

def main(argv):
    if len(argv) < 2:
        raise Exception("Missing parameter")

    path_in=argv[1]
    path_out=argv[2]

    sys.stderr.write("path_in:%s path_out:%s\n" % (path_in, path_out))

    engines = {}
    settings = []
    with open(os.path.join(path_in,"model.conf"),"r") as cfg_in_stream:
        for line in cfg_in_stream:
            if line.startswith('model.'):
                key, model_name = map(str.strip, line.split("="))
                engines[key[6:]] = model_name
            else:
                settings.append(line).strip()

    with open(os.path.join(path_out,"model.conf"),"r") as cfg_out_stream:
        cfg_out_stream.write('[models]\n')
        for key in engines:
            cfg_out_stream.write('%s = %s\n' % (key, engines[key]))
        cfg_out_stream.write('[settings]\n')
        for s in settings:
            cfg_out_stream.write('%s\n' % (s))


    for key in engines:
        convert_model(path_in, path_out, engines[key])

def convert_model(path_in, path_out, filename):
    model_in = os.path.join(path_in,filename)
    model_out = os.path.join(path_out,filename)

    shutil.copy(model_in + '.meta', model_out + '.meta')
    shutil.copy(model_in + '.bpe', model_out + '.bpe')

    chkpt_in = torch.load(model_in + '.dat', map_location=lambda storage, loc: storage)

    chkpt_out = {
                'model': chkpt_in['model'],
                'generator': chkpt_in['generator'],
            }
    torch.save(chkpt_out, model_out + '.dat')

    dict_out = {
                'src': chkpt_in['dicts']['src'],
                'tgt': chkpt_in['dicts']['tgt'],
            }
    torch.save(dict_out, model_out + '.vcb')

if __name__ == '__main__':
    main(sys.argv)

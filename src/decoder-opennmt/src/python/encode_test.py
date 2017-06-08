import json
import sys

for line in sys.stdin:
    print json.dumps({'source': line.strip()})
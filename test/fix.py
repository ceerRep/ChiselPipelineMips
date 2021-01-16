#! /usr/bin/env python3

import sys

with open(sys.argv[1]) as fin:
    lines = fin.read().split('\n')

lines = filter(lambda x:x.strip(), lines)
lines = list(lines)
lines = lines + ['0'] * (2048 - len(lines))

with open(sys.argv[1], 'w') as fout:
    fout.write('\n'.join(lines))


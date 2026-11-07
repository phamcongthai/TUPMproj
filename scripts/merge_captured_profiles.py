"""Merge captured startup rules into Baseline without inventing methods or hot flags."""
import argparse
import hashlib
import json
from pathlib import Path
import re

parser = argparse.ArgumentParser()
parser.add_argument('baseline', type=Path)
parser.add_argument('startup', type=Path)
parser.add_argument('output', type=Path)
args = parser.parse_args()
sources = [args.baseline, args.startup]
rules = {}
metadata = []
for source in sources:
    raw = source.read_bytes()
    lines = raw.decode('utf-8-sig').splitlines()
    metadata.append({'path': str(source), 'sha256': hashlib.sha256(raw).hexdigest(), 'rules': len(lines)})
    for line in lines:
        if not line.strip() or line.startswith('#'):
            continue
        match = re.fullmatch(r'([HSP]*)(L.+)', line)
        if not match:
            raise ValueError(f'Unsupported rule: {line}')
        flags, descriptor = match.groups()
        rules.setdefault(descriptor, set()).update(flags)
merged = [''.join(flag for flag in 'HSP' if flag in flags) + descriptor
          for descriptor, flags in sorted(rules.items())]
args.output.write_text('\n'.join(merged) + '\n', encoding='utf-8')
print(json.dumps({'sources': metadata, 'mergedRules': len(merged),
                  'sha256': hashlib.sha256(args.output.read_bytes()).hexdigest()}, indent=2))

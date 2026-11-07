"""Experimental Startup Profile: keep observed classes and methods with ART's S flag.

Baseline input stays unchanged. This is a hypothesis to benchmark, not a guaranteed gain.
Reference: https://developer.android.com/topic/performance/baselineprofiles/manually-create-measure#rule-syntax
"""
import argparse
import hashlib
import json
from pathlib import Path
import re

parser = argparse.ArgumentParser()
parser.add_argument('input', type=Path)
parser.add_argument('output', type=Path)
parser.add_argument('--report', type=Path, required=True)
args = parser.parse_args()
raw = args.input.read_bytes()
rules = {}
excluded = []
for line in raw.decode('utf-8-sig').splitlines():
    if not line.strip() or line.startswith('#'):
        continue
    match = re.fullmatch(r'([HSP]*)(L.+)', line)
    if not match:
        raise ValueError(line)
    flags, descriptor = match.groups()
    rules.setdefault(descriptor, set()).update(flags)
kept = []
for descriptor, flags in sorted(rules.items()):
    rule = ''.join(f for f in 'HSP' if f in flags) + descriptor
    if '->' not in descriptor or 'S' in flags:
        kept.append(rule)
    else:
        excluded.append(rule)
assert kept and excluded, 'Expected a nontrivial subset of the observed profile.'
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text('\n'.join(kept) + '\n', encoding='utf-8')
report = {'hypothesis': 'Exclude methods observed only after startup from DEX layout input; retain observed startup classes.',
          'inputSha256': hashlib.sha256(raw).hexdigest(),
          'outputSha256': hashlib.sha256(args.output.read_bytes()).hexdigest(),
          'inputUniqueRules': len(rules), 'keptRules': len(kept), 'excludedRules': len(excluded),
          'excluded': excluded}
args.report.parent.mkdir(parents=True, exist_ok=True)
args.report.write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps({k: v for k, v in report.items() if k != 'excluded'}, indent=2))

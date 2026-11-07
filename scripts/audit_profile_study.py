"""Independently validate saved Macrobenchmark reports against raw rows and ART state."""
import argparse
import csv
import hashlib
import json
import math
from pathlib import Path
import re
import statistics

parser = argparse.ArgumentParser()
parser.add_argument('directory', type=Path)
args = parser.parse_args()
root = args.directory

def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

method = read_json(root / 'methodology.json')
raw = list(csv.DictReader((root / 'raw-runs.csv').open(encoding='utf-8-sig', newline='')))
observations, blocks = [], []
for block in method['order']:
    folder = root / block
    log = (folder / 'instrumentation.log').read_text(encoding='utf-8-sig')
    assert 'OK (1 test)' in log and 'FAILURES!!!' not in log, block
    report = read_json(folder / 'benchmarkData.json')
    assert report['context']['build']['fingerprint'] == method['fingerprint'], block
    assert len(report['benchmarks']) == 1, block
    benchmark = report['benchmarks'][0]
    expected_test = 'coldStartupNoCompilation' if block[0] in 'AD' else 'coldStartupWithBaselineProfile'
    assert benchmark['name'] == expected_test, block
    if method.get('externalCompilation'):
        prep = (folder / 'external-compilation.log').read_text(encoding='utf-8-sig')
        state = (folder / 'dexopt-before.txt').read_text(encoding='utf-8-sig')
        assert re.search(r'result=10\b', prep), block
        expected_state = 'verify' if block[0] in 'AD' else 'speed-profile'
        assert f'[status={expected_state}]' in state, block
        if block[0] in 'EF':
            assert re.search(r'result=1\b', prep), block
    for metric in ('timeToInitialDisplayMs', 'timeToFullDisplayMs'):
        runs = benchmark['metrics'][metric]['runs']
        assert len(runs) == method['iterationsPerBlock'], block
        assert all(math.isfinite(value) and value > 0 for value in runs), block
        for i, value in enumerate(runs, 1):
            matching = [row for row in raw if row['block'] == block and row['metric'] == metric and int(row['iteration']) == i]
            assert len(matching) == 1 and float(matching[0]['milliseconds']) == value, (block, metric, i)
            observations.append((block[0], metric, value))
    traces = benchmark['profilerOutputs']
    assert len(traces) == method['iterationsPerBlock'], block
    for trace in traces:
        path = folder / trace['filename']
        assert path.is_file() and path.stat().st_size > 0, path
    blocks.append({'block': block, 'starts': method['iterationsPerBlock'], 'test': expected_test,
                   'reportSha256': hashlib.sha256((folder / 'benchmarkData.json').read_bytes()).hexdigest()})
assert len(raw) == len(observations), 'Extra or missing raw observations'
summary = []
for config in sorted({row[0] for row in observations}):
    for metric in ('timeToInitialDisplayMs', 'timeToFullDisplayMs'):
        values = [value for c, m, value in observations if c == config and m == metric]
        entry = {'configuration': config, 'metric': metric, 'n': len(values),
                 'medianMs': statistics.median(values), 'minimumMs': min(values), 'maximumMs': max(values)}
        saved = [s for s in read_json(root / 'statistics.json') if s['configuration'] == config and s['metric'] == metric]
        assert len(saved) == 1 and abs(saved[0]['medianMs'] - entry['medianMs']) < 1e-8, entry
        summary.append(entry)
for apk in read_json(root / 'apk-info.json'):
    path = root / 'apks' / (apk['configuration'] + '.apk')
    assert hashlib.sha256(path.read_bytes()).hexdigest().lower() == apk['sha256'].lower(), path
result = {'result': 'PASS', 'starts': len(observations) // 2, 'blocks': blocks, 'statistics': summary,
          'scope': 'Checks data integrity and preparation evidence; does not establish statistical significance.'}
(root / 'audit.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
print(json.dumps(result, indent=2))

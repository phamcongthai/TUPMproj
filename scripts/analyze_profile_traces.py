"""Extract audited main-thread phases from every saved trace, without rerunning timings."""
import argparse
import csv
import hashlib
import io
import json
import os
from pathlib import Path
import statistics
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('input', type=Path)
parser.add_argument('output', type=Path)
parser.add_argument('--serial', help='Device hosting the trace processor, not necessarily the device that recorded traces.')
parser.add_argument('--sql', type=Path, default=Path(__file__).with_name('profile-trace-phases.sql'))
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=False)
adb = str(Path(os.environ['LOCALAPPDATA']) / 'Android/Sdk/platform-tools/adb.exe')

def run(*command):
    device = ['-s', args.serial] if args.serial else []
    return subprocess.run([adb, *device, *command], check=True, capture_output=True, text=True, encoding='utf-8')

sql = args.sql
run('push', str(sql), '/data/local/tmp/profile-phases.sql')
rows, provenance = [], []
for trace in sorted(args.input.glob('*/*.perfetto-trace')):
    block = trace.parent.name
    run('push', str(trace), '/data/local/tmp/profile-phases.trace')
    result = run('shell', '/data/local/tmp/food-trace-processor', '-q',
                 '/data/local/tmp/profile-phases.sql', '/data/local/tmp/profile-phases.trace')
    prefix = args.output / (block + '-' + trace.stem)
    prefix.with_suffix('.csv').write_text(result.stdout, encoding='utf-8')
    prefix.with_suffix('.log').write_text(result.stderr, encoding='utf-8')
    provenance.append({'trace': str(trace), 'sha256': hashlib.sha256(trace.read_bytes()).hexdigest()})
    for row in csv.DictReader(io.StringIO(result.stdout)):
        row.update(block=block, configuration=block[0], trace=trace.name)
        rows.append(row)
    print(block, trace.name, flush=True)
if not rows:
    raise RuntimeError('No phase records extracted')
with (args.output / 'phases.csv').open('w', encoding='utf-8', newline='') as stream:
    writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)
# One observation per trace per phase: sum of occurrences, not sum across different phases.
summary = []
for config in sorted({r['configuration'] for r in rows}):
    for phase in sorted({r['phase'] for r in rows}):
        selected = [r for r in rows if r['configuration'] == config and r['phase'] == phase]
        traces = sorted({r['trace'] for r in selected})
        if not traces:
            continue
        entry = {'configuration': config, 'phase': phase, 'traces': len(traces)}
        for metric in ('wall_ms', 'scheduled_cpu_ms'):
            values = [sum(float(r[metric]) for r in selected if r['trace'] == t) for t in traces]
            entry[metric + '_median'] = statistics.median(values)
        summary.append(entry)
(args.output / 'summary.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
(args.output / 'provenance.json').write_text(json.dumps(provenance, indent=2), encoding='utf-8')
print(json.dumps(summary, indent=2))

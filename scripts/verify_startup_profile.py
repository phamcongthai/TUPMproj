"""Check R8's actual output against the DEX bytes in an APK.

Usage: python scripts/verify_startup_profile.py --apk ... --metadata ... --output ...
Based on Android Developers' Confirm Startup Profiles optimization procedure.
"""
import argparse
import hashlib
import json
import struct
from pathlib import Path
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--apk', type=Path, required=True)
parser.add_argument('--metadata', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()

def dex_class_descriptors(data):
    """Read DEX class definitions via their type and string IDs (not string search)."""
    strings_offset = struct.unpack_from('<I', data, 60)[0]
    types_offset = struct.unpack_from('<I', data, 68)[0]
    class_count, classes_offset = struct.unpack_from('<II', data, 96)
    descriptors = []
    for index in range(class_count):
        type_id = struct.unpack_from('<I', data, classes_offset + index * 32)[0]
        string_id = struct.unpack_from('<I', data, types_offset + type_id * 4)[0]
        offset = struct.unpack_from('<I', data, strings_offset + string_id * 4)[0]
        while data[offset] & 0x80:
            offset += 1
        offset += 1  # Skip the ULEB128 UTF-16 length.
        end = data.index(0, offset)
        descriptors.append(data[offset:end].decode('utf-8', errors='replace'))
    return descriptors

metadata = json.loads(args.metadata.read_text(encoding='utf-8-sig'))
optimization = metadata.get('startupOptimization', {})
assert optimization.get('isDexLayoutOptimizationEnabled') is True, 'DEX layout optimization is not enabled'
assert optimization.get('isProfileGuidedOptimizationEnabled') is True, 'Profile-guided optimization is not enabled'
with zipfile.ZipFile(args.apk) as apk:
    dex = []
    for name in apk.namelist():
        if name.endswith('.dex') and '/' not in name:
            data = apk.read(name)
            checksum = hashlib.sha256(data).hexdigest()
            matches = [entry for entry in metadata['dexFiles'] if entry['checksum'] == checksum]
            assert len(matches) == 1, f'R8 metadata does not match APK: {name}'
            descriptors = dex_class_descriptors(data)
            dex.append({'name': name, 'bytes': len(data), 'sha256': checksum, 'startup': matches[0]['startup'],
                        'methodIds': struct.unpack_from('<I', data, 88)[0],
                        'classDefs': len(descriptors),
                        'containsMainActivity': 'Lcom/example/jetpackcompose/MainActivity;' in descriptors})
    assert dex and any(entry['startup'] for entry in dex), 'No startup DEX found'
    assert len(dex) == len(metadata['dexFiles']), 'Unexpected DEX count'
    assert 'assets/dexopt/baseline.prof' in apk.namelist(), 'Binary Baseline Profile missing'
    baseline_bytes = apk.getinfo('assets/dexopt/baseline.prof').file_size
    assert baseline_bytes < 1_500_000, 'Binary Baseline Profile exceeds documented size limit'
    assert next(entry for entry in dex if entry['name'] == 'classes.dex')['startup'], 'Primary DEX is not startup DEX'
    assert next(entry for entry in dex if entry['name'] == 'classes.dex')['containsMainActivity'], 'App entry Activity is not in primary DEX'
report = {
    'apkSha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
    'r8Version': metadata['version'], 'startupOptimization': optimization,
    'dexFiles': dex, 'baselineProfilePackaged': True, 'baselineProfileBytes': baseline_bytes,
    'result': 'PASS: startup DEX flags and SHA-256 match actual APK contents',
    'reference': 'https://developer.android.com/topic/performance/baselineprofiles/confirm-startup-profiles',
}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(report, indent=2))

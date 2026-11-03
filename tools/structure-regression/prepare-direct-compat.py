"""Prepare compile-only API jars without modifying installed mods.
Pass --search DIR for each directory containing the pinned jars. MaidSpell's
all.jar is supported: its nested mod is extracted, then its hash is checked.
"""
import argparse, hashlib, json, zipfile
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--search', type=Path, action='append', required=True)
parser.add_argument('--output', type=Path, default=Path('build/direct-compat-api'))
args = parser.parse_args()
lock = json.loads(Path(__file__).with_name('direct-compat-api.json').read_text(encoding='utf-8'))
wanted = {v['sha256']: k for k, v in lock.items()}
found = {}
for directory in args.search:
    for path in directory.glob('*.jar'):
        data = path.read_bytes()
        digest = hashlib.sha256(data).hexdigest()
        if digest in wanted: found[wanted[digest]] = data
        if 'touhou_little_maid_spell' in path.name:
            with zipfile.ZipFile(path) as jar:
                nested = 'META-INF/maidspell/maidspell-mod.jar'
                if nested in jar.namelist():
                    data = jar.read(nested)
                    digest = hashlib.sha256(data).hexdigest()
                    if digest in wanted: found[wanted[digest]] = data
missing = sorted(set(lock) - set(found))
if missing: raise SystemExit('Missing pinned API jars: ' + ', '.join(missing))
args.output.mkdir(parents=True, exist_ok=True)
for name, data in found.items():
    dest = args.output / (name + '-local.jar')
    if not dest.exists() or dest.read_bytes() != data: dest.write_bytes(data)
print('Prepared', len(found), 'pinned API jars in', args.output)

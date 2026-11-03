"""Run packaged client fixtures sequentially in an existing, stopped full-pack copy."""
import argparse
import hashlib
import json
import pathlib
import re
import shutil
import subprocess
import uuid
import zipfile

p = argparse.ArgumentParser()
p.add_argument('--instance', type=pathlib.Path, required=True)
p.add_argument('--runtime', type=pathlib.Path, required=True)
p.add_argument('--java', type=pathlib.Path, required=True)
p.add_argument('--modes', nargs='+', required=True)
p.add_argument('--production', type=pathlib.Path, required=True, help='Freshly built production jarJar artifact')
opts = p.parse_args()
ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT, RUNTIME = opts.instance.resolve(), opts.runtime.resolve()
production = opts.production.resolve()
with zipfile.ZipFile(production) as jar:
    if not re.search(r'modId\s*=\s*"the_long_travail"', jar.read('META-INF/mods.toml').decode('utf8')):
        raise SystemExit('Production artifact is not The Long Travail')
    if 'META-INF/jarjar/metadata.json' not in jar.namelist():
        raise SystemExit('Requires the bundled jarJar artifact')
if not OUT.is_relative_to(ROOT / 'build') or not (OUT / 'source-integrity.json').exists():
    raise SystemExit('Requires a completed isolated preface-fullpack run under project/build')
if not json.loads((OUT / 'source-integrity.json').read_text())['unchanged']:
    raise SystemExit('Original environment integrity was not confirmed')
MC = RUNTIME.parents[1]
meta = json.loads((RUNTIME / (RUNTIME.name + '.json')).read_text(encoding='utf-8-sig'))


def allowed(rules):
    if not rules:
        return True
    result = False
    for rule in rules:
        os = rule.get('os', {})
        if os.get('name', 'windows') != 'windows' or os.get('arch', 'x86_64') not in ('x86_64', 'amd64') or rule.get('features'):
            continue
        result = rule['action'] == 'allow'
    return result


libraries = []
for lib in meta['libraries']:
    if not allowed(lib.get('rules')):
        continue
    artifact = lib.get('downloads', {}).get('artifact')
    if artifact:
        path = MC / 'libraries' / artifact['path']
    else:
        group, name, version, *classifier = lib['name'].split(':')
        path = MC / 'libraries' / group.replace('.', '/') / name / version / (name + '-' + version + ('-' + classifier[0] if classifier else '') + '.jar')
    if not path.is_file():
        raise SystemExit('Missing library: ' + str(path))
    libraries.append(str(path))
libraries.append(str(RUNTIME / (RUNTIME.name + '.jar')))
values = {
    'natives_directory': str(RUNTIME / (RUNTIME.name + '-natives')),
    'launcher_name': 'TLT-isolated-acceptance', 'launcher_version': '1',
    'classpath': ';'.join(libraries), 'classpath_separator': ';', 'library_directory': str(MC / 'libraries'),
    'version_name': RUNTIME.name, 'auth_player_name': 'DiaryAcceptance', 'game_directory': str(OUT),
    'assets_root': str(MC / 'assets'), 'assets_index_name': meta['assetIndex']['id'],
    'auth_uuid': uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:DiaryAcceptance').digest(), version=3).hex,
    'auth_access_token': '0', 'clientid': '', 'auth_xuid': '', 'user_type': 'legacy', 'version_type': 'release',
}


def args(items):
    result = []
    for item in items:
        if isinstance(item, dict):
            if not allowed(item.get('rules')):
                continue
            item = item['value']
        result.extend(re.sub(r'\$\{([^}]+)\}', lambda m: values[m[1]], v) for v in (item if isinstance(item, list) else [item]))
    return result


results = []
for scenario in opts.modes:
    if scenario == 'network-off':
        from network_runner import run_network
        run_network(ROOT, OUT, RUNTIME, opts.java, meta, values, args)
        continue
    mode, shader = scenario.split('-')
    if mode not in ['boundless', 'lantern', 'icarus', 'visual'] or shader not in ['on', 'off']:
        raise SystemExit('Invalid scenario: ' + scenario)
    evidence = OUT / 'acceptance-results' / scenario
    attempt = 1
    while evidence.exists():
        attempt += 1
        evidence = OUT / 'acceptance-results' / (scenario + '-' + str(attempt))
    evidence.mkdir(parents=True, exist_ok=False)
    with (evidence / 'build.log').open('w') as log:
        subprocess.run([str(ROOT / 'gradlew.bat'), '-I', 'tools/structure-regression/client-fullpack.init.gradle',
                        '-PacceptanceMode=' + mode, 'reobfFullpackClientJar', '--offline', '--no-daemon'],
                       cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
    artifact = ROOT / 'build/client-fixture' / ('travail-' + mode + '-acceptance.jar')
    with zipfile.ZipFile(artifact) as jar:
        assert json.loads(jar.read('pack.mcmeta'))['pack']['pack_format'] == 15
    for previous in (OUT / 'mods').glob('travail-*-acceptance.jar'):
        shutil.move(previous, evidence / ('prior-' + previous.name))
    shutil.copy2(artifact, OUT / 'mods' / artifact.name)
    # Identify previous versions by mod ID, including localized filenames.
    for previous in (OUT / 'mods').glob('*.jar'):
        with zipfile.ZipFile(previous) as jar:
            toml = jar.read('META-INF/mods.toml').decode('utf8') if 'META-INF/mods.toml' in jar.namelist() else ''
        if re.search(r'modId\s*=\s*"the_long_travail"', toml):
            shutil.move(previous, evidence / ('prior-' + previous.name))
    shutil.copy2(production, OUT / 'mods' / production.name)
    (evidence / 'mod-sha256.json').write_text(json.dumps({f.name: hashlib.sha256(f.read_bytes()).hexdigest() for f in (OUT / 'mods').glob('*.jar')}, ensure_ascii=False, indent=2), encoding='utf8')
    world = OUT / 'saves/visual-world'
    if world.exists():
        # Keep every previous test world; never reuse modified world state across scenarios.
        shutil.move(world, evidence / 'prior-world')
    shutil.copytree(ROOT / 'build/reading-client/saves/visual-world', world,
                    ignore=shutil.ignore_patterns('playerdata', 'session.lock'))
    config = OUT / 'config/oculus.properties'
    text = config.read_text(encoding='utf8')
    text = re.sub(r'^enableShaders=.*$', 'enableShaders=' + str(shader == 'on').lower(), text, flags=re.M)
    config.write_text(text, encoding='utf8')
    (evidence / 'oculus.properties').write_text(text, encoding='utf8')
    screenshots = OUT / 'screenshots'
    before_shots = {str(f): f.stat().st_mtime_ns for f in screenshots.glob('*.png')}
    result_file = OUT / ('result.txt' if mode == 'visual' else mode + '-client-result.txt')
    if result_file.exists():
        shutil.copy2(result_file, evidence / 'prior-result.txt')
    result_file.write_text('NOT_RUN', encoding='utf8')
    command = [str(opts.java), '-Xms1G', '-Xmx8G', '-Dfile.encoding=UTF-8', '-Dtravail.lantern.shader=' + str(shader == 'on').lower()]
    command += args(meta['arguments']['jvm']) + [meta['mainClass']] + args(meta['arguments']['game'])
    command += ['--width', '1280', '--height', '900']
    with (evidence / 'console.log').open('w', encoding='utf8') as log:
        process = subprocess.Popen(command, cwd=OUT, stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        (evidence / 'process.json').write_text(json.dumps({'pid': process.pid, 'scenario': scenario}), encoding='utf8')
        print('START', scenario, 'PID', process.pid, flush=True)
        try:
            code = process.wait(timeout=1200)
        except subprocess.TimeoutExpired:
            process.kill()
            code = process.wait()
            (evidence / 'timeout.txt').write_text('Stopped only the child launched for this scenario.')
    result = result_file.read_text(encoding='utf8')
    (evidence / 'result.txt').write_text(result, encoding='utf8')
    for f in screenshots.glob('*.png'):
        if before_shots.get(str(f)) != f.stat().st_mtime_ns:
            shutil.copy2(f, evidence / f.name)
    if (OUT / 'logs/latest.log').exists():
        shutil.copy2(OUT / 'logs/latest.log', evidence / 'latest.log')
    if (OUT / 'logs/debug.log').exists():
        shutil.copy2(OUT / 'logs/debug.log', evidence / 'debug.log')
    results.append({'scenario': scenario, 'exitCode': code, 'result': result})
    (OUT / 'acceptance-results/summary.json').write_text(json.dumps(results, indent=2), encoding='utf8')
    print('END', scenario, code, result, flush=True)
    if code != 0 or not result.startswith('PASS'):
        raise SystemExit('Scenario failed; preserve evidence and investigate before continuing: ' + scenario)

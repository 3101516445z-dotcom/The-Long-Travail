"""Two independent packaged clients and an integrated LAN server over actual TCP."""
import hashlib
import json
import pathlib
import shutil
import socket
import subprocess
import time
import tomllib
import uuid
import zipfile


def run_network(root, parent, runtime, java, meta, values, expand_args):
    original_values = dict(values)
    out = parent / 'acceptance-results/network'
    attempt = 1
    while out.exists():
        attempt += 1
        out = parent / ('acceptance-results/network-' + str(attempt))
    out.mkdir(parents=True)
    with (out / 'build.log').open('w') as log:
        subprocess.run([str(root / 'gradlew.bat'), '-I', 'tools/structure-regression/client-fullpack.init.gradle',
                        '-PacceptanceMode=network', 'reobfFullpackClientJar', '--offline', '--no-daemon'],
                       cwd=root, stdout=log, stderr=subprocess.STDOUT, check=True)
    # Use only the required Curios dependency: the full modpack is tested separately.
    jars = [root / 'build/libs/The_Long_Travail-1.0.0.jar', root / 'build/client-fixture/travail-network-acceptance.jar']
    curios = []
    for candidate in (runtime / 'mods').glob('*curios*.jar'):
        with zipfile.ZipFile(candidate) as jar:
            metadata = jar.read('META-INF/mods.toml').decode('utf8')
            if any(mod.get('modId') == 'curios' for mod in tomllib.loads(metadata.replace('\r', '')).get('mods', [])):
                curios.append(candidate)
    if len(curios) != 1:
        raise RuntimeError('Expected exactly one Curios jar')
    jars.extend(curios)
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    children, logs = [], []
    try:
        for role, name in [('host', 'NetworkHost'), ('guest', 'NetworkGuest')]:
            game = out / role
            (game / 'mods').mkdir(parents=True)
            for jar in jars:
                shutil.copy2(jar, game / 'mods' / jar.name)
            (game / 'options.txt').write_text('onboardAccessibility:false\npauseOnLostFocus:false\nlang:zh_cn\nguiScale:2\nmaxFps:30\nrenderDistance:3\nsimulationDistance:5\ntutorialStep:none\n', encoding='utf8')
            if role == 'host':
                shutil.copytree(root / 'build/reading-client/saves/visual-world', game / 'saves/visual-world',
                                ignore=shutil.ignore_patterns('playerdata', 'session.lock'))
            values['game_directory'] = str(game)
            values['auth_player_name'] = name
            values['auth_uuid'] = uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:' + name).encode()).digest(), version=3).hex
            command = [str(java), '-Xms512M', '-Xmx3G', '-Dfile.encoding=UTF-8', '-Dacceptance.host=' + str(role == 'host').lower(),
                       '-Dacceptance.shared=' + str(out), '-Dacceptance.port=' + str(port)]
            command += expand_args(meta['arguments']['jvm']) + [meta['mainClass']] + expand_args(meta['arguments']['game'])
            command += ['--width', '960', '--height', '540']
            log = (out / (role + '.log')).open('w', encoding='utf8')
            logs.append(log)
            child = subprocess.Popen(command, cwd=game, stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
            children.append(child)
            print('NETWORK_START', role, child.pid, flush=True)
        (out / 'processes.json').write_text(json.dumps({'pids': [p.pid for p in children], 'port': port}), encoding='utf8')
        deadline = time.monotonic() + 960
        while any(p.poll() is None for p in children) and time.monotonic() < deadline:
            if list(out.glob('*-failed')):
                break
            if any(p.poll() not in (None, 0) for p in children):
                break
            time.sleep(2)
    finally:
        for child in children:
            if child.poll() is None:
                child.kill()
            child.wait()
        for log in logs:
            log.close()
        values.clear()
        values.update(original_values)
    results = {role: (out / (role + '-result')).read_text() if (out / (role + '-result')).exists() else 'NOT_PASS' for role in ['host', 'guest']}
    results['exitCodes'] = [p.returncode for p in children]
    results['environment'] = 'Forge plus final TLT jar and Curios, two real clients, local TCP'
    (out / 'summary.json').write_text(json.dumps(results, indent=2), encoding='utf8')
    print('NETWORK_END', json.dumps(results), flush=True)
    if any(not str(results[role]).startswith('PASS') for role in ['host', 'guest']) or any(results['exitCodes']):
        raise RuntimeError('Network acceptance failed; inspect ' + str(out))

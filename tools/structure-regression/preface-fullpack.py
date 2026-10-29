"""Run packaged diary UI acceptance checks in a copy of an existing full-pack environment."""
import argparse, hashlib, json, pathlib, re, shutil, subprocess, sys, uuid, zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--pack', type=pathlib.Path, required=True)
parser.add_argument('--output', type=pathlib.Path, required=True)
parser.add_argument('--java', type=pathlib.Path, required=True)
parser.add_argument('--runtime', type=pathlib.Path, required=True)
parser.add_argument('--resume', action='store_true')
options = parser.parse_args()
ROOT = pathlib.Path(__file__).resolve().parents[2]
PACK, OUT, JAVA = options.pack.resolve(), options.output.resolve(), options.java.resolve()
# Validate our artifacts before copying or launching a full pack. Forge turns a
# missing pack.mcmeta into a blocking loading-warning screen even for code-only mods.
for artifact in [ROOT/'build/libs/The_Long_Travail-0.1.3.jar', ROOT/'build/preface-fixture/travail-preface-acceptance.jar']:
    with zipfile.ZipFile(artifact) as jar:
        metadata = json.loads(jar.read('pack.mcmeta'))
        if metadata.get('pack', {}).get('pack_format') != 15:
            raise SystemExit('Expected Minecraft 1.20.1 pack metadata: ' + str(artifact))
if OUT == PACK or not OUT.is_relative_to(ROOT / 'build'):
    raise SystemExit('Output must be a fresh isolated directory under project/build')
if OUT.exists() and not options.resume: raise SystemExit('Output already exists; choose another directory or --resume')
if options.resume and not (OUT/'process.json').exists(): raise SystemExit('Resume requires an existing isolated run')

def hash_file(p):
    with p.open('rb') as f: return hashlib.file_digest(f, 'sha256').hexdigest()

def source_manifest():
    files = []
    for folder in ['mods', 'config', 'defaultconfigs', 'kubejs']:
        files.extend(p for p in (PACK / folder).rglob('*') if p.is_file())
    if (PACK / 'options.txt').is_file(): files.append(PACK / 'options.txt')
    return {str(p.relative_to(PACK)): hash_file(p) for p in files}

if not options.resume:
    copy_folders = ['mods', 'config', 'defaultconfigs', 'kubejs', 'resourcepacks', 'shaderpacks',
                    'patchouli_books', 'fancymenu_data', 'moonlight-global-datapacks', 'quests_structures', 'tlm_custom_pack']
    required = sum(p.stat().st_size for folder in copy_folders for p in (PACK/folder).rglob('*') if p.is_file())
    if shutil.disk_usage(ROOT).free < required + 1024**3:
        raise SystemExit('Not enough free space for an isolated copy plus 1 GiB runtime reserve; reuse a stopped test with --resume')

OUT.mkdir(parents=True, exist_ok=options.resume)
baseline = source_manifest()
(OUT / 'source-before.json').write_text(json.dumps(baseline, ensure_ascii=False, indent=2), encoding='utf8')
if not options.resume:
    for folder in ['mods', 'config', 'defaultconfigs', 'kubejs', 'resourcepacks', 'shaderpacks',
                   'patchouli_books', 'fancymenu_data', 'moonlight-global-datapacks', 'quests_structures', 'tlm_custom_pack']:
        if (PACK / folder).is_dir(): shutil.copytree(PACK / folder, OUT / folder)
    settings = (PACK / 'options.txt').read_text(encoding='utf8') if (PACK / 'options.txt').is_file() else ''
    for key, value in {'onboardAccessibility':'false', 'pauseOnLostFocus':'false', 'lang':'zh_cn',
                       'guiScale':'2', 'maxFps':'60', 'renderDistance':'4', 'simulationDistance':'5',
                       'tutorialStep':'none'}.items():
        settings = re.sub(r'^'+key+r':.*$', '', settings, flags=re.M) + '\n'+key+':'+value
    (OUT / 'options.txt').write_text(settings+'\n', encoding='utf8')
    old = list((OUT / 'mods').glob('*[Tt]ravail*.jar'))
    if len(old) != 1: raise SystemExit('Expected exactly one TLT jar: '+str(old))
    (OUT / 'original-artifacts').mkdir()
    shutil.move(old[0], OUT / 'original-artifacts' / old[0].name)
    # Remove only the prior environment's instrumentation, keeping its gameplay mod selection.
    for trace in (OUT / 'mods').glob('phosph-trace-*.jar'):
        shutil.move(trace, OUT / 'original-artifacts' / trace.name)
    shutil.copytree(ROOT / 'build/abundance-server/abundance', OUT / 'saves/preview',
                    ignore=shutil.ignore_patterns('playerdata', 'session.lock'))
for source in [ROOT/'build/libs/The_Long_Travail-0.1.3.jar', ROOT/'build/preface-fixture/travail-preface-acceptance.jar']:
    shutil.copy2(source, OUT / 'mods' / source.name)
manifest = {p.name: hash_file(p) for p in (OUT / 'mods').glob('*.jar')}
(OUT/'mod-sha256.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf8')
RUNTIME = options.runtime.resolve()
MC = RUNTIME.parents[1]
meta = json.loads((RUNTIME/(RUNTIME.name+'.json')).read_text(encoding='utf-8-sig'))

def allowed(rules):
    if not rules: return True
    result = False
    for rule in rules:
        os = rule.get('os', {})
        if os.get('name','windows') != 'windows' or os.get('arch','x86_64') not in ('x86_64','amd64'): continue
        if rule.get('features'): continue
        result = rule['action'] == 'allow'
    return result

libraries = []
for lib in meta['libraries']:
    if not allowed(lib.get('rules')): continue
    artifact = lib.get('downloads',{}).get('artifact')
    if artifact: path = MC/'libraries'/artifact['path']
    else:
        bits = lib['name'].split(':'); group,name,version = bits[:3]
        classifier = '-'+bits[3] if len(bits)>3 else ''
        path = MC/'libraries'/group.replace('.','/')/name/version/(name+'-'+version+classifier+'.jar')
    if not path.is_file(): raise SystemExit('Missing library '+str(path))
    libraries.append(str(path))
libraries.append(str(RUNTIME/(RUNTIME.name+'.jar')))
values = {
    'natives_directory':str(RUNTIME/(RUNTIME.name+'-natives')),
    'launcher_name':'TLT-isolated-diary','launcher_version':'1',
    'classpath':';'.join(libraries),'classpath_separator':';', 'library_directory':str(MC/'libraries'),
    'version_name':PACK.name,'auth_player_name':'DiaryAcceptance','game_directory':str(OUT),
    'assets_root':str(MC/'assets'),'assets_index_name':meta['assetIndex']['id'],
    'auth_uuid':uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:DiaryAcceptance').digest(),version=3).hex,
    'auth_access_token':'0','clientid':'','auth_xuid':'','user_type':'legacy','version_type':'release',
}
def expand(arg): return re.sub(r'\$\{([^}]+)\}', lambda m:values[m[1]], arg)
def args(items):
    result = []
    for item in items:
        if isinstance(item,dict):
            if not allowed(item.get('rules')): continue
            item = item['value']
        result.extend(expand(v) for v in (item if isinstance(item,list) else [item]))
    return result

command = [str(JAVA), '-Xms1G', '-Xmx8G', '-Dfile.encoding=UTF-8', '-Ddiary.fullpack=true']
command += args(meta['arguments']['jvm']) + [meta['mainClass']] + args(meta['arguments']['game'])
command += ['--width','1280','--height','900']
(OUT/'result.txt').write_text('NOT_RUN',encoding='utf8')
with (OUT/'console.log').open('w',encoding='utf8') as log:
    process = subprocess.Popen(command,cwd=OUT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    (OUT/'process.json').write_text(json.dumps({'pid':process.pid,'modCount':len(manifest),'java':str(JAVA),
        'pack':str(PACK),'isolated':str(OUT)}),encoding='utf8')
    print('Isolated full-pack PID',process.pid,'mods',len(manifest),flush=True)
    try: code = process.wait(timeout=1200)
    except subprocess.TimeoutExpired:
        process.kill(); code=process.wait()
        (OUT/'timeout.txt').write_text('20 minute guard stopped only the child launched by this script.')
    (OUT/'exit.json').write_text(json.dumps({'exitCode':code}),encoding='utf8')
after = source_manifest()
unchanged = after == baseline
(OUT/'source-integrity.json').write_text(json.dumps({'unchanged':unchanged,'files':len(baseline),
    'changed':[k for k in baseline.keys()|after.keys() if baseline.get(k)!=after.get(k)]},ensure_ascii=False,indent=2),encoding='utf8')
result = (OUT/'result.txt').read_text().strip()
print('Exit',code,'result',result,'source test environment unchanged',unchanged,flush=True)
sys.exit(0 if code==0 and result.startswith('PASS') and unchanged else 1)

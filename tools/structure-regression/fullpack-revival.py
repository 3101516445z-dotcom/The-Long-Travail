"""在整包副本中运行死亡界面与复活测试。"""
import argparse, hashlib, json, pathlib, re, shutil, subprocess, sys, uuid
parser = argparse.ArgumentParser()
parser.add_argument('--pack', type=pathlib.Path, required=True)
parser.add_argument('--output', type=pathlib.Path, required=True)
parser.add_argument('--java', type=pathlib.Path, required=True)
parser.add_argument('--resume', action='store_true')
parser.add_argument('--hardcore-only', action='store_true')
options = parser.parse_args()
ROOT = pathlib.Path(__file__).resolve().parents[2]
PACK = options.pack.resolve()
OUT = options.output.resolve()
JAVA = options.java.resolve()
MC = PACK.parents[1]
if not options.resume:
    if PACK == OUT or PACK in OUT.parents and not ROOT in OUT.parents:
        raise SystemExit('Output must be an isolated project directory or outside the installed pack')
    OUT.mkdir(parents=True)
    folders = ['mods', 'config', 'defaultconfigs', 'kubejs', 'resourcepacks', 'shaderpacks',
               'patchouli_books', 'fancymenu_data', 'moonlight-global-datapacks',
               'quests_structures', 'tlm_custom_pack']
    for folder in folders:
        if (PACK / folder).is_dir():
            shutil.copytree(PACK / folder, OUT / folder)
    if (PACK / 'options.txt').exists():
        shutil.copy2(PACK / 'options.txt', OUT / 'options.original.txt')
        settings = (PACK / 'options.txt').read_text(encoding='utf8')
    else:
        settings = ''
    for key, value in {'onboardAccessibility':'false', 'pauseOnLostFocus':'false',
                       'lang':'zh_cn', 'guiScale':'2', 'maxFps':'60',
                       'renderDistance':'4', 'simulationDistance':'5'}.items():
        settings = re.sub(r'^'+key+r':.*$', '', settings, flags=re.M)
        settings += '\n'+key+':'+value
    (OUT / 'options.txt').write_text(settings+'\n', encoding='utf8')
    old = list((OUT / 'mods').glob('*[Tt]ravail*.jar'))
    if len(old) != 1:
        raise SystemExit('Expected one installed TLT jar, found '+str(len(old)))
    (OUT / 'original-artifacts').mkdir()
    shutil.move(old[0], OUT / 'original-artifacts' / old[0].name)
    release = ROOT / 'build/libs/The_Long_Travail-0.1.3.jar'
    fixture = ROOT / 'build/revival-fixture/travail-revival-acceptance.jar'
    for source in [release, fixture]:
        shutil.copy2(source, OUT / 'mods' / source.name)
    import nbtlib, tomllib
    world = ROOT / 'build/coloring-standalone/coloring-test'
    for name, hardcore in [('revival-normal', 0), ('revival-hardcore', 1)]:
        target = OUT / 'saves' / name
        shutil.copytree(world, target, ignore=shutil.ignore_patterns('playerdata', 'session.lock'))
        data = nbtlib.load(target / 'level.dat')
        data['Data']['hardcore'] = nbtlib.Byte(hardcore)
        data['Data']['GameType'] = nbtlib.Int(0)
        data['Data']['allowCommands'] = nbtlib.Byte(1)
        data['Data']['LevelName'] = nbtlib.String(name)
        data['Data'].pop('Player', None)
        data.save()
    config = (ROOT / 'local-archive/测试记录/20261026-acquisition/general.toml').read_text(encoding='utf-8-sig')
    defaults = json.loads((ROOT / 'src/main/resources/the_long_travail/default_loot.json').read_text(encoding='utf8'))
    for key, rule in defaults.items():
        header = '[loot.tables."'+key+'"]'
        start = config.index(header)
        end = config.find('\n[', start+len(header))
        if end < 0: end = len(config)
        section = config[start:end]
        section = re.sub(r'chance = [^\n]+', 'chance = '+str(rule['chance']), section)
        section = re.sub(r'items = \[.*?\]', 'items = '+json.dumps(rule['items']), section, flags=re.S)
        config = config[:start]+section+config[end:]
    parsed = tomllib.loads(config)
    for key, rule in defaults.items():
        assert parsed['loot']['tables'][key]['chance'] == rule['chance']
        assert parsed['loot']['tables'][key]['items'] == rule['items']
    (OUT / 'config/the_long_travail/general.toml').write_text(config, encoding='utf8')
else:
    if not (OUT / 'process.json').is_file(): raise SystemExit('Missing isolated run')
    shutil.copy2(ROOT / 'build/revival-fixture/travail-revival-acceptance.jar', OUT / 'mods/travail-revival-acceptance.jar')
manifest = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (OUT / 'mods').glob('*.jar')}
(OUT / 'mod-sha256.json').write_text(json.dumps(manifest, indent=2), encoding='utf8')
meta=json.loads((PACK/(PACK.name+'.json')).read_text(encoding='utf-8-sig'))
def allowed(rules):
    if not rules: return True
    result=False
    for rule in rules:
        os=rule.get('os',{})
        if os.get('name','windows')!='windows' or os.get('arch','x86_64') not in ('x86_64','amd64'): continue
        if rule.get('features'): continue
        result=rule['action']=='allow'
    return result
libraries=[]
for lib in meta['libraries']:
    if not allowed(lib.get('rules')): continue
    artifact=lib.get('downloads',{}).get('artifact')
    if artifact: path=MC/'libraries'/artifact['path']
    else:
        bits=lib['name'].split(':'); group,name,version=bits[:3]
        classifier='-'+bits[3] if len(bits)>3 else ''
        path=MC/'libraries'/group.replace('.','/')/name/version/(name+'-'+version+classifier+'.jar')
    if not path.is_file(): raise SystemExit('Missing library: '+str(path))
    libraries.append(str(path))
libraries.append(str(PACK/(PACK.name+'.jar')))
values={
    'natives_directory':str(PACK/(PACK.name+'-natives')),
    'launcher_name':'TLT-local-regression','launcher_version':'1',
    'classpath':';'.join(libraries),'classpath_separator':';',
    'library_directory':str(MC/'libraries'),'version_name':PACK.name,
    'auth_player_name':'RevivalAcceptance','game_directory':str(OUT),
    'assets_root':str(MC/'assets'),'assets_index_name':meta['assetIndex']['id'],
    'auth_uuid':uuid.UUID(bytes=__import__('hashlib').md5(b'OfflinePlayer:RevivalAcceptance').digest(),version=3).hex,
    'auth_access_token':'0','clientid':'','auth_xuid':'','user_type':'legacy','version_type':'release',
}
def expand(arg):
    return re.sub(r'\$\{([^}]+)\}',lambda m:values[m[1]],arg)
def args(items):
    result=[]
    for item in items:
        if isinstance(item,dict):
            if not allowed(item.get('rules')): continue
            item=item['value']
        result.extend(expand(v) for v in (item if isinstance(item,list) else [item]))
    return result
command=[str(JAVA), '-Xms1G', '-Xmx8G', '-Dfile.encoding=UTF-8']
if options.hardcore_only: command.append('-Drevival.startWorld=1')
command += args(meta['arguments']['jvm'])+[meta['mainClass']]+args(meta['arguments']['game'])
command += ['--width', '1000', '--height', '720']
(OUT/'client-result.txt').write_text('NOT_RUN',encoding='utf8')
with (OUT/'console.log').open('w',encoding='utf8') as log:
    process=subprocess.Popen(command,cwd=OUT,stdout=log,stderr=subprocess.STDOUT,
                             creationflags=subprocess.CREATE_NO_WINDOW)
    (OUT/'process.json').write_text(json.dumps({'pid':process.pid,'modCount':len(manifest),
        'java':str(JAVA),'pack':str(PACK),'isolated':str(OUT)}),encoding='utf8')
    print('Isolated full-pack PID',process.pid,'mods',len(manifest),flush=True)
    code=process.wait()
    (OUT/'exit.json').write_text(json.dumps({'exitCode':code}),encoding='utf8')
    result=(OUT/'client-result.txt').read_text().strip()
    print('Exit',code,'result',result,flush=True)
    sys.exit(0 if code==0 and result=='PASS' else 1)

import argparse,hashlib,json,pathlib,re,shutil,subprocess,sys,uuid
parser=argparse.ArgumentParser()
parser.add_argument('--pack',type=pathlib.Path,required=True)
parser.add_argument('--output',type=pathlib.Path,required=True)
parser.add_argument('--java',type=pathlib.Path,required=True)
options=parser.parse_args()
ROOT=pathlib.Path(__file__).resolve().parents[2]
PACK=options.pack.resolve();OUT=options.output.resolve();JAVA=options.java.resolve();MC=PACK.parents[1]
if not (OUT/'saves/revival-normal/level.dat').is_file() or not (OUT/'original-artifacts').is_dir(): raise SystemExit('Expected isolated test pack')
old=OUT/'mods/travail-revival-acceptance.jar'
if old.exists(): shutil.move(old,OUT/'original-artifacts/revival-fixture-before-carrying.jar')
for source in [ROOT/'build/libs/The_Long_Travail-0.1.3.jar',ROOT/'build/carrying-fixture/travail-carrying-preview.jar']:
 shutil.copy2(source,OUT/'mods'/source.name)
manifest={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (OUT/'mods').glob('*.jar')}
(OUT/'carrying-mod-sha256.json').write_text(json.dumps(manifest,indent=2),encoding='utf8')
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
command += args(meta['arguments']['jvm'])+[meta['mainClass']]+args(meta['arguments']['game'])
command += ['--width', '1280', '--height', '900']
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

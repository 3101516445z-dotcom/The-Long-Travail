import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {spawn} from 'node:child_process';
import {guardProcess} from './process-guard.mjs';
const root=process.cwd(),dir=path.join(root,'build/release-validation');
const java=process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME,'bin/java.exe') : 'java';
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const manifest=JSON.parse(fs.readFileSync(path.join(dir,'addon-inputs.json'),'utf8'));
const evidence=path.join(dir,'evidence-'+Date.now());fs.mkdirSync(evidence);
const report={status:'NOT_RUN',runs:[],addons:manifest};
for(const name of ['The_Long_Travail-0.1.2-fix.jar','travail-release-smoke-1.jar']){const src=path.join(root,'build/libs',name),dest=path.join(dir,'mods',name);fs.copyFileSync(src,dest);manifest.push({source:src,copy:dest,sha256:hash(src),modified:false});}
for(const stage of ['before','after']){
    fs.writeFileSync(path.join(dir,'restart-result.txt'),'NOT_RUN');
    const output=fs.createWriteStream(path.join(evidence,stage+'.log'));
    const child=spawn(java,['-Xmx2G','-Dtravail.restart.stage='+stage,'@libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt','--nogui'],{cwd:dir,windowsHide:true});
    const guard=guardProcess(child,{timeoutMs:180000});child.stdout.pipe(output,{end:false});child.stderr.pipe(output,{end:false});
    const code=await new Promise((resolve,reject)=>{child.once('error',reject);child.once('close',resolve);});await new Promise(resolve=>output.end(resolve));
    const result=fs.readFileSync(path.join(dir,'restart-result.txt'),'utf8');fs.writeFileSync(path.join(evidence,stage+'-result.txt'),result);
    report.runs.push({stage,exitCode:code,interruption:guard.reason,result});
    if(code!==0||guard.reason||!result.startsWith('PASS')){report.status='FAIL';break;}
}
report.originalsUnchanged=manifest.every(x=>hash(x.source)===x.sha256&&hash(x.copy)===x.sha256);
if(report.runs.length===2&&report.status!=='FAIL'&&report.originalsUnchanged)report.status='PASS';
fs.writeFileSync(path.join(evidence,'report.json'),JSON.stringify(report,null,2));console.log(report.status+': '+evidence);process.exitCode=report.status==='PASS'?0:1;

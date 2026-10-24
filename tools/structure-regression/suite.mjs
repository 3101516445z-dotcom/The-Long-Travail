import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {spawn} from 'node:child_process';
import {guardProcess} from './process-guard.mjs';

const plans = {
    core: [['01','after'],['02','after'],['03','after']],
    config: [['10','after'],['11','after']],
    combat: [['12','after']],
    maid: [['13','after']],
    restart: [['14','before'],['14','after']],
    structures: [['17','before'],['17','after']],
    soak: [['16','after']],
    'maid-social': [['15','after']],
};
const [name,...options]=process.argv.slice(2);
if (name==='--list' && !options.length) {
    console.log('core | config | combat | maid | restart | structures | soak | maid-social | all\n--build: build once before the first server run. All runs are sequential.');
    process.exit(0);
}
if (!(Object.hasOwn(plans, name) || name==='all') || options.some(o=>o!=='--build') || options.length>1) {
    console.error('Usage: node tools/structure-regression/suite.mjs core|config|combat|maid|restart|structures|soak|maid-social|all [--build]');
    process.exit(2);
}
const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'../..');
const dir=path.join(root,'build/structure-suites',new Date().toISOString().replace(/[:.]/g,'-')+'-'+name+'-'+process.pid);
fs.mkdirSync(dir,{recursive:true});
const plan=name==='all'?Object.entries(plans).filter(([key])=>!['maid-social','soak','structures'].includes(key)).flatMap(([,value])=>value):plans[name];
const report={suite:name,status:'NOT_RUN',startedAt:new Date().toISOString(),plan,runs:[]};
const save=()=>fs.writeFileSync(path.join(dir,'report.json'),JSON.stringify(report,null,2)+'\n');
save();
try {
    for (const [index,[round,phase]] of plan.entries()) {
        let output='';
        const args=[path.join(here,'run.mjs'),round,phase,...(index===0&&options.includes('--build')?['--build']:[])];
        const code=await new Promise((resolve,reject)=>{
            const child=spawn(process.execPath,args,{cwd:root,windowsHide:true});
            guardProcess(child, {timeoutMs: 20 * 60_000, onStop: reason => {report.interruption=reason;save();}});
            child.stdout.on('data',data=>{output+=data;process.stdout.write(data);});
            child.stderr.on('data',data=>{output+=data;process.stderr.write(data);});
            child.once('error',reject);child.once('close',resolve);
        });
        fs.writeFileSync(path.join(dir,`${index+1}-${round}-${phase}.log`),output);
        const match=[...output.matchAll(/(?:PASS|FAIL): (.+report\.json)/g)].at(-1);
        const childReport=match?path.resolve(match[1].trim()):null;
        if(childReport&&!childReport.startsWith(path.join(root,'build/structure-reports')+path.sep))throw Error('Unexpected report path');
        const result=childReport?JSON.parse(fs.readFileSync(childReport,'utf8')):null;
        const fingerprint=result?.inputs?crypto.createHash('sha256').update(JSON.stringify(result.inputs)).digest('hex'):null;
        if (index===0) report.inputFingerprint=fingerprint;
        const sameInputs=fingerprint!==null&&fingerprint===report.inputFingerprint;
        report.runs.push({round,phase,exitCode:code,report:childReport,status:result?.status??'FAIL',sameInputs});save();
        if (report.interruption) throw Error(report.interruption);
        if (!sameInputs) throw Error('Suite inputs changed between server runs');
        if(code!==0||result?.status!=='PASS')throw Error(`Stopped at ${round} ${phase}`);
    }
    report.status='PASS';
} catch(error) {report.status='FAIL';report.error=String(error);process.exitCode=1;}
report.finishedAt=new Date().toISOString();save();
console.log(`${report.status}: ${path.join(dir,'report.json')}`);

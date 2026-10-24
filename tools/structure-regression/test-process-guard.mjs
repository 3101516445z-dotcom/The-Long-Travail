import assert from 'node:assert/strict';
import {spawn, spawnSync} from 'node:child_process';
import {once} from 'node:events';
import {guardProcess} from './process-guard.mjs';
for (const name of ['constructor','toString','__proto__','unknown']) {
    const r=spawnSync(process.execPath,['tools/structure-regression/suite.mjs',name],{encoding:'utf8'});
    assert.equal(r.status,2,name);
}
for (const mode of ['timeout','interrupt']) {
    const child=spawn(process.execPath,['-e',`const {spawn}=require('child_process'); const c=spawn(process.execPath,['-e','setInterval(()=>{},1000)'],{stdio:'ignore'}); console.log(c.pid); setInterval(()=>{},1000);`],{windowsHide:true});
    const closed=once(child,'close');
    const [data]=await once(child.stdout,'data');
    const descendant=Number(String(data).trim());
    const guard=guardProcess(child,{timeoutMs:mode==='timeout'?200:5000});
    if(mode==='interrupt') process.emit('SIGINT');
    await closed;
    assert.equal(guard.reason,mode==='timeout'?'timeout':'SIGINT');
    await new Promise(resolve=>setTimeout(resolve,300));
    assert.throws(()=>process.kill(descendant,0),'descendant should be gone');
}
const normal=spawn(process.execPath,['-e','process.exit(0)'],{windowsHide:true});
const done=once(normal,'close');const guard=guardProcess(normal,{timeoutMs:5000});
assert.equal((await done)[0],0);assert.equal(guard.reason,null);
console.log('PASS: 4 invalid names, timeout tree cleanup, interrupt tree cleanup, normal completion');

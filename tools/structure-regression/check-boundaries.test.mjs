import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import assert from 'node:assert/strict';
import {check} from './check-boundaries.mjs';
const root=fs.mkdtempSync(path.join(os.tmpdir(),'tlt-boundary-'));
const write=(p,s)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,s)};
try {
 const java='src/main/java/com/thelongtravail/';
 write(java+'mixin/Example.java','class Example {}');
 for(const name of ['mixins.the_long_travail.json','mixins.the_long_travail.compat.json'])write('src/main/resources/'+name,JSON.stringify({package:'com.thelongtravail.mixin',mixins:['Example']}));
 assert.deepEqual(check(root),[]);
 write(java+'core/Leak.java','import com.thelongtravail.compat.maid.MaidAccessories;');assert(check(root).some(x=>x.includes('direct maid')));
 write(java+'core/Leak.java','import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;');assert(check(root).some(x=>x.includes('external API')));
 write(java+'core/Leak.java','class Leak {}');
 write(java+'core/Leak.java','import example.optional.Api;');assert(check(root).some(x=>x.includes('external API')));
 write(java+'core/Leak.java','com.thelongtravail.compat.example.Internal.call();');assert(check(root).some(x=>x.includes('unlisted compatibility')));
 write(java+'core/Leak.java','com.thelongtravail.compat.CompatTooltips.call();');assert(check(root).some(x=>x.includes('missing compatibility bridge')));
 write(java+'compat/CompatTooltips.java','class CompatTooltips {}');assert.deepEqual(check(root),[]);
 write(java+'core/Leak.java','class Leak {}');
 write(java+'abyss/copy/Leak.java','import com.thelongtravail.compat.fluid.FluidAeStore;');assert(check(root).some(x=>x.includes('direct fluid')));
 write(java+'abyss/copy/Leak.java','String id="beyonddimensions:example";');assert(check(root).some(x=>x.includes('storage backend')));
 write(java+'abyss/copy/Leak.java','class Leak {}');
 write(java+'config/Bad.java','import com.thelongtravail.compat.maid.MaidFluidDefaults;');assert(check(root).some(x=>x.includes('preset')));
 write(java+'config/Bad.java','class Bad {}');
 write('src/main/resources/mixins.the_long_travail.json',JSON.stringify({package:'com.thelongtravail.mixin',mixins:['Missing','Missing'],plugin:'com.thelongtravail.compat.Missing'}));
 const errors=check(root);assert(errors.some(x=>x.includes('duplicate')));assert(errors.some(x=>x.includes('missing Missing')));assert(errors.some(x=>x.includes('missing plugin')));
 console.log('PASS: boundary negative fixtures');
} finally {fs.rmSync(root,{recursive:true,force:true});}

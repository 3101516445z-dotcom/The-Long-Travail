import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
export function check(root) {
    const bridges=new Set(['CompatBootstrap','CompatPresets','CompatTooltips','ftbteams.FtbTeamsCompat','ftbultimine.TremorUltimine','arsnouveau.ArsNouveauAttributeRules','attributeslib.PercentAttributes','caelus.IcarusCaelusCompat','dummmmmmy.DummySoulRules','gravestone.GravestoneStorage','quark.DiaryRunes','sounds.EffectSounds']);
    const platformImport=/^(?:java\.|javax\.|net\.minecraft\.|net\.minecraftforge\.|com\.thelongtravail\.|com\.mojang\.|com\.google\.|com\.electronwill\.|com\.llamalad7\.|cpw\.mods\.|org\.spongepowered\.|org\.objectweb\.|org\.lwjgl\.|org\.joml\.|org\.jetbrains\.|org\.slf4j\.|it\.unimi\.|top\.theillusivec4\.curios\.)/;
    const java=path.join(root,'src/main/java'), base=path.join(java,'com/thelongtravail');
    const walk=d=>fs.readdirSync(d,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(d,e.name)):[path.join(d,e.name)]);
    const errors=[];
    for(const f of walk(base).filter(f=>f.endsWith('.java'))) {
        const rel=path.relative(base,f).replaceAll('\\','/'), text=fs.readFileSync(f,'utf8');
        if(!rel.startsWith('compat/')) {
            for(const match of text.matchAll(/^import\s+(?:static\s+)?([^;]+);/gm))if(!platformImport.test(match[1]))errors.push(rel+': optional external API import '+match[1]);
            for(const match of text.matchAll(/com\.thelongtravail\.compat\.((?:[a-z]\w*\.)*[A-Z]\w*)/g)) {
                if(!bridges.has(match[1]))errors.push(rel+': unlisted compatibility implementation '+match[1]);
                if(!fs.existsSync(path.join(base,'compat',...match[1].split('.'))+'.java'))errors.push(rel+': missing compatibility bridge '+match[1]);
            }
            if(/com\.thelongtravail\.compat\.fluid(?:\.|;)/.test(text))errors.push(rel+': direct fluid implementation dependency');
            if(/com\.thelongtravail\.compat\.maid(?:\.|;)/.test(text))errors.push(rel+': direct maid implementation dependency');
            if(/import\s+(?:static\s+)?(?:com\.github\.tartaricacid|dev\.shadowsoffire|org\.violetmoon|mezz\.jei|snownee\.jade|top\.theillusivec4\.caelus)\./.test(text))errors.push(rel+': optional external API import');
        }
        if(rel.startsWith('abyss/copy/')&&/(?:com\.thelongtravail\.compat\.|sophisticatedbackpacks|refinedstorage|rs_integration|appeng\.|beyonddimensions)/.test(text))errors.push(rel+': storage backend in copy core');
        if(rel.startsWith('config/')&&/import com\.thelongtravail\.compat\.(?!CompatPresets;)/.test(text))errors.push(rel+': preset implementation outside assembly');
    }
    for(const name of ['mixins.the_long_travail.json','mixins.the_long_travail.compat.json']) {
        const c=JSON.parse(fs.readFileSync(path.join(root,'src/main/resources',name),'utf8'));const seen=new Set();
        for(const entry of [...(c.mixins||[]),...(c.client||[]),...(c.server||[])]) {
            if(seen.has(entry))errors.push(name+': duplicate '+entry);seen.add(entry);
            if(!fs.existsSync(path.join(java,...(c.package+'.'+entry).split('.'))+'.java'))errors.push(name+': missing '+entry);
        }
        if(c.plugin&&!fs.existsSync(path.join(java,...c.plugin.split('.'))+'.java'))errors.push(name+': missing plugin '+c.plugin);
    }
    return errors;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
    const root=path.resolve(process.argv[2]||'.');const errors=check(root);
    console.log(errors.length?errors.join('\n'):'PASS: optional API / maid boundary / preset assembly / mixin paths');process.exitCode=errors.length?1:0;
}

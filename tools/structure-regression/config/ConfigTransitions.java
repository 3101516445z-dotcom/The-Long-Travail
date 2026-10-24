package com.thelongtravail.configsmoke;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.config.ConfigFileIO;
import com.thelongtravail.config.PerfectFluidCompatDefaults;
import com.thelongtravail.compat.maid.MaidFluidDefaults;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

final class ConfigTransitions {
    private static int checks;
    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    private static ForgeConfigSpec fixtureSpec() { var b=new ForgeConfigSpec.Builder(); b.define("fixture",true); return b.build(); }
    private static final class LiveFixture extends ModConfig {
        final CommentedConfig data=CommentedConfig.inMemory();
        final boolean fail;
        int saves;
        LiveFixture(boolean fail) {
            super(ModConfig.Type.COMMON, fixtureSpec(),
                    ModList.get().getModContainerById("the_long_travail").orElseThrow(),"fixture-live-" + UUID.randomUUID() + ".toml");
            this.fail=fail;
            data.set("items.perfect_fluid.itemBlacklist",List.of("minecraft:stone"));
        }
        @Override public CommentedConfig getConfigData() { return data; }
        @Override public void save() {
            saves++;
            expect(Boolean.TRUE.equals(data.get("items.perfect_fluid.compatInitialization.maidSoulAdded")),"live edit precedes save");
            if(fail) throw new IllegalStateException("injected save failure");
            // Models a save method which reports/rejects I/O internally and returns normally.
        }
    }
    private static void liveDefaults() throws Exception {
        var field=PerfectFluidCompatDefaults.class.getDeclaredField("loaded");field.setAccessible(true);
        var init=PerfectFluidCompatDefaults.class.getDeclaredMethod("initialize");init.setAccessible(true);
        Object original=field.get(null);
        try {
            expect(ModList.get().isLoaded("touhou_little_maid"),"live-default fixture has actual optional mod");
            var ignored=new LiveFixture(false);field.set(null,ignored);init.invoke(null);
            expect(ignored.saves==1,"default initializer saves once");
            expect(Boolean.TRUE.equals(ignored.data.get("items.perfect_fluid.compatInitialization.maidSoulAdded")),"normally rejected save retains live default");
            init.invoke(null);expect(ignored.saves==1,"marked initialization is idempotent");
            var failed=new LiveFixture(true);field.set(null,failed);
            try {init.invoke(null);throw new AssertionError("save exception swallowed");}
            catch(java.lang.reflect.InvocationTargetException e){expect(e.getCause() instanceof IllegalStateException,"save exception propagates");}
            expect(Boolean.TRUE.equals(failed.data.get("items.perfect_fluid.compatInitialization.maidSoulAdded")),"throwing save does not rollback live edit");
        } finally {field.set(null,original);}
    }
    static void run() throws Exception {
        var builder = new ForgeConfigSpec.Builder();
        var value = builder.defineInRange("value", 1, 0, 10);
        builder.defineList("ordered", List.of("a", "b"), v -> v instanceof String);
        var spec = builder.build();
        var type = Class.forName("com.thelongtravail.config.ManagedConfig");
        var ctor = type.getDeclaredConstructor(ModConfig.Type.class, ForgeConfigSpec.class,
                net.minecraftforge.fml.ModContainer.class, String.class, Path.class);
        ctor.setAccessible(true);
        Path root = Path.of("transition-fixture/the_long_travail");
        var managed = (ModConfig) ctor.newInstance(ModConfig.Type.COMMON, spec,
                ModList.get().getModContainerById("the_long_travail").orElseThrow(), "transition.toml", root);
        managed.getHandler().reader(root.getParent()).apply(managed);
        spec.acceptConfig(managed.getConfigData());
        var update = type.getDeclaredMethod("update", Predicate.class); update.setAccessible(true);
        var reload = type.getDeclaredMethod("reload"); reload.setAccessible(true);
        var watching = type.getDeclaredField("watching"); watching.setAccessible(true); watching.setBoolean(managed, true);
        Path file = managed.getFullPath();
        for (int i=0;i<100;i++) {
            int wanted=i%11;
            expect((boolean)update.invoke(managed,(Predicate<CommentedConfig>) c->{c.set("value",wanted); return true;}),"update accepted");
            expect(value.get()==wanted,"publish follows write");
            expect(((Number)ConfigFileIO.read(file).get("value")).intValue()==wanted,"disk agrees");
            byte[] before=Files.readAllBytes(file);
            expect(!(boolean)update.invoke(managed,(Predicate<CommentedConfig>) c->{c.set("value",10);return false;}),"no-op candidate rejected");
            expect(Arrays.equals(before,Files.readAllBytes(file))&&value.get()==wanted,"no-op changes neither disk nor live");
            expect(managed.getConfigData().get("ordered").equals(List.of("a","b")),"list order retained");
        }
        int previous=value.get();
        byte[] before=Files.readAllBytes(file);
        try { update.invoke(managed,(Predicate<CommentedConfig>)c->{c.set("value",10);throw new IllegalStateException("fixture");});throw new AssertionError("exception swallowed"); }
        catch(java.lang.reflect.InvocationTargetException e){expect(e.getCause() instanceof IllegalStateException,"editor exception propagates");}
        expect(Arrays.equals(before,Files.readAllBytes(file))&&value.get()==previous,"editor exception leaves disk and live unchanged");
        expect(!(boolean)update.invoke(managed,(Predicate<CommentedConfig>) c->{try{Files.writeString(file,"value = 8\nordered = [\"a\",\"b\"]\n");}catch(Exception e){throw new RuntimeException(e);}c.set("value",9);return true;}),"external edit between checks rejects update");
        expect(value.get()==previous&&Files.readString(file).contains("value = 8"),"conflicting file preserved");
        reload.invoke(managed);expect(value.get()==8,"external change subsequently reloads");
        byte[] broken="value = [\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file,broken);reload.invoke(managed);managed.save();
        expect(value.get()==8&&Arrays.equals(broken,Files.readAllBytes(file)),"syntax failure blocks saving old snapshot");
        expect(!(boolean)update.invoke(managed,(Predicate<CommentedConfig>) c->true),"rejected file blocks programmatic update");
        Files.writeString(file,"value = \"bad\"\nordered = [\"b\",\"a\"]\n");reload.invoke(managed);
        expect(value.get()==8&&managed.getConfigData().get("ordered").equals(List.of("b","a")),"bad field fallback with good neighbor");
        expect(Files.readString(file).contains("\"bad\""),"rejected original retained");
        Files.writeString(file,"value = \"7\"\nordered = [\"b\",\"a\"]\nunknown = 42\n");reload.invoke(managed);
        expect(value.get()==7&&ConfigFileIO.read(file).get("value") instanceof Number,"numeric correction accepted");
        expect(!ConfigFileIO.read(file).contains("unknown"),"existing schema correction removes unknown key");
        byte[] normalized=Files.readAllBytes(file);reload.invoke(managed);managed.save();
        expect(Arrays.equals(normalized,Files.readAllBytes(file)),"repeated reload save is byte-idempotent");
        // A directory at the exact isolated file path produces a real I/O failure on every OS.
        Path backup=file.resolveSibling("transition.saved");Files.move(file,backup);
        Files.createDirectory(file);
        expect(!(boolean)update.invoke(managed,(Predicate<CommentedConfig>) c->true),"I/O failure rejects update");
        managed.save();expect(value.get()==7&&Files.isDirectory(file),"save I/O failure keeps live and disk");
        Files.delete(file);Files.move(backup,file);Files.writeString(file,"value = 6\nordered = [\"a\",\"b\"]\n");reload.invoke(managed);
        expect(value.get()==6,"recovery after I/O failure");
        for(int mask=0;mask<64;mask++) {
            int flags=mask;var c=CommentedConfig.inMemory();c.set("items.perfect_fluid.itemBlacklist",new ArrayList<>(List.of("minecraft:stone","minecraft:stone")));
            var rules=MaidFluidDefaults.rules();
            for(int j=0;j<rules.size();j++)c.set("items.perfect_fluid.compatInitialization."+rules.get(j).flag(),(flags&(1<<j))!=0);
            var original=new ArrayList<String>(c.get("items.perfect_fluid.itemBlacklist"));
            for(int j=0;j<rules.size();j++){var rule=rules.get(j);if((flags&(1<<j))==0)for(String item:rule.items())if(!original.contains(item))original.add(item);}
            PerfectFluidCompatDefaults.apply(c, x->true,x->true);
            expect(c.get("items.perfect_fluid.itemBlacklist").equals(original),"defaults preserve order and existing duplicates");
            expect(!PerfectFluidCompatDefaults.apply(c,x->true,x->true),"defaults idempotent");
            var absent=CommentedConfig.inMemory();absent.set("items.perfect_fluid.itemBlacklist",List.of("minecraft:stone"));
            expect(!PerfectFluidCompatDefaults.apply(absent,x->false,x->true),"absent mods do not mark");
            expect(!PerfectFluidCompatDefaults.apply(absent,x->true,x->false),"absent items do not mark");
        }
        if (Boolean.getBoolean("travail.structure.liveDefaults")) liveDefaults();
        Files.writeString(Path.of("config-transitions.txt"),"PASS checks="+checks+"\n100 update cycles; conflicts; exceptions; malformed files; real I/O rejection; recovery; defaults matrix\n");
        System.out.println("CONFIG_TRANSITIONS_PASS checks="+checks);
    }
}

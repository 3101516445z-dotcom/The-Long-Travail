package travail.smoke;

import com.thelongtravail.data.WayguideSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Field;

// 仅供旧测试注入采样目标；正常运行的任务仍由真实刻调度准备。
final class WayguidePreparationSupport {
    static Field field(Class<?> type, String name) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    static Object finish(Object preparation) throws Exception {
        var type = preparation.getClass();
        var step = type.getDeclaredMethod("step"); step.setAccessible(true);
        var finished = field(type, "finished");
        for (int n = 0; !finished.getBoolean(preparation); n++) {
            if (n > 100000) throw new AssertionError("preparation stuck");
            step.invoke(preparation);
        }
        return field(type, "result").get(preparation);
    }
    static Object scanner(ServerLevel level, ItemStack diary, BlockPos pos) throws Exception {
        var type = Class.forName(WayguideSearch.class.getName() + "$Preparation");
        var constructor = type.getDeclaredConstructor(ServerLevel.class, ItemStack.class, BlockPos.class);
        constructor.setAccessible(true); return finish(constructor.newInstance(level, diary, pos));
    }
    static void prepare(Object task) throws Exception {
        var preparation = field(task.getClass(), "preparation");
        if (preparation.get(task) == null) return;
        field(task.getClass(), "scanner").set(task, finish(preparation.get(task)));
        preparation.set(task, null);
    }
}

package com.thelongtravail.farreach;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

public final class FarReachEquipment {
    public static boolean equipped(Player player,boolean gold){
        return player.isAlive()&&!player.isSpectator()&&CuriosApi.getCuriosInventory(player).map(inv->inv.findCurios(gold?ModRegistry.GOLDEN_AGE.get():ModRegistry.ICARUS.get()).stream()
                .anyMatch(r->!r.slotContext().cosmetic()&&r.slotContext().identifier().equals(gold?"charm":"back"))).orElse(false);
    }
    private FarReachEquipment(){}
}

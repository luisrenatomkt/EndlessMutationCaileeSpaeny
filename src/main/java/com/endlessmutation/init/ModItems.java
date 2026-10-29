package com.endlessmutation.init;

import com.endlessmutation.item.ItemSummoner;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class ModItems {
    private ModItems() {}
    public static Item summoner;

    public static void register() {
        summoner = new ItemSummoner();
        GameRegistry.registerItem(summoner, "summoner");
    }
}

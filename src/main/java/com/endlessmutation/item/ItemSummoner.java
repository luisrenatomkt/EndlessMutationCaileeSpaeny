package com.endlessmutation.item;

import com.endlessmutation.entity.EntityHuntress;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/** Amuleto de invocacao: clique direito num bloco para gerar a Predadora Mutante (consome 1 unidade fora do criativo). */
public class ItemSummoner extends Item {
    public ItemSummoner() {
        this.setUnlocalizedName("summoner");
        this.setCreativeTab(CreativeTabs.tabMisc);
        this.setMaxStackSize(16);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        BlockPos at = pos.offset(side);
        EntityHuntress e = new EntityHuntress(world);
        e.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.rotationYaw + 180f, 0f);
        world.spawnEntityInWorld(e);
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return true;
    }
}

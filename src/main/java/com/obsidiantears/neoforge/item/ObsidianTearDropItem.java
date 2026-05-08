package com.obsidiantears.neoforge.item;

import com.obsidiantears.neoforge.screen.WaypointMenuScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ObsidianTearDropItem extends Item {
    public ObsidianTearDropItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.AQUA);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() && player.isLocalPlayer()) {
            Minecraft.getInstance().setScreen(new WaypointMenuScreen());
        }
        return InteractionResult.SUCCESS;
    }
}

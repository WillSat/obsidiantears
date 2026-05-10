package com.obsidiantears.neoforge;

import com.mojang.logging.LogUtils;
import com.obsidiantears.neoforge.event.BlockEventHandler;
import com.obsidiantears.neoforge.event.NetworkEventHandler;
import com.obsidiantears.neoforge.item.ObsidianTearDropItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(ObsidianTears.MODID)
public class ObsidianTears {
    public static final String MODID = "obsidiantears";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredItem<Item> OBSIDIAN_TEAR_DROP = ITEMS.registerItem("obsidian_tear_drop", properties -> new ObsidianTearDropItem(properties.fireResistant()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OBSIDIAN_TEARS_TAB = CREATIVE_MODE_TABS.register("obsidian_tears", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.obsidiantears"))
            .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .icon(() -> OBSIDIAN_TEAR_DROP.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(OBSIDIAN_TEAR_DROP.get()))
            .build());

    public ObsidianTears(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(NetworkEventHandler::registerPayloadHandlers);
        modEventBus.addListener(this::addCreative);
        NeoForge.EVENT_BUS.register(BlockEventHandler.class);
        BlockEventHandler.registerBreakListener(NeoForge.EVENT_BUS);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(OBSIDIAN_TEAR_DROP);
        }
    }
}

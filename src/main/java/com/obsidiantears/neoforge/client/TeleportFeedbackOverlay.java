package com.obsidiantears.neoforge.client;

import com.obsidiantears.neoforge.ObsidianTears;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = ObsidianTears.MODID, value = Dist.CLIENT)
public final class TeleportFeedbackOverlay {
    private static final int TOTAL_TICKS = 100;
    private static final int FADE_IN_TICKS = 12;
    private static final int FADE_OUT_TICKS = 30;

    private static Component biomeLine;
    private static Component labelLine;
    private static Component infoLine;
    private static int biomeColor;
    private static int infoColor;
    private static int displayTicks;
    private static boolean showInfo;

    private TeleportFeedbackOverlay() {}

    public static void show(String biomeKey, String waypointName, String qualifiedSequence, int seqColor, String timeText, String weatherText, boolean showInfoLine) {
        biomeLine = Component.literal(biomeKey.toUpperCase());
        biomeColor = seqColor;
        labelLine = Component.literal(waypointName).withStyle(ChatFormatting.WHITE)
            .append(Component.literal(" " + qualifiedSequence)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(seqColor))));
        if (showInfoLine) {
            infoLine = Component.literal(timeText + "  " + weatherText);
            infoColor = seqColor;
        }
        showInfo = showInfoLine;
        displayTicks = TOTAL_TICKS;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (displayTicks > 0) {
            displayTicks--;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (displayTicks <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();

        int alpha = alpha();
        if (alpha <= 0) return;

        int right = mc.getWindow().getGuiScaledWidth() - 8;
        int y = 30;
        int white = withAlpha(0xFFFFFFFF, alpha);
        int biomeArgb = withAlpha(biomeColor, alpha);

        // Line 1: biome name, large font
        int biomeWidth = mc.font.width(biomeLine);
        graphics.pose().pushMatrix();
        graphics.pose().scaleAround(1.4F, right, y + 7);
        graphics.centeredText(mc.font, biomeLine, right - biomeWidth / 2, y, biomeArgb);
        graphics.pose().popMatrix();
        y += 22;

        // Line 2: label, normal font
        int labelWidth = mc.font.width(labelLine);
        graphics.centeredText(mc.font, labelLine, right - labelWidth / 2, y, white);
        y += 14;

        // Line 3: time & weather, small font, only in overworld
        if (showInfo && infoLine != null) {
            int infoWidth = mc.font.width(infoLine);
            int infoArgb = withAlpha(infoColor, alpha);
            graphics.pose().pushMatrix();
            graphics.pose().scaleAround(0.7F, right, y + 5);
            graphics.centeredText(mc.font, infoLine, right - infoWidth / 2, y, infoArgb);
            graphics.pose().popMatrix();
        }
    }

    private static int alpha() {
        if (displayTicks >= TOTAL_TICKS - FADE_IN_TICKS) {
            return (TOTAL_TICKS - displayTicks) * 255 / FADE_IN_TICKS;
        }
        if (displayTicks <= FADE_OUT_TICKS) {
            return displayTicks * 255 / FADE_OUT_TICKS;
        }
        return 255;
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}

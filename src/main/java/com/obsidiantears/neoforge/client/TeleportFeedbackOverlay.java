package com.obsidiantears.neoforge.client;

import com.obsidiantears.neoforge.ObsidianTears;
import java.util.Random;
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
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ObsidianTears.MODID, value = Dist.CLIENT)
public final class TeleportFeedbackOverlay {
    private static final int TOTAL_TICKS = 100;
    private static final int FADE_IN_TICKS = 12;
    private static final int FADE_OUT_TICKS = 30;
    private static final int DECODE_TICKS = 20;
    private static final int POTION_FADE_IN_TICKS = 15;
    private static final Random RANDOM = new Random();

    private static Component biomeLine;
    private static Component labelLine;
    private static Component infoLine;
    private static int biomeColor;
    private static int infoColor;
    private static int displayTicks;
    private static boolean showInfo;
    private static String targetBiomeName;
    private static int decodeTick;
    private static int potionFadeInTicks;

    private TeleportFeedbackOverlay() {}

    public static void show(String biomeKey, String waypointName, String qualifiedSequence, int seqColor, String timeText, String weatherText, boolean showInfoLine) {
        targetBiomeName = biomeKey.toUpperCase();
        biomeLine = Component.literal(targetBiomeName);
        biomeColor = seqColor;
        if (waypointName != null && !waypointName.isEmpty()) {
            labelLine = Component.literal(waypointName).withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + qualifiedSequence)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(seqColor))));
        } else {
            labelLine = null;
        }
        if (showInfoLine) {
            infoLine = Component.literal(timeText + "  " + weatherText);
            infoColor = seqColor;
        }
        showInfo = showInfoLine;
        displayTicks = TOTAL_TICKS;
        decodeTick = 0;
        potionFadeInTicks = 0;
    }

    public static void showBiomeEntry(String biomeKey, String timeText, String weatherText, boolean showInfoLine, int color) {
        if (displayTicks > 0) return;
        targetBiomeName = biomeKey.toUpperCase();
        biomeLine = Component.literal(targetBiomeName);
        biomeColor = color;
        labelLine = null;
        if (showInfoLine) {
            infoLine = Component.literal(timeText + "  " + weatherText);
            infoColor = color;
        } else {
            infoLine = null;
        }
        showInfo = showInfoLine;
        displayTicks = TOTAL_TICKS;
        decodeTick = 0;
        potionFadeInTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (displayTicks > 0) {
            displayTicks--;
            if (decodeTick < DECODE_TICKS) {
                decodeTick++;
            }
            if (displayTicks == 0) {
                potionFadeInTicks = POTION_FADE_IN_TICKS;
            }
        } else if (potionFadeInTicks > 0) {
            potionFadeInTicks--;
        }
    }

    @SubscribeEvent
    public static void onRenderEffectsLayerPre(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.EFFECTS) && (displayTicks > 0 || potionFadeInTicks > 0)) {
            event.setCanceled(true);
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

        // Line 1: biome name with decode animation
        if (targetBiomeName != null) {
            String name = targetBiomeName;
            int nameLen = name.length();
            int decodedCount = decodeTick >= DECODE_TICKS ? nameLen : (decodeTick * nameLen) / DECODE_TICKS;

            int biomeWidth = mc.font.width(name);
            int charX = right - biomeWidth;

            graphics.pose().pushMatrix();
            graphics.pose().scaleAround(1.4F, right, y + 7);

            for (int i = 0; i < nameLen; i++) {
                String charStr;
                int color;
                if (i < decodedCount) {
                    charStr = String.valueOf(name.charAt(i));
                    color = biomeArgb;
                } else {
                    charStr = String.valueOf((char) ('A' + RANDOM.nextInt(26)));
                    color = white;
                }
                graphics.text(mc.font, charStr, charX, y, color);
                charX += mc.font.width(charStr);
            }
            graphics.pose().popMatrix();
        }
        // Line 2: label, normal font — only after mod teleport
        if (labelLine != null) {
            y += 14;
            int labelWidth = mc.font.width(labelLine);
            graphics.centeredText(mc.font, labelLine, right - labelWidth / 2, y, white);
            y += 9;
        } else {
            y += 14;
        }

        // Line 3: time & weather, only in overworld
        if (showInfo && infoLine != null) {
            int infoWidth = mc.font.width(infoLine);
            int infoArgb = withAlpha(0xFFFFFFFF, alpha);
            graphics.pose().pushMatrix();
            graphics.pose().scaleAround(0.85F, right, y + 5);
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

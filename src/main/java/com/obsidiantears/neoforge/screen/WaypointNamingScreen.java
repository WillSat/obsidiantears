package com.obsidiantears.neoforge.screen;

import com.obsidiantears.neoforge.network.PacketHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import com.obsidiantears.neoforge.waypoint.WaypointData;
import net.minecraft.resources.Identifier;

public class WaypointNamingScreen extends Screen {
    private static final int TEXTBOX_WIDTH = 260;
    private static final int TEXTBOX_HEIGHT = 20;

    private final Identifier dimension;
    private final BlockPos waypointPos;
    private final int sequence;
    private EditBox nameInput;
    private Button confirmButton;

    public WaypointNamingScreen(Identifier dimension, BlockPos pos, int sequence) {
        super(Component.translatable("screen.obsidiantears.naming.title"));
        this.dimension = dimension;
        this.waypointPos = pos;
        this.sequence = sequence;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int inputY = centerY + 8;
        int buttonY = centerY + 42;

        addCenteredText(dimensionLabel().withStyle(ChatFormatting.AQUA), centerY - 42);
        MutableComponent seqText = Component.literal(String.valueOf(sequence))
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(WaypointData.sequenceColor(sequence))));
        addCenteredText(Component.translatable("screen.obsidiantears.naming.subtitle", seqText).withStyle(ChatFormatting.GRAY), centerY - 28);
        addCenteredText(Component.translatable("screen.obsidiantears.naming.location", waypointPos.getX(), waypointPos.getY(), waypointPos.getZ()).withStyle(ChatFormatting.DARK_GRAY), centerY - 14);

        nameInput = new EditBox(this.font, centerX - TEXTBOX_WIDTH / 2, inputY, TEXTBOX_WIDTH, TEXTBOX_HEIGHT, Component.empty());
        nameInput.setMaxLength(50);
        nameInput.setHint(Component.translatable("screen.obsidiantears.naming.hint"));
        nameInput.setResponder(value -> updateConfirmButton());
        this.addRenderableWidget(nameInput);
        this.setInitialFocus(nameInput);

        confirmButton = Button.builder(Component.translatable("gui.obsidiantears.confirm"), button -> confirmName())
            .bounds(centerX - 90, buttonY, 80, 20)
            .build();
        confirmButton.active = false;
        this.addRenderableWidget(confirmButton);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.obsidiantears.cancel"), button -> this.onClose())
            .bounds(centerX + 10, buttonY, 80, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawScaledCenteredText(graphics, this.title.copy().withStyle(ChatFormatting.BOLD), this.height / 2 - 56, 1.35F, 0xFFE6B3);
    }

    private MutableComponent dimensionLabel() {
        MutableComponent name = switch (dimension.toString()) {
            case "minecraft:overworld" -> Component.translatable("dimension.obsidiantears.overworld");
            case "minecraft:the_nether" -> Component.translatable("dimension.obsidiantears.nether");
            case "minecraft:the_end" -> Component.translatable("dimension.obsidiantears.end");
            default -> Component.literal(dimension.toString());
        };
        return Component.translatable("screen.obsidiantears.naming.dimension", name);
    }

    private void addCenteredText(Component text, int y) {
        int maxWidth = Math.min(TEXTBOX_WIDTH + 80, this.width - 32);
        int textWidth = this.font.width(text);
        if (textWidth > maxWidth) {
            text = Component.literal(trimToWidth(text.getString(), maxWidth)).withStyle(text.getStyle());
            textWidth = this.font.width(text);
        }
        this.addRenderableOnly(new StringWidget((this.width - textWidth) / 2, y, textWidth, 9, text, this.font));
    }

    private void drawScaledCenteredText(GuiGraphicsExtractor graphics, Component text, int y, float scale, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().scaleAround(scale, this.width / 2.0F, y);
        graphics.centeredText(this.font, text, this.width / 2, y, color);
        graphics.pose().popMatrix();
    }

    private String trimToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return end <= 0 ? suffix : text.substring(0, end) + suffix;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            confirmName();
            return true;
        }
        return super.keyPressed(event);
    }

    private void confirmName() {
        String name = nameInput.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        PacketHelper.nameWaypoint(dimension, waypointPos, name);
        this.onClose();
    }

    private void updateConfirmButton() {
        if (confirmButton != null) {
            confirmButton.active = !nameInput.getValue().trim().isEmpty();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}

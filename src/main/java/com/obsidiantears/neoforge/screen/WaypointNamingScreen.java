package com.obsidiantears.neoforge.screen;

import com.obsidiantears.neoforge.network.PacketHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class WaypointNamingScreen extends Screen {
    private static final int TEXTBOX_WIDTH = 220;
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

        nameInput = new EditBox(this.font, centerX - TEXTBOX_WIDTH / 2, centerY - 10, TEXTBOX_WIDTH, TEXTBOX_HEIGHT, Component.empty());
        nameInput.setMaxLength(50);
        nameInput.setHint(Component.translatable("screen.obsidiantears.naming.hint"));
        nameInput.setResponder(value -> updateConfirmButton());
        this.addRenderableWidget(nameInput);
        this.setInitialFocus(nameInput);

        confirmButton = Button.builder(Component.translatable("gui.obsidiantears.confirm"), button -> confirmName())
            .bounds(centerX - 70, centerY + 24, 60, 20)
            .build();
        confirmButton.active = false;
        this.addRenderableWidget(confirmButton);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.obsidiantears.cancel"), button -> this.onClose())
            .bounds(centerX + 10, centerY + 24, 60, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 48, 0xFFFFFF);
        graphics.centeredText(this.font, Component.translatable("screen.obsidiantears.naming.subtitle", sequence), this.width / 2, this.height / 2 - 34, 0xAAAAAA);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
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

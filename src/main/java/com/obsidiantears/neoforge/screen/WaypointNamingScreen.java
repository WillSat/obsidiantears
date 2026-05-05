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
        super(Component.literal("命名传送碑"));
        this.dimension = dimension;
        this.waypointPos = pos;
        this.sequence = sequence;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        nameInput = new EditBox(this.font, centerX - TEXTBOX_WIDTH / 2, centerY - 10, TEXTBOX_WIDTH, TEXTBOX_HEIGHT, Component.literal(""));
        nameInput.setMaxLength(50);
        nameInput.setHint(Component.literal("输入传送碑名称"));
        nameInput.setResponder(value -> updateConfirmButton());
        this.addRenderableWidget(nameInput);
        this.setInitialFocus(nameInput);

        confirmButton = Button.builder(Component.literal("确定"), button -> confirmName())
            .bounds(centerX - 70, centerY + 24, 60, 20)
            .build();
        confirmButton.active = false;
        this.addRenderableWidget(confirmButton);
        this.addRenderableWidget(Button.builder(Component.literal("取消"), button -> this.onClose())
            .bounds(centerX + 10, centerY + 24, 60, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.centeredText(this.font, "请为传送碑命名", this.width / 2, this.height / 2 - 40, 0xFFFFFF);
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

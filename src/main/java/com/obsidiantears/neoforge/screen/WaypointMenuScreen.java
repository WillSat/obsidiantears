package com.obsidiantears.neoforge.screen;

import com.obsidiantears.neoforge.network.PacketHelper;
import com.obsidiantears.neoforge.waypoint.WaypointData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class WaypointMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 340;
    private static final int BUTTON_HEIGHT = 20;
    private static final int VISIBLE_BUTTONS = 8;

    private List<WaypointData> waypoints = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean loaded = false;

    public WaypointMenuScreen() {
        super(Component.translatable("screen.obsidiantears.teleport.title"));
    }

    @Override
    protected void init() {
        PacketHelper.requestWaypoints();
        rebuildWaypointButtons();
    }

    private void rebuildWaypointButtons() {
        clearWidgets();

        int startX = (this.width - BUTTON_WIDTH) / 2;
        int startY = this.height / 4 + 28;
        int end = Math.min(waypoints.size(), scrollOffset + VISIBLE_BUTTONS);

        for (int i = scrollOffset; i < end; i++) {
            WaypointData waypoint = waypoints.get(i);
            int y = startY + (i - scrollOffset) * (BUTTON_HEIGHT + 6);
            Component text = Component.translatable(
                "screen.obsidiantears.teleport.entry",
                waypoint.getDisplayName(),
                waypoint.getSequence(),
                dimensionName(waypoint),
                waypoint.getPos().getX(),
                waypoint.getPos().getY(),
                waypoint.getPos().getZ()
            );
            this.addRenderableWidget(Button.builder(text, button -> teleportToWaypoint(waypoint))
                .bounds(startX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.obsidiantears.close"), button -> this.onClose())
            .bounds(startX, this.height - 40, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        if (!loaded) {
            drawCenteredLines(graphics, this.height / 2 - 8, 0xAAAAAA, Component.translatable("screen.obsidiantears.teleport.loading"));
        } else if (waypoints.isEmpty()) {
            drawEmptyState(graphics);
        } else if (waypoints.size() > VISIBLE_BUTTONS) {
            Component page = Component.translatable("screen.obsidiantears.teleport.page", Math.min(scrollOffset + VISIBLE_BUTTONS, waypoints.size()), waypoints.size());
            graphics.centeredText(this.font, page, this.width / 2, this.height - 62, 0xAAAAAA);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawEmptyState(GuiGraphicsExtractor graphics) {
        drawCenteredLines(
            graphics,
            this.height / 2 - 38,
            0xAAAAAA,
            Component.translatable("screen.obsidiantears.teleport.empty.title"),
            Component.translatable("screen.obsidiantears.teleport.empty.build"),
            Component.translatable("screen.obsidiantears.teleport.empty.name"),
            Component.translatable("screen.obsidiantears.teleport.empty.recipe")
        );
    }

    private void drawCenteredLines(GuiGraphicsExtractor graphics, int startY, int color, Component... lines) {
        for (int i = 0; i < lines.length; i++) {
            graphics.centeredText(this.font, lines[i], this.width / 2, startY + i * 14, color);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, waypoints.size() - VISIBLE_BUTTONS);
        int oldOffset = scrollOffset;
        scrollOffset = scrollY > 0 ? Math.max(0, scrollOffset - 1) : Math.min(maxScroll, scrollOffset + 1);
        if (oldOffset != scrollOffset) {
            rebuildWaypointButtons();
        }
        return true;
    }

    private void teleportToWaypoint(WaypointData waypoint) {
        PacketHelper.requestTeleport(waypoint.getDimension(), waypoint.getPos());
        this.onClose();
    }

    public void setWaypoints(List<WaypointData> waypoints) {
        this.loaded = true;
        this.waypoints = new ArrayList<>(waypoints);
        this.scrollOffset = Math.min(this.scrollOffset, Math.max(0, this.waypoints.size() - VISIBLE_BUTTONS));
        rebuildWaypointButtons();
    }

    private static Component dimensionName(WaypointData waypoint) {
        return switch (waypoint.getDimension().toString()) {
            case "minecraft:overworld" -> Component.translatable("dimension.obsidiantears.overworld");
            case "minecraft:the_nether" -> Component.translatable("dimension.obsidiantears.nether");
            case "minecraft:the_end" -> Component.translatable("dimension.obsidiantears.end");
            default -> Component.literal(waypoint.getDimension().toString());
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

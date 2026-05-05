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
    private static final int BUTTON_WIDTH = 320;
    private static final int BUTTON_HEIGHT = 20;
    private static final int VISIBLE_BUTTONS = 8;

    private List<WaypointData> waypoints = new ArrayList<>();
    private int scrollOffset = 0;

    public WaypointMenuScreen() {
        super(Component.literal("传送菜单"));
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
            String text = waypoint.getDisplayName() + " #" + waypoint.getSequence()
                + " [" + dimensionName(waypoint) + " "
                + waypoint.getPos().getX() + ", " + waypoint.getPos().getY() + ", " + waypoint.getPos().getZ() + "]";
            this.addRenderableWidget(Button.builder(Component.literal(text), button -> teleportToWaypoint(waypoint))
                .bounds(startX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("关闭"), button -> this.onClose())
            .bounds(startX, this.height - 40, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.centeredText(this.font, "黑曜石泪滴传送", this.width / 2, 20, 0xFFFFFF);
        if (waypoints.isEmpty()) {
            graphics.centeredText(this.font, "暂无传送碑", this.width / 2, this.height / 2, 0xAAAAAA);
        } else if (waypoints.size() > VISIBLE_BUTTONS) {
            String page = Math.min(scrollOffset + VISIBLE_BUTTONS, waypoints.size()) + "/" + waypoints.size();
            graphics.centeredText(this.font, page, this.width / 2, this.height - 62, 0xAAAAAA);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
        this.waypoints = new ArrayList<>(waypoints);
        this.scrollOffset = Math.min(this.scrollOffset, Math.max(0, this.waypoints.size() - VISIBLE_BUTTONS));
        rebuildWaypointButtons();
    }

    private static String dimensionName(WaypointData waypoint) {
        return switch (waypoint.getDimension().toString()) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            default -> waypoint.getDimension().toString();
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

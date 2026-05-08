package com.obsidiantears.neoforge.screen;

import com.obsidiantears.neoforge.network.PacketHelper;
import com.obsidiantears.neoforge.waypoint.WaypointData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

public class WaypointMenuScreen extends Screen {
    private static final int MIN_BUTTON_WIDTH = 260;
    private static final int MAX_BUTTON_WIDTH = 380;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_GAP = 5;
    private static final int MAX_VISIBLE_ROWS = 12;
    private static final int COORD_BUTTON_WIDTH = 90;
    private static final int MOVE_BUTTON_WIDTH = 20;
    private static final List<Identifier> VANILLA_DIMENSION_ORDER = List.of(
        Identifier.fromNamespaceAndPath("minecraft", "overworld"),
        Identifier.fromNamespaceAndPath("minecraft", "the_nether"),
        Identifier.fromNamespaceAndPath("minecraft", "the_end")
    );

    private List<WaypointData> allWaypoints = new ArrayList<>();
    private List<DisplayRow> rows = new ArrayList<>();
    private List<Identifier> dimensionTabs = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean loaded = false;
    private int currentTab = 0;

    public WaypointMenuScreen() {
        super(Component.translatable("screen.obsidiantears.teleport.title"));
    }

    @Override
    protected void init() {
        if (!loaded) {
            PacketHelper.requestWaypoints();
        }
        rebuildWaypointButtons();
    }

    private void selectTab(int tab) {
        int tabCount = dimensionTabs.size() + 1;
        if (tab == currentTab || tab >= tabCount) return;
        currentTab = tab;
        scrollOffset = 0;
        rebuildWaypointButtons();
    }

    private List<Identifier> buildDimensionTabs() {
        Set<Identifier> activeDims = allWaypoints.stream()
            .map(WaypointData::getDimension)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Identifier> result = new ArrayList<>();
        for (Identifier vanilla : VANILLA_DIMENSION_ORDER) {
            if (activeDims.remove(vanilla)) {
                result.add(vanilla);
            }
        }
        List<Identifier> modDims = new ArrayList<>(activeDims);
        modDims.sort(Comparator.comparing(Identifier::toString));
        result.addAll(modDims);
        return result;
    }

    private List<DisplayRow> buildTabRows() {
        if (currentTab == 0) {
            return buildAllRows(allWaypoints);
        }
        Identifier targetDim = dimensionTabs.get(currentTab - 1);
        List<WaypointData> filtered = allWaypoints.stream()
            .filter(wp -> wp.getDimension().equals(targetDim))
            .sorted(Comparator.comparingInt(WaypointData::getSequence))
            .toList();
        return filtered.stream().map(DisplayRow::new).toList();
    }

    private List<DisplayRow> buildAllRows(List<WaypointData> waypoints) {
        Map<Identifier, List<WaypointData>> grouped = new LinkedHashMap<>();
        for (Identifier dim : dimensionTabs) {
            grouped.put(dim, new ArrayList<>());
        }
        for (WaypointData wp : waypoints) {
            grouped.computeIfAbsent(wp.getDimension(), k -> new ArrayList<>()).add(wp);
        }
        List<DisplayRow> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            List<WaypointData> list = entry.getValue();
            if (list.isEmpty()) continue;
            list.sort(Comparator.comparingInt(WaypointData::getSequence));
            for (WaypointData wp : list) {
                result.add(new DisplayRow(wp));
            }
        }
        return result;
    }

    private void rebuildWaypointButtons() {
        clearWidgets();

        rows = buildTabRows();
        dimensionTabs = buildDimensionTabs();
        int tabCount = dimensionTabs.size() + 1;
        if (currentTab >= tabCount) {
            currentTab = 0;
            rows = buildTabRows();
        }

        int buttonWidth = listButtonWidth();
        int visibleRows = visibleRowCount();
        int startX = (this.width - buttonWidth) / 2;
        int listY = listStartY();
        int maxScroll = maxScroll();
        scrollOffset = clamp(scrollOffset, 0, maxScroll);
        int end = Math.min(rows.size(), scrollOffset + visibleRows);

        // --- tab bar ---
        int tabBarY = 28;
        int tabGap = 2;
        int tabWidth = tabCount > 0 ? (buttonWidth - (tabCount - 1) * tabGap) / tabCount : buttonWidth;
        for (int t = 0; t < tabCount; t++) {
            int tx = startX + t * (tabWidth + tabGap);
            int tabIndex = t;
            this.addRenderableWidget(Button.builder(tabName(t), btn -> selectTab(tabIndex))
                .bounds(tx, tabBarY, tabWidth, BUTTON_HEIGHT)
                .build());
        }

        // --- status text ---
        if (!loaded) {
            addCenteredText(Component.translatable("screen.obsidiantears.teleport.loading").withStyle(ChatFormatting.GRAY), this.height / 2 - 8);
        } else if (rows.isEmpty()) {
            addEmptyStateText();
        } else if (rows.size() > visibleRows) {
            Component page = Component.translatable("screen.obsidiantears.teleport.page",
                scrollOffset + 1, Math.min(scrollOffset + visibleRows, rows.size()), rows.size())
                .withStyle(ChatFormatting.GRAY);
            addCenteredText(page, this.height - 62);
        }

        // --- scrollable list ---
        if (loaded) {
            Map<Identifier, Integer> maxSeqByDim = new HashMap<>();
            for (WaypointData wp : allWaypoints) {
                maxSeqByDim.merge(wp.getDimension(), wp.getSequence(), Math::max);
            }

            for (int i = scrollOffset; i < end; i++) {
                DisplayRow row = rows.get(i);
                int y = listY + (i - scrollOffset) * (BUTTON_HEIGHT + BUTTON_GAP);
                if (row.isHeader()) {
                    addSectionHeader(row.headerText(), y, buttonWidth);
                } else {
                    WaypointData wp = row.waypoint();
                    boolean dimensionView = currentTab != 0;
                    int extrasWidth = COORD_BUTTON_WIDTH + 3;
                    if (dimensionView) {
                        extrasWidth += (MOVE_BUTTON_WIDTH + 3) * 2;
                    }
                    int teleWidth = buttonWidth - extrasWidth;
                    this.addRenderableWidget(Button.builder(teleportText(wp), btn -> teleportToWaypoint(wp))
                        .bounds(startX, y, teleWidth, BUTTON_HEIGHT)
                        .build());
                    int offset = startX + teleWidth + 3;
                    if (dimensionView) {
                        int maxSeq = maxSeqByDim.getOrDefault(wp.getDimension(), 1);
                        Button upBtn = Button.builder(Component.literal("▲"), btn -> moveUp(wp))
                            .bounds(offset, y, MOVE_BUTTON_WIDTH, BUTTON_HEIGHT)
                            .build();
                        upBtn.active = wp.getSequence() > 1;
                        this.addRenderableWidget(upBtn);
                        offset += MOVE_BUTTON_WIDTH + 3;
                        Button downBtn = Button.builder(Component.literal("▼"), btn -> moveDown(wp))
                            .bounds(offset, y, MOVE_BUTTON_WIDTH, BUTTON_HEIGHT)
                            .build();
                        downBtn.active = wp.getSequence() < maxSeq;
                        this.addRenderableWidget(downBtn);
                        offset += MOVE_BUTTON_WIDTH + 3;
                    }
                    this.addRenderableWidget(Button.builder(coordText(wp), btn -> copyCoords(wp))
                        .bounds(offset, y, COORD_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build());
                }
            }
        }

        // --- scroll buttons ---
        if (loaded && rows.size() > visibleRows && startX + buttonWidth + 32 < this.width) {
            Button prev = Button.builder(Component.translatable("screen.obsidiantears.teleport.previous"), btn -> changeScroll(-visibleRows))
                .bounds(startX + buttonWidth + 6, listY, 24, BUTTON_HEIGHT)
                .build();
            prev.active = scrollOffset > 0;
            this.addRenderableWidget(prev);

            Button next = Button.builder(Component.translatable("screen.obsidiantears.teleport.next"), btn -> changeScroll(visibleRows))
                .bounds(startX + buttonWidth + 6, listY + BUTTON_HEIGHT + BUTTON_GAP, 24, BUTTON_HEIGHT)
                .build();
            next.active = scrollOffset < maxScroll;
            this.addRenderableWidget(next);
        }

        // --- bottom bar ---
        int bottomY = this.height - 40;
        if (buttonWidth >= 280) {
            int halfWidth = (buttonWidth - BUTTON_GAP) / 2;
            Button refresh = Button.builder(Component.translatable("screen.obsidiantears.teleport.refresh"), btn -> refreshWaypoints())
                .bounds(startX, bottomY, halfWidth, BUTTON_HEIGHT)
                .build();
            refresh.active = loaded;
            this.addRenderableWidget(refresh);
            this.addRenderableWidget(Button.builder(Component.translatable("gui.obsidiantears.close"), btn -> this.onClose())
                .bounds(startX + halfWidth + BUTTON_GAP, bottomY, halfWidth, BUTTON_HEIGHT)
                .build());
        } else {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.obsidiantears.close"), btn -> this.onClose())
                .bounds(startX, bottomY, buttonWidth, BUTTON_HEIGHT)
                .build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawScaledCenteredText(graphics, this.title.copy().withStyle(ChatFormatting.BOLD), 14, 1.35F, 0xFFE6B3);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_R) {
            refreshWaypoints();
            return true;
        }
        if (!loaded || rows.isEmpty()) {
            return super.keyPressed(event);
        }

        int visibleRows = visibleRowCount();
        switch (event.key()) {
            case GLFW.GLFW_KEY_UP -> { changeScroll(-1); return true; }
            case GLFW.GLFW_KEY_DOWN -> { changeScroll(1); return true; }
            case GLFW.GLFW_KEY_PAGE_UP -> { changeScroll(-visibleRows); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { changeScroll(visibleRows); return true; }
            case GLFW.GLFW_KEY_HOME -> { setScrollOffset(0); return true; }
            case GLFW.GLFW_KEY_END -> { setScrollOffset(maxScroll()); return true; }
            case GLFW.GLFW_KEY_TAB -> { cycleTab(); return true; }
            default -> {}
        }
        return super.keyPressed(event);
    }

    private void cycleTab() {
        int tabCount = dimensionTabs.size() + 1;
        if (tabCount > 1) {
            selectTab((currentTab + 1) % tabCount);
        }
    }

    private void addEmptyStateText() {
        addCenteredTextLines(
            this.height / 2 - 24,
            Component.translatable("screen.obsidiantears.teleport.empty.title").withStyle(ChatFormatting.GRAY),
            Component.translatable("screen.obsidiantears.teleport.empty.build").withStyle(ChatFormatting.DARK_GRAY),
            Component.translatable("screen.obsidiantears.teleport.empty.name").withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    private void addCenteredTextLines(int startY, Component... lines) {
        for (int i = 0; i < lines.length; i++) {
            addCenteredText(lines[i], startY + i * 14);
        }
    }

    private void addCenteredText(Component text, int y) {
        int maxWidth = Math.min(listButtonWidth() + 60, this.width - 32);
        int textWidth = this.font.width(text);
        if (textWidth > maxWidth) {
            text = Component.literal(trimToWidth(text.getString(), maxWidth)).withStyle(text.getStyle());
            textWidth = this.font.width(text);
        }
        this.addRenderableOnly(new StringWidget((this.width - textWidth) / 2, y, textWidth, 9, text, this.font));
    }

    private void addSectionHeader(Component text, int y, int buttonWidth) {
        MutableComponent styled = text.copy().withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GOLD);
        int maxWidth = Math.min(buttonWidth, this.width - 32);
        int textWidth = this.font.width(styled);
        if (textWidth > maxWidth) {
            styled = Component.literal(trimToWidth(styled.getString(), maxWidth)).withStyle(styled.getStyle());
            textWidth = this.font.width(styled);
        }
        this.addRenderableOnly(new StringWidget((this.width - textWidth) / 2, y, textWidth, 9, styled, this.font));
    }

    private void drawScaledCenteredText(GuiGraphicsExtractor graphics, Component text, int y, float scale, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().scaleAround(scale, this.width / 2.0F, y);
        graphics.centeredText(this.font, text, this.width / 2, y, color);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!loaded || rows.size() <= visibleRowCount()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        changeScroll(scrollY > 0 ? -1 : 1);
        return true;
    }

    private void changeScroll(int amount) {
        setScrollOffset(scrollOffset + amount);
    }

    private void setScrollOffset(int value) {
        int oldOffset = scrollOffset;
        scrollOffset = clamp(value, 0, maxScroll());
        if (oldOffset != scrollOffset) {
            rebuildWaypointButtons();
        }
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRowCount());
    }

    private int visibleRowCount() {
        int availableHeight = Math.max(BUTTON_HEIGHT, this.height - listStartY() - 80);
        int count = availableHeight / (BUTTON_HEIGHT + BUTTON_GAP);
        return clamp(count, 3, MAX_VISIBLE_ROWS);
    }

    private int listButtonWidth() {
        return clamp(this.width - 64, MIN_BUTTON_WIDTH, MAX_BUTTON_WIDTH);
    }

    private int listStartY() {
        return 54;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void teleportToWaypoint(WaypointData waypoint) {
        PacketHelper.requestTeleport(waypoint.getDimension(), waypoint.getPos());
        this.onClose();
    }

    private void moveUp(WaypointData waypoint) {
        PacketHelper.moveWaypointUp(waypoint.getDimension(), waypoint.getPos());
    }

    private void moveDown(WaypointData waypoint) {
        PacketHelper.moveWaypointDown(waypoint.getDimension(), waypoint.getPos());
    }

    public void setWaypoints(List<WaypointData> waypoints) {
        this.loaded = true;
        this.allWaypoints = new ArrayList<>(waypoints);
        this.scrollOffset = Math.min(this.scrollOffset, maxScroll());
        rebuildWaypointButtons();
    }

    private void refreshWaypoints() {
        loaded = false;
        scrollOffset = 0;
        PacketHelper.requestWaypoints();
        rebuildWaypointButtons();
    }

    private Component teleportText(WaypointData waypoint) {
        MutableComponent seqText = Component.literal(waypoint.getQualifiedSequence())
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(WaypointData.sequenceColor(waypoint.getDimension(), waypoint.getSequence()))));
        return Component.translatable(
            "screen.obsidiantears.teleport.entry",
            Component.literal(trimToWidth(waypoint.getDisplayName(), 80)),
            seqText
        );
    }

    private Component coordText(WaypointData waypoint) {
        String text = "X:" + waypoint.getPos().getX() + " Y:" + waypoint.getPos().getY() + " Z:" + waypoint.getPos().getZ();
        return Component.literal(trimToWidth(text, 100)).withStyle(ChatFormatting.GRAY);
    }

    private void copyCoords(WaypointData waypoint) {
        String coords = "X:" + waypoint.getPos().getX() + " Y:" + waypoint.getPos().getY() + " Z:" + waypoint.getPos().getZ();
        if (this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(coords);
            if (this.minecraft.player != null) {
                this.minecraft.gui.setOverlayMessage(
                    Component.translatable("screen.obsidiantears.teleport.copied").withStyle(ChatFormatting.GREEN), false);
            }
        }
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

    private MutableComponent sectionName(Identifier dimensionId) {
        MutableComponent name = switch (dimensionId.toString()) {
            case "minecraft:overworld" -> Component.translatable("screen.obsidiantears.teleport.section.overworld");
            case "minecraft:the_nether" -> Component.translatable("screen.obsidiantears.teleport.section.nether");
            case "minecraft:the_end" -> Component.translatable("screen.obsidiantears.teleport.section.end");
            default -> Component.literal(dimensionId.getPath());
        };
        return Component.literal("—— ").append(name).append(" ——");
    }

    private MutableComponent tabName(int tab) {
        if (tab == 0) {
            return Component.translatable("screen.obsidiantears.teleport.tab.all");
        }
        if (tab - 1 < dimensionTabs.size()) {
            Identifier dim = dimensionTabs.get(tab - 1);
            return switch (dim.toString()) {
                case "minecraft:overworld" -> Component.translatable("screen.obsidiantears.teleport.tab.overworld");
                case "minecraft:the_nether" -> Component.translatable("screen.obsidiantears.teleport.tab.nether");
                case "minecraft:the_end" -> Component.translatable("screen.obsidiantears.teleport.tab.end");
                default -> Component.literal(WaypointData.dimensionPrefix(dim));
            };
        }
        return Component.literal("?");
    }

    private static class DisplayRow {
        private final Component headerText;
        private final WaypointData waypoint;

        DisplayRow(Component headerText) {
            this.headerText = headerText;
            this.waypoint = null;
        }

        DisplayRow(WaypointData waypoint) {
            this.headerText = null;
            this.waypoint = waypoint;
        }

        boolean isHeader() { return headerText != null; }
        boolean isWaypoint() { return waypoint != null; }
        Component headerText() { return headerText; }
        WaypointData waypoint() { return waypoint; }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

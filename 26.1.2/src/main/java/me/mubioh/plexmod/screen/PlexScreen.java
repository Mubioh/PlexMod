package me.mubioh.plexmod.screen;

import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.feature.PlexFeature;
import me.mubioh.plexmod.core.feature.PlexRegistry;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PlexScreen extends Screen {

    private static final int BUTTON_WIDTH       = 150;
    private static final int BUTTON_SPACING     = 4;
    private static final int FULL_WIDTH         = (BUTTON_WIDTH * 2) + BUTTON_SPACING;
    private static final int MAX_MESSAGE_LENGTH = 100;

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    private EditBox autoGgBox;
    private EditBox autoGlBox;

    public PlexScreen(Screen parent) {
        super(Component.literal("PlexMod Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);

        List<PlexFeature> others     = new ArrayList<>();
        PlexFeature autoGgFeature    = null;
        PlexFeature autoGlFeature    = null;
        PlexFeature autoTauntFeature    = null;
        PlexFeature betterLobbiesFeature = null;

        for (PlexFeature feature : PlexRegistry.getAll()) {
            if (!feature.isToggleable()) continue;
            switch (feature.getId()) {
                case "autogg"    -> autoGgFeature    = feature;
                case "autogl"    -> autoGlFeature    = feature;
                case "autotaunt"       -> autoTauntFeature      = feature;
                case "better_lobbies"  -> betterLobbiesFeature  = feature;
                default          -> others.add(feature);
            }
        }

        LinearLayout content = this.layout.addToContents(LinearLayout.vertical().spacing(BUTTON_SPACING));

        for (int i = 0; i < others.size(); i += 2) {
            GridLayout row = new GridLayout().columnSpacing(BUTTON_SPACING);
            GridLayout.RowHelper helper = row.createRowHelper(2);
            helper.addChild(buildToggleButton(others.get(i), BUTTON_WIDTH));
            if (i + 1 < others.size()) {
                helper.addChild(buildToggleButton(others.get(i + 1), BUTTON_WIDTH));
            } else {
                helper.addChild(Button.builder(Component.empty(), btn -> {}).width(BUTTON_WIDTH).build());
            }
            content.addChild(row);
        }

        if (autoGlFeature != null) {
            autoGlBox = buildMessageBox("AutoGL message (default: GL HF!)",
                    "Message sent at the start of a game.",
                    PlexConfig.getInstance().getAutoGlMessage());

            GridLayout glRow = new GridLayout().columnSpacing(BUTTON_SPACING);
            GridLayout.RowHelper glHelper = glRow.createRowHelper(2);
            glHelper.addChild(buildToggleButton(autoGlFeature, BUTTON_WIDTH));
            glHelper.addChild(autoGlBox);
            content.addChild(glRow);
        }

        if (autoGgFeature != null) {
            autoGgBox = buildMessageBox("AutoGG message (default: GG!)",
                    "Message sent at the end of a game.",
                    PlexConfig.getInstance().getAutoGgMessage());

            GridLayout ggRow = new GridLayout().columnSpacing(BUTTON_SPACING);
            GridLayout.RowHelper ggHelper = ggRow.createRowHelper(2);
            ggHelper.addChild(buildToggleButton(autoGgFeature, BUTTON_WIDTH));
            ggHelper.addChild(autoGgBox);
            content.addChild(ggRow);
        }

        if (autoTauntFeature != null || betterLobbiesFeature != null) {
            GridLayout tauntRow = new GridLayout().columnSpacing(BUTTON_SPACING);
            GridLayout.RowHelper tauntHelper = tauntRow.createRowHelper(2);

            if (autoTauntFeature != null) {
                tauntHelper.addChild(buildToggleButton(autoTauntFeature, BUTTON_WIDTH));
            } else {
                tauntHelper.addChild(Button.builder(Component.empty(), btn -> {}).width(BUTTON_WIDTH).build());
            }

            if (betterLobbiesFeature != null) {
                tauntHelper.addChild(buildToggleButton(betterLobbiesFeature, BUTTON_WIDTH));
            }

            content.addChild(tauntRow);
        }

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    private EditBox buildMessageBox(String hint, String tooltip, String currentValue) {
        EditBox box = new EditBox(this.font, 0, 0, BUTTON_WIDTH, 20, Component.empty());
        box.setMaxLength(MAX_MESSAGE_LENGTH);
        box.setValue(currentValue);
        box.setHint(Component.literal(hint));
        box.setTooltip(Tooltip.create(Component.literal(tooltip)));
        return box;
    }

    private Button buildToggleButton(PlexFeature feature, int width) {
        Button.Builder builder = Button.builder(
                getFeatureLabel(feature),
                btn -> {
                    boolean current = PlexRegistry.isEnabled(feature.getId());
                    PlexRegistry.setEnabled(feature.getId(), !current);
                    btn.setMessage(getFeatureLabel(feature));
                }
        ).width(width);

        String tooltip = feature.getTooltip();
        if (tooltip != null && !tooltip.isBlank()) {
            builder.tooltip(Tooltip.create(Component.literal(tooltip)));
        }

        return builder.build();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    private Component getFeatureLabel(PlexFeature feature) {
        boolean enabled = PlexRegistry.isEnabled(feature.getId());
        return Component.literal(feature.getDisplayName() + ": " + (enabled ? "Enabled" : "Disabled"));
    }

    @Override
    public void onClose() {
        if (autoGgBox != null) {
            String gg = autoGgBox.getValue().trim();
            PlexConfig.getInstance().setAutoGgMessage(gg.isBlank() ? "GG!" : gg);
        }
        if (autoGlBox != null) {
            String gl = autoGlBox.getValue().trim();
            PlexConfig.getInstance().setAutoGlMessage(gl.isBlank() ? "GL HF!" : gl);
        }
        PlexConfig.getInstance().save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
package me.mubioh.plexmod.screen;

import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.feature.PlexFeature;
import me.mubioh.plexmod.core.feature.PlexRegistry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlexScreen extends GuiScreen {

    private static final int BTN_W    = 150;
    private static final int BTN_H    = 20;
    private static final int SPACING  = 4;
    private static final int MAX_LEN  = 100;

    private final GuiScreen parent;

    private GuiTextField autoGgBox;
    private GuiTextField autoGlBox;

    private PlexFeature autoGgFeature;
    private PlexFeature autoGlFeature;

    public PlexScreen(GuiScreen parent) { this.parent = parent; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        List<PlexFeature> others          = new ArrayList<>();
        PlexFeature autoTauntFeature      = null;
        PlexFeature betterLobbiesFeature  = null;
        autoGgFeature = null;
        autoGlFeature = null;

        for (PlexFeature f : PlexRegistry.getAll()) {
            if (!f.isToggleable()) continue;
            switch (f.getId()) {
                case "autogg":        autoGgFeature       = f; break;
                case "autogl":        autoGlFeature       = f; break;
                case "autotaunt":     autoTauntFeature    = f; break;
                case "better_lobbies":betterLobbiesFeature= f; break;
                default:              others.add(f);           break;
            }
        }

        int centerX = this.width / 2;
        int startY  = this.height / 4;
        int rowY    = startY;

        for (int i = 0; i < others.size(); i += 2) {
            addToggleButton(others.get(i), centerX - BTN_W - SPACING / 2, rowY);
            if (i + 1 < others.size()) addToggleButton(others.get(i + 1), centerX + SPACING / 2, rowY);
            rowY += BTN_H + SPACING;
        }

        if (autoGlFeature != null) {
            addToggleButton(autoGlFeature, centerX - BTN_W - SPACING / 2, rowY);
            autoGlBox = new GuiTextField(0, this.fontRendererObj, centerX + SPACING / 2 + 1, rowY, BTN_W - 2, BTN_H);
            autoGlBox.setMaxStringLength(MAX_LEN);
            autoGlBox.setText(PlexConfig.getInstance().getAutoGlMessage());
            rowY += BTN_H + SPACING;
        }

        if (autoGgFeature != null) {
            addToggleButton(autoGgFeature, centerX - BTN_W - SPACING / 2, rowY);
            autoGgBox = new GuiTextField(1, this.fontRendererObj, centerX + SPACING / 2 + 1, rowY, BTN_W - 2, BTN_H);            autoGgBox.setMaxStringLength(MAX_LEN);
            autoGgBox.setText(PlexConfig.getInstance().getAutoGgMessage());
            rowY += BTN_H + SPACING;
        }

        if (autoTauntFeature != null || betterLobbiesFeature != null) {
            if (autoTauntFeature     != null) addToggleButton(autoTauntFeature,     centerX - BTN_W - SPACING / 2, rowY);
            if (betterLobbiesFeature != null) addToggleButton(betterLobbiesFeature, centerX + SPACING / 2,         rowY);
            rowY += BTN_H + SPACING;
        }

        this.buttonList.add(new GuiButton(999, centerX - 75, this.height - 30, 150, BTN_H, "Done"));
    }

    private void addToggleButton(PlexFeature f, int x, int y) {
        int btnId = f.getId().hashCode() & 0x7FFFFFFF;
        this.buttonList.add(new GuiButton(btnId, x, y, BTN_W, BTN_H, featureLabel(f)));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 999) { this.mc.displayGuiScreen(parent); return; }

        for (PlexFeature f : PlexRegistry.getAll()) {
            if (!f.isToggleable()) continue;
            if ((f.getId().hashCode() & 0x7FFFFFFF) == button.id) {
                boolean current = PlexRegistry.isEnabled(f.getId());
                PlexRegistry.setEnabled(f.getId(), !current);
                button.displayString = featureLabel(f);
                return;
            }
        }
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "PlexMod Settings", this.width / 2, 20, 0xFFFFFF);
        super.drawScreen(mx, my, partial);
        if (autoGgBox != null) autoGgBox.drawTextBox();
        if (autoGlBox != null) autoGlBox.drawTextBox();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (autoGgBox != null) autoGgBox.textboxKeyTyped(typedChar, keyCode);
        if (autoGlBox != null) autoGlBox.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) this.mc.displayGuiScreen(parent);
        else super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (autoGgBox != null) autoGgBox.mouseClicked(mx, my, button);
        if (autoGlBox != null) autoGlBox.mouseClicked(mx, my, button);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        PlexConfig cfg = PlexConfig.getInstance();
        if (autoGgBox != null) {
            String v = autoGgBox.getText().trim();
            cfg.setAutoGgMessage(v.isEmpty() ? "GG!" : v);
        }
        if (autoGlBox != null) {
            String v = autoGlBox.getText().trim();
            cfg.setAutoGlMessage(v.isEmpty() ? "GL HF!" : v);
        }
        cfg.save();
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private String featureLabel(PlexFeature f) {
        return f.getDisplayName() + ": " + (PlexRegistry.isEnabled(f.getId()) ? "ON" : "OFF");
    }
}

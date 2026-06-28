package me.mubioh.plexmod.screen;

import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.feature.PlexRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlexHudScreen extends GuiScreen {

    private static final int C_BG         = 0xF0111317;
    private static final int C_SURFACE    = 0xFF161921;
    private static final int C_CARD       = 0xFF1A1D23;
    private static final int C_CARD_HOVER = 0xFF1E2229;
    private static final int C_BORDER     = 0xFF23262F;
    private static final int C_BORDER_FOC = 0xFFC2580E;
    private static final int C_ORANGE     = 0xFFF97316;
    private static final int C_ORANGE_BG  = 0x18F97316;
    private static final int C_TEXT       = 0xFFBFC2CC;
    private static final int C_TEXT_DIM   = 0xFF666B7A;
    private static final int C_TEXT_MUTED = 0xFF44475A;
    private static final int C_TOG_ON     = 0x40F97316;
    private static final int C_TOG_OFF    = 0xFF2A2D35;
    private static final int C_KNOB_ON    = 0xFFF97316;
    private static final int C_KNOB_OFF   = 0xFF666B7A;
    private static final int C_DIVIDER    = 0xFF1E2128;

    private static final Map<String, String> TOOLTIPS = new HashMap<>();
    static {
        TOOLTIPS.put("chat_cycle",     "Press Tab in chat to cycle between channels.");
        TOOLTIPS.put("autogl",         "Sends a message automatically when a game starts.");
        TOOLTIPS.put("autogg",         "Sends a message automatically when a game ends.");
        TOOLTIPS.put("autotaunt",      "Runs /taunt automatically when a game starts.");
        TOOLTIPS.put("discord_rpc",    "Shows your current Mineplex game in Discord.");
        TOOLTIPS.put("autofriend",     "Automatically accepts all incoming friend requests (while online).");
        TOOLTIPS.put("nametag",        "See your own nametag in third-person view.");
        TOOLTIPS.put("nametag_extra",  "Show player levels above their nametag.");
        TOOLTIPS.put("better_lobbies", "Hides boss bars and mutes annoying lobby sounds.");
        TOOLTIPS.put("scoreboard_red", "Hides red score numbers on the sidebar scoreboard.");
    }

    private static final int BASE_W   = 480;
    private static final int BASE_H   = 320;
    private static final int HEADER_H = 34;
    private static final int FOOTER_H = 24;
    private static final int PAD      = 9;
    private static final int ROW_H    = 28;
    private static final int ROW_GAP  = 3;
    private static final int COLS     = 3;
    private static final int EXP_H    = 24;
    private static final int TOG_W    = 26;
    private static final int TOG_H    = 13;
    private static final int KNOB_S   = 9;
    private static final int TAB_H    = 16;
    private static final int SEARCH_H = 16;
    private static final int SEARCH_W = 120;

    private static class FeatureDef {
        final String id, label;
        final int cat;
        final boolean hasExpand;
        final String expandKey, expandHint;
        FeatureDef(String id, String label, int cat, boolean hasExpand, String expandKey, String expandHint) {
            this.id = id; this.label = label; this.cat = cat;
            this.hasExpand = hasExpand; this.expandKey = expandKey; this.expandHint = expandHint;
        }
    }

    private static final int CAT_ALL     = -1;
    private static final int CAT_CHAT    =  0;
    private static final int CAT_SOCIAL  =  1;
    private static final int CAT_DISPLAY =  2;

    private static final FeatureDef[] DEFS = {
            new FeatureDef("chat_cycle",     "Chat Cycle",      CAT_CHAT,    false, null,            null),
            new FeatureDef("autogl",         "AutoGL",          CAT_CHAT,    true,  "autoGlMessage", "GL HF!"),
            new FeatureDef("autogg",         "AutoGG",          CAT_CHAT,    true,  "autoGgMessage", "GG!"),
            new FeatureDef("autotaunt",      "AutoTaunt",       CAT_CHAT,    false, null,            null),
            new FeatureDef("discord_rpc",    "Discord RPC",     CAT_SOCIAL,  false, null,            null),
            new FeatureDef("autofriend",     "AutoFriend",      CAT_SOCIAL,  false, null,            null),
            new FeatureDef("nametag",        "Own Nametag",     CAT_DISPLAY, false, null,            null),
            new FeatureDef("nametag_extra",  "Level Tag",       CAT_DISPLAY, false, null,            null),
            new FeatureDef("better_lobbies", "Cleaner Lobbies", CAT_DISPLAY, false, null,            null),
            new FeatureDef("scoreboard_red", "Hide Red Scores", CAT_DISPLAY, false, null,            null),
    };

    private static final String[] CAT_LABELS = {"All", "Chat", "Social", "Display"};

    private static class Row {
        final FeatureDef def;
        boolean      expanded = false;
        GuiTextField msgBox   = null;
        Row(FeatureDef d) { this.def = d; }
    }

    private final GuiScreen parent;
    private int panelX, panelY, panelW, panelH;
    private int contentAreaY, contentAreaH;
    private int activeCat = CAT_ALL;
    private int scrollPx  = 0;
    private GuiTextField searchBox;
    private String searchPlaceholder = "Search...";
    private final List<Row> rows = new ArrayList<>();

    private String hoveredTooltip = null;
    private int    tooltipX, tooltipY;

    private long  openTime = -1;
    private static final float ANIM_DURATION = 180f;

    public PlexHudScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int margin = 16;
        panelW = Math.min(BASE_W, this.width  - margin * 2);
        panelH = Math.min(BASE_H, this.height - margin * 2);
        panelW = Math.max(180, panelW);
        panelH = Math.max(140, panelH);
        panelX = (this.width  - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        contentAreaY = panelY + HEADER_H + 2;
        contentAreaH = panelH - HEADER_H - FOOTER_H - 4;

        String prevSearch = searchBox != null ? searchBox.getText() : "";
        int sW = Math.min(SEARCH_W, panelW / 3);
        searchBox = new GuiTextField(0, this.fontRendererObj,
                panelX + PAD + 4,
                panelY + (HEADER_H - SEARCH_H) / 2 + 4,
                sW - 8,
                SEARCH_H - 8);
        searchBox.setMaxStringLength(40);
        searchBox.setEnableBackgroundDrawing(false);
        searchBox.setText(prevSearch);

        if (openTime < 0) openTime = System.currentTimeMillis();
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        scrollPx = 0;
        String q = searchBox != null ? searchBox.getText().toLowerCase().trim() : "";

        for (FeatureDef def : DEFS) {
            if (activeCat != CAT_ALL && def.cat != activeCat) continue;
            if (!q.isEmpty() && !def.label.toLowerCase().contains(q)) continue;

            Row row = new Row(def);
            if (def.hasExpand) {
                PlexConfig cfg = PlexConfig.getInstance();
                String cur = "autoGlMessage".equals(def.expandKey)
                        ? cfg.getAutoGlMessage() : cfg.getAutoGgMessage();
                row.msgBox = new GuiTextField(def.id.hashCode(),
                        this.fontRendererObj, 0, 0, 100, 12);
                row.msgBox.setMaxStringLength(80);
                row.msgBox.setText(cur);
                row.msgBox.setEnableBackgroundDrawing(false);
                row.msgBox.setVisible(false);
            }
            rows.add(row);
        }
    }

    private int totalH() {
        int h = 0, col = 0, grpH = 0;
        for (Row r : rows) {
            int rh = ROW_H + (r.expanded && r.def.hasExpand ? EXP_H : 0) + ROW_GAP;
            grpH = Math.max(grpH, rh);
            if (++col == COLS) { h += grpH; grpH = 0; col = 0; }
        }
        if (col > 0) h += grpH;
        return h;
    }

    private float getAnimProg() {
        if (openTime < 0) return 1f;
        float elapsed = (float)(System.currentTimeMillis() - openTime);
        float t = Math.min(1f, elapsed / ANIM_DURATION);
        float f = 1f - t;
        return 1f - f * f * f;
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        float prog   = getAnimProg();
        int   slideY = (int)((1f - prog) * 10f);

        int backdropA = (int)(0x88 * prog);
        drawRect(0, 0, this.width, this.height, backdropA << 24);

        int drawPanelY = panelY + slideY;

        drawRect(panelX, drawPanelY, panelX + panelW, drawPanelY + panelH,
                applyAlpha(C_BG, prog));
        drawBorder(panelX, drawPanelY, panelW, panelH, applyAlpha(C_BORDER, prog));

        hoveredTooltip = null;
        renderHeader(mx, my, drawPanelY, prog);
        renderContent(mx, my, drawPanelY, prog);
        renderFooter(drawPanelY, prog);

        if (searchBox != null) {
            int sW = Math.min(SEARCH_W, panelW / 3);
            int sx = panelX + PAD;
            int sy = drawPanelY + (HEADER_H - SEARCH_H) / 2;

            drawRect(sx, sy, sx + sW, sy + SEARCH_H, applyAlpha(C_SURFACE, prog));
            drawBorder(sx, sy, sW, SEARCH_H,
                    applyAlpha(searchBox.isFocused() ? C_BORDER_FOC : C_BORDER, prog));

            searchBox.yPosition = sy + 4;
            searchBox.drawTextBox();
            searchBox.yPosition = panelY + (HEADER_H - SEARCH_H) / 2 + 4;

            if (searchBox.getText().isEmpty() && !searchBox.isFocused()) {
                this.fontRendererObj.drawString(searchPlaceholder,
                        sx + 4, sy + (SEARCH_H - 8) / 2,
                        applyAlpha(C_TEXT_MUTED, prog));
            }
        }

        for (Row r : rows) {
            if (r.expanded && r.msgBox != null && r.msgBox.getVisible()) {
                // draw a custom border around it first
                int bx = r.msgBox.xPosition - 3;
                int by = r.msgBox.yPosition - 3;
                int bw = r.msgBox.width + 6;
                int bh = r.msgBox.height + 6;
                drawRect(bx, by, bx + bw, by + bh, applyAlpha(C_SURFACE, prog));
                drawBorder(bx, by, bw, bh,
                        applyAlpha(r.msgBox.isFocused() ? C_BORDER_FOC : C_BORDER, prog));
                r.msgBox.drawTextBox();
            }
        }

        if (hoveredTooltip != null) {
            renderTooltip(hoveredTooltip, tooltipX, tooltipY);
        }
    }

    private void renderHeader(int mx, int my, int py, float alpha) {
        drawRect(panelX, py, panelX + panelW, py + HEADER_H, applyAlpha(C_SURFACE, alpha));
        drawRect(panelX, py + HEADER_H - 1, panelX + panelW, py + HEADER_H, applyAlpha(C_DIVIDER, alpha));

        int tx = panelX + PAD + Math.min(SEARCH_W, panelW / 3) + 8;
        int ty = py + (HEADER_H - TAB_H) / 2;
        for (int i = 0; i < CAT_LABELS.length; i++) {
            int cat = i - 1;
            int tw  = this.fontRendererObj.getStringWidth(CAT_LABELS[i]) + 10;
            boolean active = activeCat == cat;
            boolean hov    = !active && mx >= tx && mx <= tx + tw && my >= ty && my <= ty + TAB_H;

            int textColour = active ? applyAlpha(C_ORANGE, alpha)
                    : hov    ? applyAlpha(C_TEXT, alpha)
                    :          applyAlpha(C_TEXT_DIM, alpha);

            this.fontRendererObj.drawString(CAT_LABELS[i], tx + 5, ty + (TAB_H - 8) / 2, textColour);
            tx += tw + 4;
        }


        String xs = "x";
        int cx = panelX + panelW - PAD - this.fontRendererObj.getStringWidth(xs);
        int cy = py + (HEADER_H - 8) / 2;
        boolean chov = mx >= cx - 2 && mx <= cx + this.fontRendererObj.getStringWidth(xs) + 2
                && my >= py && my <= py + HEADER_H;
        this.fontRendererObj.drawString(xs, cx, cy,
                applyAlpha(chov ? C_ORANGE : C_TEXT_DIM, alpha));
    }

    private void renderContent(int mx, int my, int py, float alpha) {
        int areaX = panelX;
        int areaY = py + HEADER_H + 2;
        int areaH = contentAreaH;
        int colW  = (panelW - PAD * 2) / COLS;

        net.minecraft.client.gui.ScaledResolution sr =
                new net.minecraft.client.gui.ScaledResolution(this.mc);
        int scaleFactor = sr.getScaleFactor();

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        int scissorX = areaX * scaleFactor;
        int scissorY = this.mc.displayHeight - (areaY + areaH) * scaleFactor;
        int scissorW = panelW * scaleFactor;
        int scissorH = areaH * scaleFactor;
        org.lwjgl.opengl.GL11.glScissor(scissorX, scissorY, scissorW, scissorH);

        int startX = areaX + PAD;
        int drawY  = areaY + PAD - scrollPx;
        int col    = 0;
        int grpH   = 0;

        for (int i = 0; i < rows.size(); i++) {
            if (col == 0) {
                grpH = 0;
                for (int j = i; j < Math.min(i + COLS, rows.size()); j++) {
                    Row r = rows.get(j);
                    int rExpH = r.expanded && r.def.hasExpand ? EXP_H : 0;
                    grpH = Math.max(grpH, ROW_H + rExpH + ROW_GAP);
                }
            }

            Row row   = rows.get(i);
            int expH  = row.expanded && row.def.hasExpand ? EXP_H : 0;
            int cellX = startX + col * colW;
            int cellY = drawY;

            boolean inArea = cellY + ROW_H > areaY && cellY < areaY + areaH;
            boolean hov    = inArea && mx >= cellX + 2 && mx <= cellX + colW - 2
                    && my >= cellY && my < cellY + ROW_H;

            if (inArea) {
                drawRect(cellX + 2, cellY, cellX + colW - 2, cellY + ROW_H,
                        applyAlpha(hov ? C_CARD_HOVER : C_CARD, alpha));

                if (row.def.hasExpand) {
                    String arrow = row.expanded ? "▼" : "▶";
                    this.fontRendererObj.drawString(arrow, cellX + 5, cellY + (ROW_H - 8) / 2,
                            applyAlpha(row.expanded ? C_ORANGE : C_TEXT_DIM, alpha));
                }

                int lx = cellX + (row.def.hasExpand ? 16 : 7);
                this.fontRendererObj.drawString(row.def.label, lx, cellY + (ROW_H - 8) / 2,
                        applyAlpha(C_TEXT, alpha));

                int togX = cellX + colW - TOG_W - 7;
                int togY = cellY + (ROW_H - TOG_H) / 2;
                renderToggle(togX, togY, isOn(row.def.id), alpha);

                if (hov) {
                    String tip = TOOLTIPS.get(row.def.id);
                    if (tip != null) {
                        hoveredTooltip = tip;
                        tooltipX = mx;
                        tooltipY = cellY - 14;
                    }
                }
            }

            if (row.expanded && row.def.hasExpand) {
                int ey = cellY + ROW_H;
                if (ey < areaY + areaH) {
                    drawRect(cellX + 2, ey, cellX + colW - 2, ey + EXP_H, applyAlpha(0xFF13161C, alpha));
                    drawRect(cellX + 2, ey, cellX + colW - 2, ey + 1, applyAlpha(C_DIVIDER, alpha));
                    this.fontRendererObj.drawString("Message", cellX + 7, ey + (EXP_H - 8) / 2,
                            applyAlpha(C_TEXT_DIM, alpha));
                    if (row.msgBox != null) {
                        row.msgBox.xPosition = cellX + 56;
                        row.msgBox.yPosition = ey + (EXP_H - 12) / 2;
                        row.msgBox.width     = colW - 64;
                        row.msgBox.setVisible(true);
                    }
                }
            } else if (row.msgBox != null) {
                row.msgBox.setVisible(false);
            }

            col++;
            if (col == COLS) { drawY += grpH; grpH = 0; col = 0; }
        }

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);

        int total = totalH();
        if (total > areaH) {
            int bh = Math.max(12, areaH * areaH / total);
            int by = areaY + scrollPx * (areaH - bh) / Math.max(1, total - areaH);
            drawRect(panelX + panelW - 4, areaY, panelX + panelW - 2, areaY + areaH,
                    applyAlpha(C_CARD, alpha));
            drawRect(panelX + panelW - 4, by, panelX + panelW - 2, by + bh,
                    applyAlpha(C_TEXT_DIM, alpha));
        }
    }

    private void renderToggle(int x, int y, boolean on, float alpha) {
        drawRect(x, y, x + TOG_W, y + TOG_H, applyAlpha(on ? C_TOG_ON : C_TOG_OFF, alpha));
        drawBorder(x, y, TOG_W, TOG_H, applyAlpha(on ? C_BORDER_FOC : C_BORDER, alpha));
        int kx = on ? x + TOG_W - KNOB_S - 2 : x + 2;
        int ky = y + (TOG_H - KNOB_S) / 2;
        drawRect(kx, ky, kx + KNOB_S, ky + KNOB_S, applyAlpha(on ? C_KNOB_ON : C_KNOB_OFF, alpha));
    }

    private void renderFooter(int py, float alpha) {
        int fy = py + panelH - FOOTER_H;
        drawRect(panelX, fy, panelX + panelW, py + panelH, applyAlpha(C_SURFACE, alpha));
        drawRect(panelX, fy, panelX + panelW, fy + 1, applyAlpha(C_DIVIDER, alpha));

        long on = 0;
        for (FeatureDef d : DEFS) { if (isOn(d.id)) on++; }
        this.fontRendererObj.drawString(on + " / " + DEFS.length + " enabled",
                panelX + PAD, fy + (FOOTER_H - 8) / 2, applyAlpha(C_TEXT_MUTED, alpha));

        String link = "modrinth.com/mod/plexmod";
        this.fontRendererObj.drawString(link,
                panelX + panelW - PAD - this.fontRendererObj.getStringWidth(link),
                fy + (FOOTER_H - 8) / 2, applyAlpha(C_TEXT_MUTED, alpha));
    }

    private void renderTooltip(String text, int tx, int ty) {
        int tw  = this.fontRendererObj.getStringWidth(text) + 8;
        int th  = 14;
        int ttX = Math.min(tx, panelX + panelW - tw - 4);
        ttX     = Math.max(ttX, panelX + 4);
        int ttY = Math.max(panelY + HEADER_H + 4, ty);

        drawRect(ttX - 1, ttY - 1, ttX + tw + 1, ttY + th + 1, 0xCC000000);
        drawRect(ttX, ttY, ttX + tw, ttY + th, 0xEE111317);
        drawBorder(ttX, ttY, tw, th, C_BORDER);
        this.fontRendererObj.drawString(text, ttX + 4, ttY + (th - 8) / 2, C_TEXT);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (mx < panelX || mx > panelX + panelW || my < panelY || my > panelY + panelH) {
            playClick(); onGuiClosed(); this.mc.displayGuiScreen(parent); return;
        }

        String xs = "x";
        int cx = panelX + panelW - PAD - this.fontRendererObj.getStringWidth(xs);
        if (mx >= cx - 2 && mx <= cx + this.fontRendererObj.getStringWidth(xs) + 2
                && my >= panelY && my <= panelY + HEADER_H) {
            playClick(); onGuiClosed(); this.mc.displayGuiScreen(parent); return;
        }

        if (handleTabClick(mx, my)) return;
        if (handleContentClick(mx, my)) return;

        if (searchBox != null) searchBox.mouseClicked(mx, my, button);
        for (Row r : rows)
            if (r.msgBox != null && r.msgBox.getVisible()) r.msgBox.mouseClicked(mx, my, button);

        super.mouseClicked(mx, my, button);
    }

    private boolean handleTabClick(int mx, int my) {
        int ty = panelY + (HEADER_H - TAB_H) / 2;
        if (my < ty || my > ty + TAB_H) return false;
        int tx = panelX + PAD + Math.min(SEARCH_W, panelW / 3) + 8;
        for (int i = 0; i < CAT_LABELS.length; i++) {
            int tw  = this.fontRendererObj.getStringWidth(CAT_LABELS[i]) + 10;
            int cat = i - 1;
            if (mx >= tx && mx <= tx + tw) {
                activeCat = cat; rebuildRows(); playClick(); return true;
            }
            tx += tw + 4;
        }
        return false;
    }

    private boolean handleContentClick(int mx, int my) {
        if (my < contentAreaY || my > contentAreaY + contentAreaH) return false;
        int colW   = (panelW - PAD * 2) / COLS;
        int startX = panelX + PAD;
        int drawY  = contentAreaY + PAD - scrollPx;
        int col    = 0;
        int grpH   = 0;

        for (int i = 0; i < rows.size(); i++) {
            if (col == 0) {
                grpH = 0;
                for (int j = i; j < Math.min(i + COLS, rows.size()); j++) {
                    Row r = rows.get(j);
                    int rExpH = r.expanded && r.def.hasExpand ? EXP_H : 0;
                    grpH = Math.max(grpH, ROW_H + rExpH + ROW_GAP);
                }
            }

            Row row   = rows.get(i);
            int expH  = row.expanded && row.def.hasExpand ? EXP_H : 0;
            int cellX = startX + col * colW;
            int cellY = drawY;

            int togX = cellX + colW - TOG_W - 7;
            int togY = cellY + (ROW_H - TOG_H) / 2;
            boolean onToggle = mx >= togX && mx <= togX + TOG_W && my >= togY && my <= togY + TOG_H;
            boolean onCard   = mx >= cellX + 2 && mx <= cellX + colW - 2
                    && my >= cellY && my <= cellY + ROW_H;

            if (onToggle || (onCard && !row.def.hasExpand)) {
                toggle(row.def.id); playClick(); return true;
            }
            if (row.def.hasExpand && mx >= cellX + 2 && mx < togX - 2
                    && my >= cellY && my <= cellY + ROW_H) {
                if (row.expanded && row.msgBox != null) saveMsgBox(row);
                row.expanded = !row.expanded;
                if (!row.expanded && row.msgBox != null) row.msgBox.setVisible(false);
                playClick(); return true;
            }

            col++;
            if (col == COLS) { drawY += grpH; grpH = 0; col = 0; }
        }
        return false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            int maxScroll = Math.max(0, totalH() - contentAreaH + PAD);
            scrollPx = (int) Math.max(0, Math.min(maxScroll, scrollPx - dWheel / 5));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            onGuiClosed(); this.mc.displayGuiScreen(parent); return;
        }
        if (searchBox != null && searchBox.isFocused()) {
            String before = searchBox.getText();
            searchBox.textboxKeyTyped(typedChar, keyCode);
            if (!searchBox.getText().equals(before)) rebuildRows();
            return;
        }
        for (Row r : rows)
            if (r.msgBox != null && r.msgBox.isFocused()) {
                r.msgBox.textboxKeyTyped(typedChar, keyCode); return;
            }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        for (Row r : rows) if (r.expanded && r.msgBox != null) saveMsgBox(r);
        PlexConfig.getInstance().save();
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private void toggle(String id) {
        PlexConfig cfg = PlexConfig.getInstance();
        if ("nametag_extra".equals(id)) {
            boolean cur = cfg.getNametagExtraTag() == PlexConfig.NametagExtraTag.LEVEL;
            cfg.setNametagExtraTag(cur ? PlexConfig.NametagExtraTag.NONE : PlexConfig.NametagExtraTag.LEVEL);
        } else if ("scoreboard_red".equals(id)) {
            cfg.setFeatureEnabled("scoreboard_red", !cfg.isFeatureEnabled("scoreboard_red"));
        } else {
            PlexRegistry.setEnabled(id, !PlexRegistry.isEnabled(id));
        }
    }

    private boolean isOn(String id) {
        PlexConfig cfg = PlexConfig.getInstance();
        if ("nametag_extra".equals(id)) return cfg.getNametagExtraTag() == PlexConfig.NametagExtraTag.LEVEL;
        if ("scoreboard_red".equals(id)) return !cfg.isFeatureEnabled("scoreboard_red");
        return PlexRegistry.isEnabled(id);
    }

    private void saveMsgBox(Row row) {
        if (row.msgBox == null) return;
        String v = row.msgBox.getText().trim();
        PlexConfig cfg = PlexConfig.getInstance();
        if ("autoGlMessage".equals(row.def.expandKey)) cfg.setAutoGlMessage(v.isEmpty() ? "GL HF!" : v);
        else                                           cfg.setAutoGgMessage(v.isEmpty() ? "GG!"    : v);
    }

    private void playClick() {
        Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
    }

    private static int applyAlpha(int argb, float alpha) {
        int a = (int)(((argb >> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static void drawBorder(int x, int y, int w, int h, int c) {
        net.minecraft.client.gui.Gui.drawRect(x,         y,         x + w, y + 1,     c);
        net.minecraft.client.gui.Gui.drawRect(x,         y + h - 1, x + w, y + h,     c);
        net.minecraft.client.gui.Gui.drawRect(x,         y,         x + 1, y + h,     c);
        net.minecraft.client.gui.Gui.drawRect(x + w - 1, y,         x + w, y + h,     c);
    }
}
package me.mubioh.plexmod.screen;

import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.feature.PlexRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PlexHudScreen extends Screen {

    // ── colours ───────────────────────────────────────────────────────────
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

    // ── tooltips ──────────────────────────────────────────────────────────
    private static final java.util.Map<String, String> TOOLTIPS = new java.util.HashMap<>();
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

    // ── base layout ───────────────────────────────────────────────────────
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

    // search box inner padding (indent from custom border edge)
    private static final int SEARCH_INNER_PAD = 3;
    // message box inner padding
    private static final int MSG_INNER_PAD    = 3;

    // ── animation ─────────────────────────────────────────────────────────
    private static final float ANIM_DURATION = 180f; // ms
    private long  openTime = -1;

    // ── feature data ──────────────────────────────────────────────────────
    private record FeatureDef(String id, String label, int cat,
                              boolean hasExpand, String expandKey, String expandHint) {}

    private static final int CAT_ALL     = -1;
    private static final int CAT_CHAT    =  0;
    private static final int CAT_SOCIAL  =  1;
    private static final int CAT_DISPLAY =  2;

    private static final List<FeatureDef> DEFS = List.of(
            new FeatureDef("chat_cycle",     "Chat Cycle",      CAT_CHAT,    false, null,            null),
            new FeatureDef("autogl",         "AutoGL",          CAT_CHAT,    true,  "autoGlMessage", "GL HF!"),
            new FeatureDef("autogg",         "AutoGG",          CAT_CHAT,    true,  "autoGgMessage", "GG!"),
            new FeatureDef("autotaunt",      "AutoTaunt",       CAT_CHAT,    false, null,            null),
            new FeatureDef("discord_rpc",    "Discord RPC",     CAT_SOCIAL,  false, null,            null),
            new FeatureDef("autofriend",     "AutoFriend",      CAT_SOCIAL,  false, null,            null),
            new FeatureDef("nametag",        "Own Nametag",     CAT_DISPLAY, false, null,            null),
            new FeatureDef("nametag_extra",  "Level Tag",       CAT_DISPLAY, false, null,            null),
            new FeatureDef("better_lobbies", "Cleaner Lobbies", CAT_DISPLAY, false, null,            null),
            new FeatureDef("scoreboard_red", "Hide Red Scores", CAT_DISPLAY, false, null,            null)
    );

    private static final String[] CAT_LABELS = {"All", "Chat", "Social", "Display"};

    // ── runtime row ───────────────────────────────────────────────────────
    private static class Row {
        final FeatureDef def;
        boolean expanded = false;
        EditBox msgBox   = null;
        Row(FeatureDef d) { this.def = d; }
    }

    // ── state ─────────────────────────────────────────────────────────────
    private final Screen    parent;
    private int             panelX, panelY, panelW, panelH;
    private int             contentAreaY, contentAreaH;
    private int             activeCat = CAT_ALL;
    private int             scrollPx  = 0;
    private EditBox         searchBox;
    private final List<Row> rows = new ArrayList<>();

    // tooltip hover state
    private String hoveredTooltip = null;
    private int    tooltipX, tooltipY;

    public PlexHudScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    // ── init ──────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        int margin = 16;
        panelW = Math.min(BASE_W, this.width  - margin * 2);
        panelH = Math.min(BASE_H, this.height - margin * 2);
        panelW = Math.max(180, panelW);
        panelH = Math.max(140, panelH);

        panelX = (this.width  - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        contentAreaY = panelY + HEADER_H + 2;
        contentAreaH = panelH - HEADER_H - FOOTER_H - 4;

        String prevSearch = searchBox != null ? searchBox.getValue() : "";

        // The custom border box spans: x=[sx-1 .. sx+sw+1], y=[sy-1 .. sy+SEARCH_H+1]
        // We want the EditBox text vertically centred inside that box.
        // EditBox (unbordered) renders text at its own y+1.
        // Box centre y = sy + SEARCH_H/2.  Text centre needs: editY + 1 + 4 = box centre
        // => editY = sy + SEARCH_H/2 - 5
        int sw   = Math.min(SEARCH_W, panelW / 3);
        int sy   = panelY + (HEADER_H - SEARCH_H) / 2;
        int editH = 10; // just needs to be >= font height (8)
        int editY = sy + 4;

        searchBox = new EditBox(this.font,
                panelX + PAD + SEARCH_INNER_PAD,   // indent text from left border
                editY,
                sw - SEARCH_INNER_PAD * 2,
                editH,
                Component.empty());
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setMaxLength(40);
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFBFC2CC);
        searchBox.setValue(prevSearch);
        searchBox.setResponder(s -> rebuildRows());
        this.addWidget(searchBox);

        if (openTime < 0) openTime = System.currentTimeMillis();

        rebuildRows();
    }

    // ── row building ──────────────────────────────────────────────────────
    private void rebuildRows() {
        for (Row r : rows) if (r.msgBox != null) this.removeWidget(r.msgBox);
        rows.clear();
        scrollPx = 0;

        String q = searchBox != null ? searchBox.getValue().toLowerCase().trim() : "";

        for (FeatureDef def : DEFS) {
            if (activeCat != CAT_ALL && def.cat() != activeCat) continue;
            if (!q.isEmpty() && !def.label().toLowerCase().contains(q)) continue;

            Row row = new Row(def);
            if (def.hasExpand()) {
                PlexConfig cfg = PlexConfig.getInstance();
                String cur = def.expandKey().equals("autoGlMessage")
                        ? cfg.getAutoGlMessage() : cfg.getAutoGgMessage();
                int msgH = 10;
                row.msgBox = new EditBox(this.font, 0, 0, 100, msgH, Component.empty());
                row.msgBox.setMaxLength(80);
                row.msgBox.setValue(cur);
                row.msgBox.setBordered(false);
                row.msgBox.setTextColor(0xFFBFC2CC);
                row.msgBox.setVisible(false);
                this.addWidget(row.msgBox);
            }
            rows.add(row);
        }
    }

    private int totalH() {
        int h = 0, col = 0, grpH = 0;
        for (Row r : rows) {
            int rh = ROW_H + (r.expanded && r.def.hasExpand() ? EXP_H : 0) + ROW_GAP;
            grpH = Math.max(grpH, rh);
            if (++col == COLS) { h += grpH; grpH = 0; col = 0; }
        }
        if (col > 0) h += grpH;
        return h;
    }

    // ── animation easing ──────────────────────────────────────────────────
    private static float easeOutCubic(float t) {
        float f = 1f - t;
        return 1f - f * f * f;
    }

    private float getAnimProg() {
        if (openTime < 0) return 1f;
        float elapsed = (float)(System.currentTimeMillis() - openTime);
        return Math.min(1f, elapsed / ANIM_DURATION);
    }

    // ── render ────────────────────────────────────────────────────────────
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        float prog   = easeOutCubic(getAnimProg());
        float alpha  = prog;
        int   slideY = (int)((1f - prog) * 10f);

        int backdropAlpha = (int)(0x88 * alpha);
        g.fill(0, 0, this.width, this.height, (backdropAlpha << 24));

        int drawPanelY = panelY + slideY;

        int bgAlpha = (int)(0xF0 * alpha);
        g.fill(panelX, drawPanelY, panelX + panelW, drawPanelY + panelH,
                (bgAlpha << 24) | (C_BG & 0x00FFFFFF));
        drawBorderAlpha(g, panelX, drawPanelY, panelW, panelH, C_BORDER, alpha);

        hoveredTooltip = null;
        renderHeader(g, mx, my, drawPanelY, alpha);
        renderContent(g, mx, my, drawPanelY, alpha);
        renderFooter(g, drawPanelY, alpha);

        // ── search box ────────────────────────────────────────────────────
        int sw = Math.min(SEARCH_W, panelW / 3);
        int sx = panelX + PAD;
        int sy = drawPanelY + (HEADER_H - SEARCH_H) / 2;

        g.fill(sx - 1, sy - 1, sx + sw + 1, sy + SEARCH_H + 1, applyAlpha(C_SURFACE, alpha));
        drawBorderAlpha(g, sx - 1, sy - 1, sw + 2, SEARCH_H + 2,
                searchBox.isFocused() ? C_BORDER_FOC : C_BORDER, alpha);

        searchBox.setX(sx + SEARCH_INNER_PAD);
        searchBox.setY(sy + 4);  // keep Y in animated position — no restore
        searchBox.setWidth(sw - SEARCH_INNER_PAD * 2);
        searchBox.extractRenderState(g, mx, my, delta);

        // ── message boxes ─────────────────────────────────────────────────
        for (Row r : rows) {
            if (r.expanded && r.msgBox != null && r.msgBox.isVisible()) {
                r.msgBox.extractRenderState(g, mx, my, delta);
            }
        }

        if (hoveredTooltip != null) {
            renderTooltip(g, hoveredTooltip, tooltipX, tooltipY);
        }
    }

    private void renderHeader(GuiGraphicsExtractor g, int mx, int my, int py, float alpha) {
        g.fill(panelX, py, panelX + panelW, py + HEADER_H, applyAlpha(C_SURFACE, alpha));
        g.fill(panelX, py + HEADER_H - 1, panelX + panelW, py + HEADER_H, applyAlpha(C_DIVIDER, alpha));

        int tx = panelX + PAD + Math.min(SEARCH_W, panelW / 3) + 8;
        int ty = py + (HEADER_H - TAB_H) / 2;
        for (int i = 0; i < CAT_LABELS.length; i++) {
            int cat = i - 1;
            int tw  = this.font.width(CAT_LABELS[i]) + 10;
            boolean active = activeCat == cat;
            boolean hov    = !active && mx >= tx && mx <= tx + tw && my >= ty && my <= ty + TAB_H;
            if (active) {
                g.text(this.font, CAT_LABELS[i], tx + 5, ty + (TAB_H - 8) / 2, applyAlpha(C_ORANGE, alpha));
            } else {
                g.text(this.font, CAT_LABELS[i], tx + 5, ty + (TAB_H - 8) / 2,
                        applyAlpha(hov ? C_TEXT : C_TEXT_DIM, alpha));
            }
            tx += tw + 4;
        }

        String xs = "✕";
        int cx = panelX + panelW - PAD - this.font.width(xs);
        int cy = py + (HEADER_H - 8) / 2;
        boolean chov = mx >= cx - 2 && mx <= cx + this.font.width(xs) + 2
                && my >= py && my <= py + HEADER_H;
        g.text(this.font, xs, cx, cy, applyAlpha(chov ? C_ORANGE : C_TEXT_DIM, alpha));
    }

    private void renderContent(GuiGraphicsExtractor g, int mx, int my, int py, float alpha) {
        int areaX = panelX;
        int areaY = py + HEADER_H + 2;
        int areaH = contentAreaH;
        int colW  = (panelW - PAD * 2) / COLS;

        g.enableScissor(areaX, areaY, areaX + panelW, areaY + areaH);

        int startX = areaX + PAD;
        int drawY  = areaY + PAD - scrollPx;
        int col    = 0;
        int grpH   = 0;

        for (Row row : rows) {
            int expH  = row.expanded && row.def.hasExpand() ? EXP_H : 0;
            int cellH = ROW_H + expH;
            int cellX = startX + col * colW;
            int cellY = drawY;

            boolean inArea = cellY + ROW_H > areaY && cellY < areaY + areaH;
            boolean hov    = inArea && mx >= cellX + 2 && mx <= cellX + colW - 2
                    && my >= cellY && my < cellY + ROW_H;

            if (inArea) {
                g.fill(cellX + 2, cellY, cellX + colW - 2, cellY + ROW_H,
                        applyAlpha(hov ? C_CARD_HOVER : C_CARD, alpha));

                if (row.def.hasExpand()) {
                    g.text(this.font, row.expanded ? "▼" : "▶",
                            cellX + 5, cellY + (ROW_H - 8) / 2,
                            applyAlpha(C_TEXT_DIM, alpha));
                }

                int lx = cellX + (row.def.hasExpand() ? 16 : 7);
                g.text(this.font, row.def.label(), lx, cellY + (ROW_H - 8) / 2,
                        applyAlpha(C_TEXT, alpha));

                int togX = cellX + colW - TOG_W - 7;
                int togY = cellY + (ROW_H - TOG_H) / 2;
                renderToggle(g, togX, togY, isOn(row.def.id()), alpha);

                if (hov) {
                    String tip = TOOLTIPS.get(row.def.id());
                    if (tip != null) {
                        hoveredTooltip = tip;
                        tooltipX = mx;
                        tooltipY = cellY - 14;
                    }
                }
            }

            // expand panel
            if (row.expanded && row.def.hasExpand()) {
                int ey = cellY + ROW_H;
                if (ey < areaY + areaH) {
                    g.fill(cellX + 2, ey, cellX + colW - 2, ey + EXP_H, applyAlpha(0xFF13161C, alpha));
                    g.fill(cellX + 2, ey, cellX + colW - 2, ey + 1, applyAlpha(C_DIVIDER, alpha));
                    g.text(this.font, "Message", cellX + 7, ey + (EXP_H - 8) / 2,
                            applyAlpha(C_TEXT_DIM, alpha));

                    if (row.msgBox != null) {
                        // custom border box for message field
                        int bx = cellX + 56;
                        int bw = colW - 64;
                        int boxH = 14;
                        int by = ey + (EXP_H - boxH) / 2;

                        g.fill(bx - 1, by - 1, bx + bw + 1, by + boxH + 1, applyAlpha(C_SURFACE, alpha));
                        drawBorderAlpha(g, bx - 1, by - 1, bw + 2, boxH + 2,
                                row.msgBox.isFocused() ? C_BORDER_FOC : C_BORDER, alpha);

                        // position EditBox centred + indented inside the border
                        int msgH   = row.msgBox.getHeight(); // 10
                        int editY  = by + (boxH - msgH) / 2 + 1;
                        row.msgBox.setX(bx + MSG_INNER_PAD);
                        row.msgBox.setY(editY);
                        row.msgBox.setWidth(bw - MSG_INNER_PAD * 2);
                        row.msgBox.setVisible(true);
                    }
                }
            } else if (row.msgBox != null) {
                row.msgBox.setVisible(false);
            }

            grpH = Math.max(grpH, cellH + ROW_GAP);
            col++;
            if (col == COLS) { drawY += grpH; grpH = 0; col = 0; }
        }

        g.disableScissor();

        int total = totalH();
        if (total > areaH) {
            int bh = Math.max(12, areaH * areaH / total);
            int by = areaY + scrollPx * (areaH - bh) / Math.max(1, total - areaH);
            g.fill(panelX + panelW - 4, areaY, panelX + panelW - 2, areaY + areaH,
                    applyAlpha(C_CARD, alpha));
            g.fill(panelX + panelW - 4, by, panelX + panelW - 2, by + bh,
                    applyAlpha(C_TEXT_DIM, alpha));
        }
    }

    private void renderToggle(GuiGraphicsExtractor g, int x, int y, boolean on, float alpha) {
        g.fill(x, y, x + TOG_W, y + TOG_H, applyAlpha(on ? C_TOG_ON : C_TOG_OFF, alpha));
        drawBorderAlpha(g, x, y, TOG_W, TOG_H, on ? C_BORDER_FOC : C_BORDER, alpha);
        int kx = on ? x + TOG_W - KNOB_S - 2 : x + 2;
        int ky = y + (TOG_H - KNOB_S) / 2;
        g.fill(kx, ky, kx + KNOB_S, ky + KNOB_S, applyAlpha(on ? C_KNOB_ON : C_KNOB_OFF, alpha));
    }

    private void renderFooter(GuiGraphicsExtractor g, int py, float alpha) {
        int fy = py + panelH - FOOTER_H;
        g.fill(panelX, fy, panelX + panelW, py + panelH, applyAlpha(C_SURFACE, alpha));
        g.fill(panelX, fy, panelX + panelW, fy + 1, applyAlpha(C_DIVIDER, alpha));

        long on = DEFS.stream().filter(d -> isOn(d.id())).count();
        g.text(this.font, on + " / " + DEFS.size() + " enabled",
                panelX + PAD, fy + (FOOTER_H - 8) / 2, applyAlpha(C_TEXT_MUTED, alpha));

        String link = "modrinth.com/mod/plexmod";
        g.text(this.font, link,
                panelX + panelW - PAD - this.font.width(link),
                fy + (FOOTER_H - 8) / 2, applyAlpha(C_TEXT_MUTED, alpha));
    }

    private void renderTooltip(GuiGraphicsExtractor g, String text, int mx, int ty) {
        int tw  = this.font.width(text) + 8;
        int th  = 14;
        int tx  = Math.min(mx, panelX + panelW - tw - 4);
        tx      = Math.max(tx, panelX + 4);
        int ttY = Math.max(panelY + HEADER_H + 4, ty);

        g.fill(tx - 1, ttY - 1, tx + tw + 1, ttY + th + 1, 0xCC000000);
        g.fill(tx, ttY, tx + tw, ttY + th, 0xEE111317);
        drawBorder(g, tx, ttY, tw, th, C_BORDER);
        g.text(this.font, text, tx + 4, ttY + (th - 8) / 2, C_TEXT);
    }

    // ── input ─────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();

        if (mx < panelX || mx > panelX + panelW || my < panelY || my > panelY + panelH) {
            playClick(); onClose(); return true;
        }

        String xs = "✕";
        int cx = panelX + panelW - PAD - this.font.width(xs);
        if (mx >= cx - 2 && mx <= cx + this.font.width(xs) + 2
                && my >= panelY && my <= panelY + HEADER_H) {
            playClick(); onClose(); return true;
        }

        if (handleTabClick(mx, my))     return true;
        if (handleContentClick(mx, my)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleTabClick(int mx, int my) {
        int ty = panelY + (HEADER_H - TAB_H) / 2;
        if (my < ty || my > ty + TAB_H) return false;
        int tx = panelX + PAD + Math.min(SEARCH_W, panelW / 3) + 8;
        for (int i = 0; i < CAT_LABELS.length; i++) {
            int tw  = this.font.width(CAT_LABELS[i]) + 10;
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
        int grpStart = 0;

        for (int i = 0; i < rows.size(); i++) {
            // at the start of each group, precompute max height across all COLS in this group
            if (col == 0) {
                grpH = 0;
                for (int j = i; j < Math.min(i + COLS, rows.size()); j++) {
                    Row r = rows.get(j);
                    int rExpH = r.expanded && r.def.hasExpand() ? EXP_H : 0;
                    grpH = Math.max(grpH, ROW_H + rExpH + ROW_GAP);
                }
            }

            Row row  = rows.get(i);
            int expH = row.expanded && row.def.hasExpand() ? EXP_H : 0;
            int cellX = startX + col * colW;
            int cellY = drawY;

            int togX = cellX + colW - TOG_W - 7;
            int togY = cellY + (ROW_H - TOG_H) / 2;
            boolean onToggle = mx >= togX && mx <= togX + TOG_W && my >= togY && my <= togY + TOG_H;
            boolean onCard   = mx >= cellX + 2 && mx <= cellX + colW - 2
                    && my >= cellY && my <= cellY + ROW_H;
            if (onToggle || (onCard && !row.def.hasExpand())) {
                toggle(row.def.id()); playClick(); return true;
            }
            if (row.def.hasExpand() && mx >= cellX + 2 && mx < togX - 2
                    && my >= cellY && my <= cellY + ROW_H) {
                row.expanded = !row.expanded;
                if (!row.expanded && row.msgBox != null) {
                    saveMsgBox(row); row.msgBox.setVisible(false);
                }
                playClick(); return true;
            }

            col++;
            if (col == COLS) { drawY += grpH; grpH = 0; col = 0; }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int maxScroll = Math.max(0, totalH() - contentAreaH + PAD);
        scrollPx = (int) Math.max(0, Math.min(maxScroll, scrollPx - dy * 10));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (searchBox != null && searchBox.isFocused()) return searchBox.keyPressed(event);
        for (Row r : rows)
            if (r.msgBox != null && r.msgBox.isFocused()) return r.msgBox.keyPressed(event);
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBox != null && searchBox.isFocused()) return searchBox.charTyped(event);
        for (Row r : rows)
            if (r.msgBox != null && r.msgBox.isFocused()) return r.msgBox.charTyped(event);
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        for (Row r : rows) if (r.expanded && r.msgBox != null) saveMsgBox(r);
        PlexConfig.getInstance().save();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── helpers ───────────────────────────────────────────────────────────
    private void toggle(String id) {
        PlexConfig cfg = PlexConfig.getInstance();
        if (id.equals("nametag_extra")) {
            boolean cur = cfg.getNametagExtraTag() == PlexConfig.NametagExtraTag.LEVEL;
            cfg.setNametagExtraTag(cur ? PlexConfig.NametagExtraTag.NONE : PlexConfig.NametagExtraTag.LEVEL);
            cfg.save();
        } else if (id.equals("scoreboard_red")) {
            cfg.setFeatureEnabled("scoreboard_red", !cfg.isFeatureEnabled("scoreboard_red"));
        } else {
            PlexRegistry.setEnabled(id, !PlexRegistry.isEnabled(id));
        }
    }

    private boolean isOn(String id) {
        PlexConfig cfg = PlexConfig.getInstance();
        if (id.equals("nametag_extra"))
            return cfg.getNametagExtraTag() == PlexConfig.NametagExtraTag.LEVEL;
        if (id.equals("scoreboard_red"))
            return !cfg.isFeatureEnabled("scoreboard_red");
        return PlexRegistry.isEnabled(id);
    }

    private void saveMsgBox(Row row) {
        if (row.msgBox == null) return;
        String v = row.msgBox.getValue().trim();
        PlexConfig cfg = PlexConfig.getInstance();
        if (row.def.expandKey().equals("autoGlMessage"))
            cfg.setAutoGlMessage(v.isEmpty() ? "GL HF!" : v);
        else
            cfg.setAutoGgMessage(v.isEmpty() ? "GG!" : v);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    // ── colour utilities ──────────────────────────────────────────────────
    private static int applyAlpha(int argb, float alpha) {
        int a = (int)(((argb >> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int alphaInt(float alpha) { return (int)(alpha * 255f); }

    private static void drawBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x,         y,         x + w, y + 1,     c);
        g.fill(x,         y + h - 1, x + w, y + h,     c);
        g.fill(x,         y,         x + 1, y + h,     c);
        g.fill(x + w - 1, y,         x + w, y + h,     c);
    }

    private static void drawBorderAlpha(GuiGraphicsExtractor g, int x, int y, int w, int h, int c, float alpha) {
        drawBorder(g, x, y, w, h, applyAlpha(c, alpha));
    }
}
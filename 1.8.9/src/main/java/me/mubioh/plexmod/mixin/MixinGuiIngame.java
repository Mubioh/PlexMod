package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.util.GameDetectorUtil;
import me.mubioh.plexmod.feature.betterlobbies.BetterLobbiesFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(value = GuiIngame.class)
public abstract class MixinGuiIngame extends Gui {

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    private void onRenderBossHealth(CallbackInfo ci) {
        if (BetterLobbiesFeature.getInstance() == null) return;
        if (GameDetectorUtil.isInLobby()) ci.cancel();
    }

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(ScoreObjective objective, ScaledResolution resolution, CallbackInfo ci) {
        ci.cancel();
        drawScoreboardWithoutNumbers(objective, resolution);
    }

    private void drawScoreboardWithoutNumbers(ScoreObjective objective, ScaledResolution resolution) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        Scoreboard scoreboard = objective.getScoreboard();

        List<Score> scores = new ArrayList<Score>();
        for (Score score : (Collection<Score>) scoreboard.getSortedScores(objective)) {
            String name = score.getPlayerName();
            if (scores.size() < 15 && name != null && !name.startsWith("#")) {
                scores.add(score);
            }
        }

        int width = fr.getStringWidth(objective.getDisplayName());
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            String withNumber = line + ": " + score.getScorePoints();
            width = Math.max(width, fr.getStringWidth(withNumber));
        }

        int sidebarX = resolution.getScaledWidth() - width - 3;
        int sidebarY = (resolution.getScaledHeight() + scores.size() * fr.FONT_HEIGHT) / 2;
        int scoreX = sidebarX + width + 1;
        int bgColor = 0x44000000;
        int headerColor = 0x66000000;

        int index = 0;
        for (Score score : scores) {
            index++;
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            int y = sidebarY - index * fr.FONT_HEIGHT;

            drawRect(sidebarX - 2, y, scoreX, y + fr.FONT_HEIGHT, bgColor);
            fr.drawString(line, sidebarX, y, 0xFFFFFF);

            if (index == scores.size()) {
                String title = objective.getDisplayName();
                drawRect(sidebarX - 2, y - fr.FONT_HEIGHT - 1, scoreX, y - 1, headerColor);
                drawRect(sidebarX - 2, y - 1, scoreX, y, bgColor);
                fr.drawString(title,
                        sidebarX + (width - fr.getStringWidth(title)) / 2,
                        y - fr.FONT_HEIGHT, 0xFFFFFF);
            }
        }
    }
}
package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.config.PlexConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
public class ScoreboardScoreMixin {

    @Redirect(
            method = "displayScoreboardSidebar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
            )
    )
    private void suppressScoreNumbers(GuiGraphicsExtractor graphics, Font font,
                                      Component text, int x, int y, int color, boolean shadow) {
        String plain = text.getString().trim();

        if (plain.matches("-?\\d+") && !PlexConfig.getInstance().isFeatureEnabled("scoreboard_red")) {
            return;
        }

        graphics.text(font, text, x, y, color, shadow);
    }
}

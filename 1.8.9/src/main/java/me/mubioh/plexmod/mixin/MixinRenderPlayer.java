package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.util.PlayerDataService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderPlayer.class)
public abstract class MixinRenderPlayer {

    @Inject(method = "renderOffsetLivingLabel", at = @At("RETURN"))
    private void plexmod$renderLevelTag(AbstractClientPlayer player,
                                        double x, double y, double z,
                                        String name, float fontScale, double distanceSq,
                                        CallbackInfo ci) {
        if (PlexConfig.getInstance().getNametagExtraTag() != PlexConfig.NametagExtraTag.LEVEL) return;
        if (distanceSq >= 64.0D * 64.0D) return;
        if (player.ridingEntity != null) return;

        String uuid = player.getUniqueID().toString();
        PlayerDataService.PlayerData data = PlayerDataService.getInstance().get(uuid);
        if (data == null || data.level <= 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.client.gui.FontRenderer fr = mc.fontRendererObj;
        RenderManager rm = mc.getRenderManager();

        String prefix = "Level: ";
        String number = String.valueOf(data.level);
        int totalW = fr.getStringWidth(prefix + number);
        int color  = levelColor(data.level);

        // Build labelY the same way the reference mod does —
        // feet Y + full entity height + padding to clear the nametag stack
        double nametagBase = y + player.height + 0.5D;

        // If scoreboard score is showing, vanilla pushes the name up one line
        net.minecraft.scoreboard.ScoreObjective obj =
                player.getWorldScoreboard().getObjectiveInDisplaySlot(2);
        if (obj != null && distanceSq < 100.0D) {
            nametagBase += (double)(fr.FONT_HEIGHT * 1.15F * fontScale);
        }

        // Our tag goes one more line above the name
        double labelY = nametagBase + (double)(fr.FONT_HEIGHT * 1.15F * fontScale);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) labelY, (float) z);
        org.lwjgl.opengl.GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX,  1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-fontScale, -fontScale, fontScale);
        GlStateManager.disableLighting();

        if (player.isSneaking()) {
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            net.minecraft.client.gui.Gui.drawRect(-totalW / 2 - 1, -1, totalW / 2 + 1, fr.FONT_HEIGHT, 0x22000000);
            fr.drawString(prefix, -totalW / 2, 0, 0x22FFFF55);
            fr.drawString(number, -totalW / 2 + fr.getStringWidth(prefix), 0, color & 0x22FFFFFF);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        } else {
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            net.minecraft.client.gui.Gui.drawRect(-totalW / 2 - 1, -1, totalW / 2 + 1, fr.FONT_HEIGHT, 0x40000000);
            fr.drawString(prefix, -totalW / 2, 0, 0xFFFF55);
            fr.drawString(number, -totalW / 2 + fr.getStringWidth(prefix), 0, color);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        }

        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @Unique
    private static int levelColor(int level) {
        if (level < 20) return 0xAAAAAA;
        if (level < 40) return 0x5555FF;
        if (level < 60) return 0x55FF55;
        if (level < 80) return 0xFFAA00;
        return 0xFF5555;
    }
}

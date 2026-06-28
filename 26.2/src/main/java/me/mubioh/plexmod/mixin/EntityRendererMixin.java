package me.mubioh.plexmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.util.PlayerDataService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class EntityRendererMixin {

    @Unique private static final float LINE_HEIGHT = 9.0f * 1.15f * 0.025f;

    @Unique private static final int COLOR_GRAY   = 0xAAAAAA;
    @Unique private static final int COLOR_BLUE   = 0x5555FF;
    @Unique private static final int COLOR_GREEN  = 0x55FF55;
    @Unique private static final int COLOR_GOLD   = 0xFFAA00;
    @Unique private static final int COLOR_RED    = 0xFF5555;
    @Unique private static final int COLOR_YELLOW = 0xFFFF55;

    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;" +
                    "Lcom/mojang/blaze3d/vertex/PoseStack;" +
                    "Lnet/minecraft/client/renderer/SubmitNodeCollector;" +
                    "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN")
    )
    private void plexmod$renderExtraTag(AvatarRenderState renderState, PoseStack poseStack,
                                        SubmitNodeCollector collector, CameraRenderState camera,
                                        CallbackInfo ci) {
        if (PlexConfig.getInstance().getNametagExtraTag() != PlexConfig.NametagExtraTag.LEVEL) return;

        if (renderState.nameTag == null || renderState.nameTagAttachment == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(renderState.id);
        if (!(entity instanceof Player player)) return;

        String uuid = player.getStringUUID();
        PlayerDataService svc = PlayerDataService.getInstance();
        svc.requestPlayer(uuid);

        PlayerDataService.PlayerData data = svc.get(uuid);
        if (data == null || data.level <= 0) return;

        int levelColor = levelColor(data.level);
        Component label = Component.literal("Level: ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_YELLOW)))
                .append(Component.literal(String.valueOf(data.level))
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(levelColor))));

        boolean hasTitle = false;
        for (Entity passenger : player.getPassengers()) {
            if (isInvisibleArmorStandWithName(passenger)) { hasTitle = true; break; }
            for (Entity passenger2 : passenger.getPassengers()) {
                if (isInvisibleArmorStandWithName(passenger2)) { hasTitle = true; break; }
                for (Entity passenger3 : passenger2.getPassengers()) {
                    if (isInvisibleArmorStandWithName(passenger3)) { hasTitle = true; break; }
                }
            }
        }

        if (renderState.scoreText != null) hasTitle = true;

        float offset = hasTitle ? LINE_HEIGHT * 2.5f : LINE_HEIGHT * 1.5f;

        poseStack.pushPose();
        poseStack.translate(0.0f, offset, 0.0f);

        collector.submitNameTag(
                poseStack,
                renderState.nameTagAttachment,
                0,
                label,
                !renderState.isDiscrete,
                renderState.lightCoords,
                camera
        );

        poseStack.popPose();
    }

    @Unique
    private static boolean isInvisibleArmorStandWithName(Entity entity) {
        return entity instanceof net.minecraft.world.entity.decoration.ArmorStand stand
                && stand.isInvisible()
                && stand.hasCustomName()
                && !stand.getCustomName().getString().isBlank();
    }

    @Unique
    private static int levelColor(int level) {
        if (level < 20) return COLOR_GRAY;
        if (level < 40) return COLOR_BLUE;
        if (level < 60) return COLOR_GREEN;
        if (level < 80) return COLOR_GOLD;
        return COLOR_RED;
    }
}
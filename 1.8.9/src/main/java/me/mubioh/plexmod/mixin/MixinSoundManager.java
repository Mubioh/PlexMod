package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.util.GameDetectorUtil;
import me.mubioh.plexmod.feature.betterlobbies.BetterLobbiesFeature;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundManager.class)
public class MixinSoundManager {

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(ISound sound, CallbackInfo ci) {
        if (BetterLobbiesFeature.getInstance() == null) return;
        if (!GameDetectorUtil.isInLobby()) return;
        cancelIfAnnoying(sound, ci);
    }

    @Inject(method = "playDelayedSound", at = @At("HEAD"), cancellable = true)
    private void onPlayDelayedSound(ISound sound, int delay, CallbackInfo ci) {
        if (BetterLobbiesFeature.getInstance() == null) return;
        if (!GameDetectorUtil.isInLobby()) return;
        cancelIfAnnoying(sound, ci);
    }

    private static void cancelIfAnnoying(ISound sound, CallbackInfo ci) {
        String path = sound.getSoundLocation().getResourcePath();
        if (path.contains("portal")
                || path.contains("fire.")
                || path.contains("cat.")
                || path.contains("zombie.")
                || path.contains("mob.enderdragon.flap")
                || path.contains("mob.enderdragon.growl")
                || path.contains("mob.wither.spawn")
                || path.contains("ambient.weather.thunder")) {
            ci.cancel();
        }
    }
}

package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.feature.chatcycle.ChatCycleFeature;
import net.minecraft.util.IChatComponent;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiNewChat.class)
public class MixinGuiNewChat {

    @Inject(method = "printChatMessageWithOptionalDeletion",
            at = @At("HEAD"), cancellable = true)
    private void onPrintChat(IChatComponent component, int chatLineId, CallbackInfo ci) {
        ChatCycleFeature f = ChatCycleFeature.getInstance();
        if (f == null) return;

        if (f.isRepopulating()) return;

        f.onChatMessage(component);

        if (!f.currentChannelMatches(component.getUnformattedText())) {
            ci.cancel();
        }
    }
}

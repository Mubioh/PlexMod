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

    /**
     * printChatMessageWithOptionalDeletion is the main message-add entry point in 1.8.9.
     * We intercept here to:
     *   1. Route the message to ChatCycleFeature for history tracking.
     *   2. Cancel display if the message doesn't match the active channel.
     */
    @Inject(method = "printChatMessageWithOptionalDeletion",
            at = @At("HEAD"), cancellable = true)
    private void onPrintChat(IChatComponent component, int chatLineId, CallbackInfo ci) {
        ChatCycleFeature f = ChatCycleFeature.getInstance();
        if (f == null) return;

        if (f.isRepopulating()) return; // let it through unfiltered

        // Route into channel history
        f.onChatMessage(component);

        // Only display if it matches the current channel
        if (!f.currentChannelMatches(component.getUnformattedText())) {
            ci.cancel();
        }
    }
}

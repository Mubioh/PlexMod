package me.mubioh.plexmod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.mubioh.plexmod.feature.chatcycle.ChatCycleFeature;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(method = "addMessage", at = @At("HEAD"))
    private void onAddMessage(Component contents, MessageSignature signature,
                              GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        ChatCycleFeature feature = ChatCycleFeature.getInstance();
        if (feature == null || feature.isRepopulating()) return;
        feature.onChatMessage(contents);
    }

    @WrapMethod(method = "addMessageToDisplayQueue")
    private void onAddMessageToDisplayQueue(GuiMessage message, Operation<Void> original) {
        ChatCycleFeature feature = ChatCycleFeature.getInstance();
        if (feature == null || feature.isRepopulating()) {
            original.call(message);
            return;
        }

        String plainText = message.content().getString();
        if (feature.currentChannelMatches(plainText)) {
            original.call(message);
        }
    }
}
package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.chat.ChatChannel;
import me.mubioh.plexmod.feature.chatcycle.ChatCycleFeature;
import me.mubioh.plexmod.feature.chatcycle.ChatCycleHudRenderer;
import me.mubioh.plexmod.feature.chatcycle.DmChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow protected EditBox input;

    @Unique private long plexmod$lastTabClickTime = 0;
    @Unique private Object plexmod$lastTabClicked = null;

    private static final long DOUBLE_CLICK_MS = 400;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ChatCycleFeature feature = ChatCycleFeature.getInstance();
        if (feature == null) return;
        plexmod$refreshDisplay();
        feature.setOnChannelChanged(this::plexmod$refreshDisplay);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        ChatCycleFeature feature = ChatCycleFeature.getInstance();
        if (feature == null || input == null) return;

        if (event.key() == GLFW.GLFW_KEY_TAB && input.getValue().isBlank()) {
            feature.cycle();
            cir.setReturnValue(true);
            return;
        }

        if (!event.isConfirmation()) return;

        if (feature.getCurrentDmChannel() != null) {
            String current = input.getValue();
            if (!current.isBlank() && !current.startsWith("/")) {
                input.setValue("/msg " + feature.getCurrentDmChannel().getPlayerName() + " " + current);
            }
            return;
        }

        ChatChannel channel = feature.getCurrentChannel();
        if (channel.prefix == null) return;

        String current = input.getValue();
        if (current.isBlank() || current.startsWith("/")) return;

        input.setValue(channel.prefix + current);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        ChatCycleFeature feature = ChatCycleFeature.getInstance();
        if (feature == null || input == null) return;

        int tabY = input.getY() - ChatCycleHudRenderer.TAB_HEIGHT - ChatCycleHudRenderer.TAB_GAP;
        int tabX = input.getX();
        int mx   = (int) event.x();
        int my   = (int) event.y();

        ChatChannel clickedFixed = ChatCycleHudRenderer.getClickedFixedTab(
                mx, my, tabX, tabY, feature.isInParty(), feature.isInTeamGame());
        if (clickedFixed != null) {
            long now = System.currentTimeMillis();
            boolean isDoubleClick = clickedFixed == plexmod$lastTabClicked
                    && (now - plexmod$lastTabClickTime) <= DOUBLE_CLICK_MS;
            plexmod$lastTabClicked   = clickedFixed;
            plexmod$lastTabClickTime = now;
            feature.handleTabClick(clickedFixed, isDoubleClick);
            cir.setReturnValue(true);
            return;
        }

        DmChannel clickedDm = ChatCycleHudRenderer.getClickedDmTab(
                mx, my, tabX, tabY, feature.isInParty(), feature.isInTeamGame(), feature.getDmChannels());
        if (clickedDm != null) {
            long now = System.currentTimeMillis();
            boolean isDoubleClick = clickedDm == plexmod$lastTabClicked
                    && (now - plexmod$lastTabClickTime) <= DOUBLE_CLICK_MS;
            plexmod$lastTabClicked   = clickedDm;
            plexmod$lastTabClickTime = now;
            feature.handleDmTabClick(clickedDm, isDoubleClick);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ChatCycleFeature feature = ChatCycleFeature.getInstance();
        if (feature == null || input == null) return;

        ChatCycleHudRenderer.renderTabs(
                graphics,
                input.getX(),
                input.getY() - ChatCycleHudRenderer.TAB_HEIGHT - ChatCycleHudRenderer.TAB_GAP,
                feature.getCurrentChannel(),
                feature.getPinnedChannel(),
                feature.getCurrentDmChannel(),
                feature.getPinnedDmChannel(),
                feature.getDmChannels(),
                feature.isInParty(), feature.isInTeamGame(),
                mouseX, mouseY
        );
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        ChatCycleFeature feature = ChatCycleFeature.getInstance();
        if (feature == null) return;
        feature.setOnChannelChanged(null);
        feature.resetChannel();
        plexmod$refreshDisplay();
    }

    @Unique
    private void plexmod$refreshDisplay() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.gui.hud.getChat().rescaleChat();
        }
    }
}
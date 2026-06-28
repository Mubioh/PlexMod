package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.chat.ChatChannel;
import me.mubioh.plexmod.feature.chatcycle.ChatCycleFeature;
import me.mubioh.plexmod.feature.chatcycle.ChatCycleHudRenderer;
import me.mubioh.plexmod.feature.chatcycle.DmChannel;
import me.mubioh.plexmod.feature.community.CommunityChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(value = GuiChat.class)
public class MixinGuiChat {

    @Shadow protected GuiTextField inputField;

    @Unique private long   plexmod$lastClickTime  = 0;
    @Unique private Object plexmod$lastClickedTab  = null;
    private static final long DOUBLE_CLICK_MS = 400;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ChatCycleFeature f = ChatCycleFeature.getInstance();
        if (f == null) return;
        plexmod$refreshChat();
        f.setOnChannelChanged(new Runnable() {
            public void run() { plexmod$refreshChat(); }
        });
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        ChatCycleFeature f = ChatCycleFeature.getInstance();
        if (f == null || inputField == null) return;

        if (keyCode == Keyboard.KEY_TAB && inputField.getText().trim().isEmpty()) {
            f.cycle();
            ci.cancel();
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            String text = inputField.getText();
            if (!text.isEmpty() && !text.startsWith("/")) {
                if (f.getCurrentDmChannel() != null) {
                    inputField.setText("/msg " + f.getCurrentDmChannel().getPlayerName() + " " + text);
                } else if (f.getCurrentChannel().prefix != null) {
                    inputField.setText(f.getCurrentChannel().prefix + text);
                }
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mx, int my, int button, CallbackInfo ci) {
        ChatCycleFeature f = ChatCycleFeature.getInstance();
        if (f == null || inputField == null) return;

        int tabY = inputField.yPosition - ChatCycleHudRenderer.TAB_HEIGHT - ChatCycleHudRenderer.TAB_GAP;
        int tabX = inputField.xPosition;

        ChatChannel fixedClicked = ChatCycleHudRenderer.getClickedFixedTab(
                mx, my, tabX, tabY,
                f.isInParty(), f.isInTeamGamePublic(), f.isInCommunity());
        if (fixedClicked != null) {
            long now = System.currentTimeMillis();
            boolean dbl = fixedClicked.equals(plexmod$lastClickedTab)
                    && (now - plexmod$lastClickTime) <= DOUBLE_CLICK_MS;
            plexmod$lastClickedTab = fixedClicked;
            plexmod$lastClickTime  = now;
            f.handleTabClick(fixedClicked, dbl);
            ci.cancel();
            return;
        }

        CommunityChannel commChannel = f.getCommunityChannel();
        DmChannel dmClicked = ChatCycleHudRenderer.getClickedDmTab(
                mx, my, tabX, tabY,
                f.isInParty(), f.isInTeamGamePublic(), f.isInCommunity(),
                commChannel, f.getDmChannels());
        if (dmClicked != null) {
            long now = System.currentTimeMillis();
            boolean dbl = dmClicked.equals(plexmod$lastClickedTab)
                    && (now - plexmod$lastClickTime) <= DOUBLE_CLICK_MS;
            plexmod$lastClickedTab = dmClicked;
            plexmod$lastClickTime  = now;
            f.handleDmTabClick(dmClicked, dbl);
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void onDrawScreen(int mx, int my, float partial, CallbackInfo ci) {
        ChatCycleFeature f = ChatCycleFeature.getInstance();
        if (f == null || inputField == null) return;

        int tabY = inputField.yPosition - ChatCycleHudRenderer.TAB_HEIGHT - ChatCycleHudRenderer.TAB_GAP;
        CommunityChannel commChannel = f.getCommunityChannel();
        ChatCycleHudRenderer.renderTabs(
                inputField.xPosition, tabY,
                f.getCurrentChannel(), f.getPinnedChannel(),
                f.getCurrentDmChannel(), f.getPinnedDmChannel(),
                commChannel, f.getDmChannels(),
                f.isInParty(), f.isInTeamGamePublic(), f.isInCommunity(),
                mx, my
        );
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void onClosed(CallbackInfo ci) {
        ChatCycleFeature f = ChatCycleFeature.getInstance();
        if (f == null) return;
        f.setOnChannelChanged(null);
        f.resetChannel();
        plexmod$refreshChat();
    }

    @Unique
    private void plexmod$refreshChat() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI == null) return;

        ChatCycleFeature f = ChatCycleFeature.getInstance();
        if (f == null) { mc.ingameGUI.getChatGUI().refreshChat(); return; }

        GuiNewChat chat = mc.ingameGUI.getChatGUI();

        List<String> sentHistory = new ArrayList<String>(chat.getSentMessages());

        chat.clearChatMessages();

        for (String msg : sentHistory) {
            chat.addToSentMessages(msg);
        }

        List<IChatComponent> history;
        if (f.getCurrentDmChannel() != null) {
            history = f.getCurrentDmChannel().getHistory();
        } else if (f.getCurrentChannel() == ChatChannel.COMMUNITY && f.getCommunityChannel() != null) {
            history = f.getCommunityChannel().getHistory();
        } else {
            history = f.getCurrentState().getHistory();
        }

        List<IChatComponent> reversed = new ArrayList<IChatComponent>(history);
        Collections.reverse(reversed);

        f.setRepopulating(true);
        for (IChatComponent msg : reversed) {
            chat.printChatMessage(msg);
        }
        f.setRepopulating(false);

        if (inputField != null) {
            inputField.setFocused(true);
            inputField.setCursorPositionEnd();
        }
    }
}

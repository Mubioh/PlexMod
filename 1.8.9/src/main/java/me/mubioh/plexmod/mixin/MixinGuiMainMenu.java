package me.mubioh.plexmod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiScreen {

    private static final int BTN_JOIN_MINEPLEX = 201;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        // Find the Multiplayer button (id = 1 in vanilla 1.8.9)
        GuiButton mpBtn = null;
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton && ((GuiButton) obj).id == 1) {
                mpBtn = (GuiButton) obj;
                break;
            }
        }
        if (mpBtn == null) return;

        // Shrink it to half-width and add a sibling "Join Mineplex" button
        int gap   = 4;
        int half  = (mpBtn.width - gap) / 2;
        int origX = mpBtn.xPosition;
        int origY = mpBtn.yPosition;

        mpBtn.width     = half;
        mpBtn.xPosition = origX;

        this.buttonList.add(new GuiButton(BTN_JOIN_MINEPLEX, origX + half + gap, origY, half, 20, "Join Mineplex"));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id != BTN_JOIN_MINEPLEX) return;
        plexmod$connect();
        ci.cancel();
    }

    @Unique
    private void plexmod$connect() {
        Minecraft mc = Minecraft.getMinecraft();
        ServerData data = new ServerData("Mineplex", "mineplex.com", false);
        net.minecraft.client.multiplayer.GuiConnecting connecting =
                new net.minecraft.client.multiplayer.GuiConnecting(this, mc, data);
        mc.displayGuiScreen(connecting);
    }
}

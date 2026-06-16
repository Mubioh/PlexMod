package me.mubioh.plexmod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.Nullable;

@Mixin(value = TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Shadow
    @Nullable
    protected abstract Component getMultiplayerDisabledReason();

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "createNormalMenuOptions", at = @At("HEAD"), cancellable = true)
    private void overrideCreateNormalMenuOptions(int topPos, int spacing, CallbackInfoReturnable<Integer> cir) {
        Component disabledReason = this.getMultiplayerDisabledReason();
        boolean multiplayerEnabled = disabledReason == null;
        Tooltip tooltip = disabledReason != null ? Tooltip.create(disabledReason) : null;

        this.addRenderableWidget(Button.builder(Component.translatable("menu.singleplayer"),
                btn -> this.minecraft.setScreen(new SelectWorldScreen(this))).bounds(this.width / 2 - 100, topPos, 200, 20).build());

        int gap       = 4;
        int halfWidth = (200 - gap) / 2;
        int xLeft     = this.width / 2 - 100;
        int xRight    = xLeft + halfWidth + gap;
        int rowY      = topPos + spacing;

        ((Button) this.addRenderableWidget(Button.builder(
                Component.translatable("menu.multiplayer"),
                btn -> {
                    Screen screen = this.minecraft.options.skipMultiplayerWarning
                            ? new JoinMultiplayerScreen(this)
                            : new SafetyScreen(this);
                    this.minecraft.setScreen(screen);
                }
        ).bounds(xLeft, rowY, halfWidth, 20).tooltip(tooltip).build())).active = multiplayerEnabled;

        ((Button) this.addRenderableWidget(Button.builder(
                Component.literal("Join Mineplex"),
                btn -> connect()
        ).bounds(xRight, rowY, halfWidth, 20).tooltip(tooltip).build())).active = multiplayerEnabled;

        int realmsY = rowY + spacing;
        ((Button) this.addRenderableWidget(Button.builder(
                Component.translatable("menu.online"),
                btn -> this.minecraft.setScreen(new com.mojang.realmsclient.RealmsMainScreen(this))
        ).bounds(this.width / 2 - 100, realmsY, 200, 20).tooltip(tooltip).build())).active = multiplayerEnabled;

        cir.setReturnValue(realmsY);
    }

    @Unique
    private void connect() {
        Minecraft mc = Minecraft.getInstance();
        ServerData data = new ServerData("Mineplex", "mineplex.com", ServerData.Type.OTHER);
        ConnectScreen.startConnecting(this, mc, ServerAddress.parseString("mineplex.com"), data, false, null);
    }
}
package me.mubioh.plexmod.mixin;

import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiPlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {

    // The @Accessor annotation tells Mixin to generate a public getter for the private field
    @Accessor("header")
    IChatComponent plexmod$getHeader();
}
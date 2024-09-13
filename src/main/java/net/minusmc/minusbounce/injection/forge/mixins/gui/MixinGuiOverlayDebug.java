package net.minusmc.minusbounce.injection.forge.mixins.gui;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.fmcpe.viaforge.ProtocolBase;
import net.minecraft.client.gui.GuiOverlayDebug;
import net.minusmc.minusbounce.utils.MinecraftInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug {

    @Inject(method = "getDebugInfoRight", at = @At(value = "TAIL"))
    public void drawVersion(CallbackInfoReturnable<List<String>> cir) {
        final ProtocolVersion version = ProtocolBase.getManager().targetVersion;

        cir.getReturnValue().add("");

        if (!MinecraftInstance.mc.isIntegratedServerRunning())
            cir.getReturnValue().add("Protocol: " + version.getName());
        else cir.getReturnValue().add("Protocol: 1.8.x");
    }
}

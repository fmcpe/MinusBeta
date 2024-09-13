/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.minusmc.minusbounce.injection.forge.mixins.render;

import net.fmcpe.viaforge.api.McUpdatesHandler;
import net.fmcpe.viaforge.api.ProtocolFixer;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import net.minusmc.minusbounce.MinusBounce;
import net.minusmc.minusbounce.features.module.modules.render.Glint;
import net.minusmc.minusbounce.utils.MinecraftInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({LayerArmorBase.class})
public class MixinLayerArmorBase {
    @ModifyArgs(method="renderGlint", slice=@Slice(from=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GlStateManager;disableLighting()V", ordinal=0)), at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V", ordinal=0), require=1, allow=1)
    private void renderGlint(Args args) {
        final Glint glint = MinusBounce.moduleManager.getModule(Glint.class);

        assert glint != null;
        if (glint.getState()) {
            int n = glint.getColor().getRGB();
            args.set(0, (Object) ((float) (n >> 16 & 0xFF) / 255.0f));
            args.set(1, (Object) ((float) (n >> 8 & 0xFF) / 255.0f));
            args.set(2, (Object) ((float) (n & 0xFF) / 255.0f));
            args.set(3, (Object) ((float) (n >> 24 & 0xFF) / 255.0f));
        }
    }

    @Inject(method = {"doRenderLayer"}, at = {@At("HEAD")}, cancellable = true)
    public void doRenderLayer(final EntityLivingBase entitylivingbaseIn, final float limbSwing, final float limbSwingAmount, final float partialTicks, final float ageInTicks, final float netHeadYaw, final float headPitch, final float scale, final CallbackInfo ci) {
        if (ProtocolFixer.newerThanOrEqualsTo1_13() && McUpdatesHandler.shouldAnimation() && entitylivingbaseIn == MinecraftInstance.mc.thePlayer)
            ci.cancel();
    }
}
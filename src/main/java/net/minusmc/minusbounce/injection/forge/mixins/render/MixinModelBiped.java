/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.injection.forge.mixins.render;

import net.fmcpe.viaforge.api.McUpdatesHandler;
import net.fmcpe.viaforge.api.ProtocolFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minusmc.minusbounce.utils.MinecraftInstance;
import net.minusmc.minusbounce.utils.RotationUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public class MixinModelBiped {

    @Shadow
    public ModelRenderer bipedRightArm;

    @Shadow
    public int heldItemRight;

    @Shadow
    public ModelRenderer bipedHead;

    @Shadow
    public ModelRenderer bipedLeftArm;

    @Shadow
    public boolean isSneak;

    @Shadow
    public ModelRenderer bipedHeadwear;

    @Inject(method = "setRotationAngles", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelBiped;swingProgress:F"))
    private void setRotationAngles(float p_setRotationAngles_1_, float p_setRotationAngles_2_, float p_setRotationAngles_3_, float p_setRotationAngles_4_, float p_setRotationAngles_5_, float p_setRotationAngles_6_, Entity p_setRotationAngles_7_, CallbackInfo callbackInfo) {
        if(heldItemRight == 3) {
            this.bipedRightArm.rotateAngleY = 0F;
        }

        if (p_setRotationAngles_7_ instanceof EntityPlayer && p_setRotationAngles_7_.equals(Minecraft.getMinecraft().thePlayer)) {
            if (RotationUtils.targetRotation != null) {
                this.bipedHead.rotateAngleX = RotationUtils.targetRotation.getPitch() / (180F / (float) Math.PI);
            }
        }

        if (ProtocolFixer.newerThanOrEqualsTo1_13() && McUpdatesHandler.shouldAnimation() && p_setRotationAngles_7_ instanceof EntityPlayer && p_setRotationAngles_7_.equals(MinecraftInstance.mc.thePlayer)) {
            GlStateManager.rotate(45.0F, 1F, 0.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.0F, -0.7F);

            float swing = MinecraftInstance.mc.thePlayer.limbSwing / 3;

            this.bipedHead.rotateAngleX = -0.95f;
            this.bipedHeadwear.rotateAngleX = -0.95f;
            this.bipedLeftArm.rotateAngleX = swing;
            this.bipedRightArm.rotateAngleX = swing;
            this.bipedLeftArm.rotateAngleY = swing;
            this.bipedRightArm.rotateAngleY = -swing;
            this.isSneak = false;
        }
    }
}
/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.injection.forge.mixins.render;

import com.google.common.base.Predicates;
import net.fmcpe.viaforge.api.AnimationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.*;
import net.minusmc.minusbounce.MinusBounce;
import net.minusmc.minusbounce.event.RayTraceRangeEvent;
import net.minusmc.minusbounce.event.Render3DEvent;
import net.minusmc.minusbounce.features.module.modules.client.NoHurtCam;
import net.minusmc.minusbounce.features.module.modules.combat.KillAura;
import net.minusmc.minusbounce.features.module.modules.render.CameraClip;
import net.minusmc.minusbounce.features.module.modules.render.TargetMark;
import net.minusmc.minusbounce.utils.MinecraftInstance;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow
    public abstract void loadShader(ResourceLocation resourceLocationIn);

    @Shadow
    public abstract void setupCameraTransform(float partialTicks, int pass);

    @Shadow
    private Entity pointedEntity;

    @Shadow
    private Minecraft mc;

    @Shadow
    public float thirdPersonDistanceTemp;

    @Shadow
    public float thirdPersonDistance;

    @Shadow
    private boolean cloudFog;

    @Unique
    private float height;

    @Unique
    private float previousHeight;

    @Inject(method = "renderStreamIndicator", at = @At("HEAD"), cancellable = true)
    private void cancelStreamIndicator(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderWorldPass", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/util/EnumWorldBlockLayer;TRANSLUCENT:Lnet/minecraft/util/EnumWorldBlockLayer;")), at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/EnumWorldBlockLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 0))
    private void enablePolygonOffset(CallbackInfo ci) {
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-0.325F, -0.325F);
    }

    @Inject(method = "renderWorldPass", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/util/EnumWorldBlockLayer;TRANSLUCENT:Lnet/minecraft/util/EnumWorldBlockLayer;")), at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/EnumWorldBlockLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 0, shift = At.Shift.AFTER))
    private void disablePolygonOffset(CallbackInfo ci) {
        GlStateManager.disablePolygonOffset();
    }

    @Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z", shift = At.Shift.BEFORE))
    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo callbackInfo) {
        MinusBounce.eventManager.callEvent(new Render3DEvent(partialTicks));
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void injectHurtCameraEffect(CallbackInfo callbackInfo) {
        if (Objects.requireNonNull(MinusBounce.moduleManager.getModule(NoHurtCam.class)).getState()) callbackInfo.cancel();
    }

    @Inject(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"), cancellable = true)
    private void cameraClip(float partialTicks, CallbackInfo callbackInfo) {
        if (Objects.requireNonNull(MinusBounce.moduleManager.getModule(CameraClip.class)).getState()) {
            callbackInfo.cancel();

            Entity entity = this.mc.getRenderViewEntity();
            float f = entity.getEyeHeight();

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping()) {
                f = (float) ((double) f + 1D);
                GlStateManager.translate(0F, 0.3F, 0.0F);

                if (!this.mc.gameSettings.debugCamEnable) {
                    BlockPos blockpos = new BlockPos(entity);
                    IBlockState iblockstate = this.mc.theWorld.getBlockState(blockpos);
                    net.minecraftforge.client.ForgeHooksClient.orientBedCamera(this.mc.theWorld, blockpos, iblockstate, entity);

                    GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                    GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
                }
            } else if (this.mc.gameSettings.thirdPersonView > 0) {
                double d3 = this.thirdPersonDistanceTemp + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * partialTicks;

                if (this.mc.gameSettings.debugCamEnable) {
                    GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
                } else {
                    float f1 = entity.rotationYaw;
                    float f2 = entity.rotationPitch;

                    if (this.mc.gameSettings.thirdPersonView == 2) f2 += 180.0F;

                    if (this.mc.gameSettings.thirdPersonView == 2) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

                    GlStateManager.rotate(entity.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(entity.rotationYaw - f1, 0.0F, 1.0F, 0.0F);
                    GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
                    GlStateManager.rotate(f1 - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(f2 - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
                }
            } else GlStateManager.translate(0.0F, 0.0F, -0.1F);

            if (!this.mc.gameSettings.debugCamEnable) {
                float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F;
                float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
                float roll = 0.0F;
                if (entity instanceof EntityAnimal) {
                    EntityAnimal entityanimal = (EntityAnimal) entity;
                    yaw = entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F;
                }

                Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, partialTicks);
                net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup event = new net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup((EntityRenderer) (Object) this, entity, block, partialTicks, yaw, pitch, roll);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                GlStateManager.rotate(event.roll, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(event.pitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(event.yaw, 0.0F, 1.0F, 0.0F);
            }

            GlStateManager.translate(0.0F, -f, 0.0F);
            double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
            double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
            double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;
            this.cloudFog = this.mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
        }
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = At.Shift.BEFORE))
    private void setupCameraViewBobbingBefore(final CallbackInfo callbackInfo) {
        final TargetMark targetMark = MinusBounce.moduleManager.getModule(TargetMark.class);
        final KillAura aura = MinusBounce.moduleManager.getModule(KillAura.class);

        if (targetMark != null && aura != null && targetMark.getModeValue().get().equalsIgnoreCase("tracers") && !aura.getTargetModeValue().get().equalsIgnoreCase("multi") && aura.getTarget() != null)
            GL11.glPushMatrix();
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = At.Shift.AFTER))
    private void setupCameraViewBobbingAfter(final CallbackInfo callbackInfo) {
        final TargetMark targetMark = MinusBounce.moduleManager.getModule(TargetMark.class);
        final KillAura aura = MinusBounce.moduleManager.getModule(KillAura.class);

        if (targetMark != null && aura != null && targetMark.getModeValue().get().equalsIgnoreCase("tracers") && !aura.getTargetModeValue().get().equalsIgnoreCase("multi") && aura.getTarget() != null)
            GL11.glPopMatrix();
    }

    /**
     * Finds what block or object the mouse is over at the specified partial tick time. Args: partialTickTime
     *
     * @author fmcpe
     * @reason .
     */
    @Overwrite
    public void getMouseOver(float partialTicks) {
        final Entity renderViewEntity = this.mc.getRenderViewEntity();
        if (renderViewEntity != null && this.mc.theWorld != null) {
            final RayTraceRangeEvent rayTraceRangeEvent = new RayTraceRangeEvent(this.mc.playerController.getBlockReachDistance(), 3.0f, 0f);
            MinusBounce.eventManager.callEvent(rayTraceRangeEvent);
            this.mc.mcProfiler.startSection("pick");
            this.mc.pointedEntity = null;
            final double n2 = rayTraceRangeEvent.getBlockReachDistance();
            this.mc.objectMouseOver = renderViewEntity.rayTrace(n2, partialTicks);
            double distanceTo = n2;
            final Vec3 positionEyes = renderViewEntity.getPositionEyes(partialTicks);
            boolean b = false;
            double n3;
            if (this.mc.playerController.extendedReach()) {
                n3 = 6.0;
                distanceTo = 6.0;
            } else {
                if (n2 > rayTraceRangeEvent.getRange()) {
                    b = true;
                }
                n3 = n2;
            }
            if (this.mc.objectMouseOver != null) {
                distanceTo = this.mc.objectMouseOver.hitVec.distanceTo(positionEyes);
            }
            final Vec3 look = renderViewEntity.getLook(partialTicks);
            final Vec3 addVector = positionEyes.addVector(look.xCoord * n3, look.yCoord * n3, look.zCoord * n3);
            this.pointedEntity = null;
            Vec3 vec3 = null;
            final float n4 = 1.0f;
            final List<Entity> entitiesInAABBexcluding = this.mc.theWorld.getEntitiesInAABBexcluding(renderViewEntity, renderViewEntity.getEntityBoundingBox().addCoord(look.xCoord * n3, look.yCoord * n3, look.zCoord * n3).expand(n4, n4, n4), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double n5 = distanceTo;
            for (Entity pointedEntityObj : entitiesInAABBexcluding) {
                if (pointedEntityObj.getName().contains("fake!")) {
                    continue;
                }
                final float collisionBorderSize = pointedEntityObj.getCollisionBorderSize();
                final AxisAlignedBB expand = pointedEntityObj.getEntityBoundingBox().expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
                final MovingObjectPosition calculateIntercept = expand.calculateIntercept(positionEyes, addVector);
                if (expand.isVecInside(positionEyes)) {
                    if (n5 < 0.0) {
                        continue;
                    }
                    this.pointedEntity = pointedEntityObj;
                    vec3 = ((calculateIntercept == null) ? positionEyes : calculateIntercept.hitVec);
                    n5 = 0.0;
                } else {
                    if (calculateIntercept == null) {
                        continue;
                    }
                    final double distanceTo2 = positionEyes.distanceTo(calculateIntercept.hitVec);
                    if (distanceTo2 >= n5 && n5 != 0.0) {
                        continue;
                    }
                    if (pointedEntityObj == renderViewEntity.ridingEntity && !pointedEntityObj.canRiderInteract()) {
                        if (n5 != 0.0) {
                            continue;
                        }
                        this.pointedEntity = pointedEntityObj;
                        vec3 = calculateIntercept.hitVec;
                    } else {
                        this.pointedEntity = pointedEntityObj;
                        vec3 = calculateIntercept.hitVec;
                        n5 = distanceTo2;
                    }
                }
            }
            if (this.pointedEntity != null && positionEyes.distanceTo(vec3) > 0.0) {
                rayTraceRangeEvent.setRayTraceRange((float) positionEyes.distanceTo(vec3));
            }
            if (this.pointedEntity != null && b && positionEyes.distanceTo(vec3) > rayTraceRangeEvent.getRange()) {
                this.pointedEntity = null;
                assert vec3 != null;
                this.mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec3, null, new BlockPos(vec3));
            }
            if (this.pointedEntity != null && (n5 < distanceTo || this.mc.objectMouseOver == null)) {
                this.mc.objectMouseOver = new MovingObjectPosition(this.pointedEntity, vec3);
                if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
                    this.mc.pointedEntity = this.pointedEntity;
                }
            }
            this.mc.mcProfiler.endSection();
        }
    }

    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEyeHeight()F"))
    public float modifyEyeHeight(Entity entity, float partialTicks) {
        return previousHeight + (height - previousHeight) * partialTicks;
    }

    @Inject(method = "updateRenderer", at = @At("HEAD"))
    private void interpolateHeight(CallbackInfo ci) {
        Entity entity = MinecraftInstance.mc.getRenderViewEntity();
        float eyeHeight = entity.getEyeHeight();
        previousHeight = height;
        if (eyeHeight < height)
            height = eyeHeight;
        else
            height += (eyeHeight - height) * 0.5f;
        AnimationUtils.setAnimatedEyeHeight(height);
    }
}
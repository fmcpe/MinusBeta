/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils

import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.injection.implementations.IEntityPlayerSP
import net.minusmc.minusbounce.injection.implementations.IEntityLivingBase

open class MinecraftInstance {
    companion object {
        @JvmField
        val mc: Minecraft = Minecraft.getMinecraft()

        var runTimeTicks = 0
        var AABBOffGroundticks = 0
        val serverRotation: Rotation
            get() = (mc.thePlayer as IEntityPlayerSP).serverRotation

        val motionX: Double
            get() = mc.thePlayer.motionX

        val motionZ: Double
            get() = mc.thePlayer.motionZ

        val bb: AxisAlignedBB
            get() = mc.thePlayer.entityBoundingBox

        val eyesPos: Vec3
            get() = mc.thePlayer.getPositionEyes(mc.timer.renderPartialTicks)

        var EntityLivingBase.realPosX: Double
            get() = (this as IEntityLivingBase).realPosX
            set(value: Double) {
                (this as IEntityLivingBase).realPosX = value
            }

        var EntityLivingBase.realPosY: Double
            get() = (this as IEntityLivingBase).realPosY
            set(value: Double) {
                (this as IEntityLivingBase).realPosY = value
            }

        var EntityLivingBase.realPosZ: Double
            get() = (this as IEntityLivingBase).realPosZ
            set(value: Double) {
                (this as IEntityLivingBase).realPosZ = value
            }
    }
}

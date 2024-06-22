/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils

import net.minecraft.client.Minecraft
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.injection.implementations.IEntityPlayerSP

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
    }
}

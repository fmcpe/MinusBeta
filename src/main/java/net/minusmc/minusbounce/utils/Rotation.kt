/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils

import net.minusmc.minusbounce.utils.block.PlaceInfo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.injection.implementations.IEntityPlayerSP
import kotlin.math.*

/**
 * Rotations
 */
data class Rotation(var yaw: Float, var pitch: Float) {

    /**
     * Set rotations to [player]
     */
    fun toPlayer(p: EntityPlayer) {
        if(yaw.isNaN() && pitch.isNaN() && pitch !in -90.0..90.0)
            return

        fixedSensitivity(MinecraftInstance.mc.gameSettings.mouseSensitivity)

        p.rotationYaw = yaw
        p.rotationPitch = pitch
    }

    /**
     * Patch gcd exploit in [Rotation]
     *
     */
    @JvmOverloads
    fun fixedSensitivity(
        s: Float,
        rotation : Rotation? = (MinecraftInstance.mc.thePlayer as IEntityPlayerSP).serverRotation
    ) {
        rotation?.let{
            val f = s * (1f + Math.random().toFloat() / 10000000f) * 0.6F + 0.2F
            val m = f * f * f * 8.0F * 0.15f
            yaw = it.yaw + ((yaw - it.yaw) / m).roundToInt() * m
            pitch = (it.pitch + ((pitch - it.pitch) / m).roundToInt() * m).coerceIn(-90F, 90F)
        }
    }

    /**
     * Convert [Rotation] into [Vec3]
     *
     */
    fun toDirection(): Vec3 {
        val f = cos(-yaw * 0.017453292 - Math.PI)
        val f1 = sin(-yaw * 0.017453292 - Math.PI)
        val f2 = -cos(-pitch * 0.017453292)
        val f3 = sin(-pitch * 0.017453292)
        return Vec3(f1 * f2, f3, f * f2)
    }

    override fun toString(): String {
        return "Rotation(yaw=$yaw, pitch=$pitch)"
    }
}

/**
 * Rotation with vector
 */
data class VecRotation(val vec: Vec3, val rotation: Rotation)

/**
 * Rotation with place info
 */
data class PlaceRotation(val placeInfo: PlaceInfo, val rotation: Rotation)

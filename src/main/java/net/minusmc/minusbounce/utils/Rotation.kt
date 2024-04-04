/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.utils.MinecraftInstance.Companion.mc
import net.minusmc.minusbounce.utils.block.PlaceInfo
import kotlin.math.*

/**
 * Rotations
 *
 */
data class Rotation(var yaw: Float, var pitch: Float) {

    /**
     * Set rotations to [player]
     */
    fun toPlayer(p: EntityPlayer) {
        if(isNan()){
            return
        }

        fixedSensitivity(mc.gameSettings.mouseSensitivity)

        p.rotationYaw = yaw
        p.rotationPitch = pitch
    }

    /**
     * Patch gcd exploit in [Rotation]
     *
     */
    @JvmOverloads
    fun fixedSensitivity(
        s: Float = mc.gameSettings.mouseSensitivity,
        rotation : Rotation? = MinecraftInstance.serverRotation
    ) {
        rotation?.let{
            val f = s * (1f + Math.random().toFloat() / 10000000f) * 0.6F + 0.2F
            val m = f * f * f * 8.0F * 0.15f
            yaw = it.yaw + round((yaw - it.yaw) / m) * m
            pitch = (it.pitch + round((pitch - it.pitch) / m) * m).coerceIn(-90F, 90F)
        }
    }

    /**
     * Nan checks
     */
    fun isNan() = yaw.isNaN() || pitch.isNaN() || pitch !in -90.0..90.0

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

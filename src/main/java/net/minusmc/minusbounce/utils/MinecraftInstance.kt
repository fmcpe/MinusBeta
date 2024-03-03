/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils

import net.minecraft.client.Minecraft
import net.minusmc.minusbounce.injection.implementations.IEntityPlayerSP

open class MinecraftInstance {
    companion object {
        @JvmField
        val mc: Minecraft = Minecraft.getMinecraft()

        val serverRotation: Rotation
            get() = (mc.thePlayer as IEntityPlayerSP).serverRotation

        val playerRotation: Rotation?
            get() = (mc.thePlayer as IEntityPlayerSP).playerRotation
    }
}

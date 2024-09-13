package net.fmcpe.viaforge.api

import net.fmcpe.viaforge.api.AnimationUtils.animate
import net.minecraft.block.material.Material
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.item.EntityBoat
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.utils.MinecraftInstance

class McUpdatesHandler : MinecraftInstance(), Listenable {
    @EventTarget
    fun onPushOut(event: PushOutEvent) {
        if (ProtocolFixer.newerThanOrEqualsTo1_13() && (shouldAnimation() || mc.thePlayer.isSneaking)) event.cancelEvent()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (ProtocolFixer.newerThanOrEqualsTo1_13()) {
            val START_HEIGHT = 1.62f

            lastEyeHeight = eyeHeight

            val END_HEIGHT = 0.45f
            val delta = 0.085f

            if (shouldAnimation()) {
                eyeHeight = animate(END_HEIGHT, eyeHeight, 4 * delta)
                doingEyeRot = true
            } else if (eyeHeight < START_HEIGHT) eyeHeight = animate(START_HEIGHT, eyeHeight, 4 * delta)

            if (eyeHeight >= START_HEIGHT && doingEyeRot) doingEyeRot = false
        }
    }

    @EventTarget(priority = 5)
    fun onUpdate(event: UpdateEvent) {
        if (ProtocolFixer.newerThanOrEqualsTo1_13()) {
            if (isSwimming) {
                if (mc.thePlayer.motionX < -0.4) {
                    mc.thePlayer.motionX = -0.39
                }
                if (mc.thePlayer.motionX > 0.4) {
                    mc.thePlayer.motionX = 0.39
                }
                if (mc.thePlayer.motionY < -0.4) {
                    mc.thePlayer.motionY = -0.39
                }
                if (mc.thePlayer.motionY > 0.4) {
                    mc.thePlayer.motionY = 0.39
                }
                if (mc.thePlayer.motionZ < -0.4) {
                    mc.thePlayer.motionZ = -0.39
                }
                if (mc.thePlayer.motionZ > 0.4) {
                    mc.thePlayer.motionZ = 0.39
                }

                val d3 = mc.thePlayer.lookVec.yCoord
                val d4 = 0.025

                if (d3 <= 0.0 || mc.thePlayer.worldObj.getBlockState(
                        BlockPos(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY + 1.0 - 0.64,
                            mc.thePlayer.posZ
                        )
                    ).block.material === Material.water
                ) {
                    mc.thePlayer.motionY += (d3 - mc.thePlayer.motionY) * d4
                }

                mc.thePlayer.motionY += 0.018

                if (shouldAnimation()) {
                    mc.thePlayer.motionX *= 1.09
                    mc.thePlayer.motionZ *= 1.09
                }
            }
        }

        val sneakLength = if (ProtocolFixer.newerThanOrEqualsTo1_9() && ProtocolFixer.olderThanOrEqualsTo1_13_2()) 1.65f
        else if (ProtocolFixer.newerThanOrEqualsTo1_14()) 1.5f
        else 1.8f

        val d0 = mc.thePlayer.width / 2.0
        val box = mc.thePlayer.entityBoundingBox
        val setThrough = AxisAlignedBB(
            mc.thePlayer.posX - d0,
            box.minY,
            mc.thePlayer.posZ - d0,
            mc.thePlayer.posX + d0,
            box.minY + mc.thePlayer.height,
            mc.thePlayer.posZ + d0
        )
        val sneak = AxisAlignedBB(box.minX, box.minY + 0.9, box.minZ, box.minX + 0.6, box.minY + 1.8, box.minZ + 0.6)
        val crawl = AxisAlignedBB(box.minX, box.minY + 0.9, box.minZ, box.minX + 0.6, box.minY + 1.5, box.minZ + 0.6)

        val newHeight: Float
        val newWidth: Float

        if (ProtocolFixer.newerThanOrEqualsTo1_13() && isSwimmingOrCrawling && underWater() && mc.thePlayer.rotationPitch >= 0.0) {
            newHeight = 0.6f
            newWidth = 0.6f
            isSwimmingOrCrawling = true
            mc.thePlayer.entityBoundingBox = setThrough
        } else if (ProtocolFixer.newerThanOrEqualsTo1_13() && (isSwimming && underWater() || mc.theWorld.getCollisionBoxes(
                crawl
            ).isNotEmpty())
        ) {
            newHeight = 0.6f
            newWidth = 0.6f
            isSwimmingOrCrawling = true
            mc.thePlayer.entityBoundingBox = setThrough
        } else if (mc.thePlayer.isSneaking && !underWater()) {
            newHeight = sneakLength
            newWidth = 0.6f
            mc.thePlayer.entityBoundingBox = setThrough
        } else {
            if (isSwimmingOrCrawling) isSwimmingOrCrawling = false
            newHeight = 1.8f
            newWidth = 0.6f
            mc.thePlayer.entityBoundingBox = setThrough
        }

        if (ProtocolFixer.newerThanOrEqualsTo1_9() && mc.thePlayer.onGround && !mc.thePlayer.isSneaking && !underWater() && (mc.thePlayer.height == sneakLength || mc.thePlayer.height == 0.6f) && !mc.theWorld.getCollisionBoxes(
                sneak
            ).isEmpty()
        ) {
            mc.gameSettings.keyBindSneak.pressed = true
        } else if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && mc.theWorld.getCollisionBoxes(sneak)
                .isEmpty()
        ) {
            mc.gameSettings.keyBindSneak.pressed = false
        }

        try {
            mc.thePlayer.height = newHeight
            mc.thePlayer.width = newWidth
        } catch (ignored: IllegalArgumentException) {
        }
    }

    override fun handleEvents(): Boolean {
        return true
    }

    companion object {
        var isSwimmingOrCrawling: Boolean = false
        @JvmField
        var doingEyeRot: Boolean = false
        @JvmField
        var eyeHeight: Float = 0f
        @JvmField
        var lastEyeHeight: Float = 0f

        private fun underWater(): Boolean {
            val world = mc.thePlayer.entityWorld
            val eyeBlock = mc.thePlayer.posY + mc.thePlayer.getEyeHeight().toDouble() - 0.25
            val blockPos = BlockPos(mc.thePlayer.posX, eyeBlock, mc.thePlayer.posZ)

            return world.getBlockState(blockPos).block.material === Material.water && mc.thePlayer.ridingEntity !is EntityBoat
        }

        private val isSwimming: Boolean
            get() = !mc.thePlayer.noClip && mc.thePlayer.isInWater && mc.thePlayer.isSprinting

        @JvmStatic
        fun shouldAnimation(): Boolean {
            val box = mc.thePlayer.entityBoundingBox
            val crawl =
                AxisAlignedBB(box.minX, box.minY + 0.9, box.minZ, box.minX + 0.6, box.minY + 1.5, box.minZ + 0.6)

            return !mc.thePlayer.noClip && (isSwimmingOrCrawling && mc.thePlayer.isSprinting && mc.thePlayer.isInWater || isSwimmingOrCrawling && mc.theWorld.getCollisionBoxes(
                crawl
            ).isNotEmpty())
        }
    }
}
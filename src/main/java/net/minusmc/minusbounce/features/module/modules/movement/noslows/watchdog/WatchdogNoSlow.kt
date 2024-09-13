package net.minusmc.minusbounce.features.module.modules.movement.noslows.watchdog

import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.PostMotionEvent
import net.minusmc.minusbounce.features.module.modules.movement.noslows.NoSlowMode
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.Rotation
import net.minusmc.minusbounce.utils.RotationUtils
import net.minusmc.minusbounce.utils.extensions.rayTraceCustom

class WatchdogNoSlow: NoSlowMode("Watchdog") {
    override fun onPostMotion(event: PostMotionEvent) {
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(mc.thePlayer.inventory.currentItem + 36).stack))
    }
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C08PacketPlayerBlockPlacement) {
            if (noslow.isEating) {
                if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit === MovingObjectPosition.MovingObjectType.BLOCK && packet.position != BlockPos(
                        -1,
                        -1,
                        1
                    )
                ) return
                event.cancelEvent()
                val position: MovingObjectPosition = mc.thePlayer.rayTraceCustom(
                    mc.playerController.blockReachDistance, 1F, mc.thePlayer.rotationYaw, 90f)
                    ?: return
                val rot = Rotation(mc.thePlayer.rotationYaw, 90f)
                RotationUtils.setRotations(rot)
                sendUseItem(position)
            }
        }
    }

    private fun sendUseItem(mouse: MovingObjectPosition) {
        val facingX = (mouse.hitVec.xCoord - mouse.blockPos.x.toDouble()).toFloat()
        val facingY = (mouse.hitVec.yCoord - mouse.blockPos.y.toDouble()).toFloat()
        val facingZ = (mouse.hitVec.zCoord - mouse.blockPos.z.toDouble()).toFloat()
        PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(mouse.blockPos, mouse.sideHit.index, mc.thePlayer.heldItem, facingX, facingY, facingZ))
    }
}
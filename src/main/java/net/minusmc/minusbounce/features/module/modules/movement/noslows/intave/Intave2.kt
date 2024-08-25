package net.minusmc.minusbounce.features.module.modules.movement.noslows.intave

import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.PreMotionEvent
import net.minusmc.minusbounce.features.module.modules.movement.noslows.NoSlowMode
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.utils.PacketUtils


class Intave2: NoSlowMode("Intave2") {
    private var lastUsingItem = false

    override fun onPreMotion(event: PreMotionEvent) {
        if(MovementUtils.isMoving){
            if(noslow.isEating) {
                if (mc.thePlayer.itemInUseDuration == 1 || mc.thePlayer.itemInUseDuration == 32) {
                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos(mc.thePlayer),
                            EnumFacing.UP
                        )
                    )
                }
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        if(noslow.isBlocking){
            when(val packet = event.packet){
                is C08PacketPlayerBlockPlacement -> {
                    if(mc.thePlayer.isUsingItem && MovementUtils.isMoving){
                        PacketUtils.sendPacket(packet, false)
                    }
                }

                is C07PacketPlayerDigging -> {
                    PacketUtils.sendPacket(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos(mc.thePlayer),
                            mc.thePlayer.horizontalFacing.opposite
                        ),
                        false
                    )
                }
            }
        }
    }

    override fun onDisable() {
        lastUsingItem = false
    }
}
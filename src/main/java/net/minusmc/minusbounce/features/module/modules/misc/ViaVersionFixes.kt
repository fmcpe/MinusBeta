package net.minusmc.minusbounce.features.module.modules.misc

import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.HitBoxEvent
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.PreMotionEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.utils.PacketUtils.sendPacket
import net.minusmc.minusbounce.utils.PacketUtils.sendPacketNoEvent
import net.minusmc.minusbounce.value.BoolValue


@ModuleInfo(name = "ViaVersionFixes", description = "ViaVersion Fix", category = ModuleCategory.MISC)
class ViaVersionFixes : Module() {
    private var fastBreak = BoolValue("FastBreak", true)
    private var viaC0B = BoolValue("ViaC0B", true)
    private var fabricatedPlace = BoolValue("FabricatedPlace", true)
    private var swing: Boolean = false

    @EventTarget(priority = -5)
    fun onMotion(event: PreMotionEvent) {
        if (viaC0B.get()) {
            if (mc.thePlayer.movementInput.sneak) {
                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
            } else {
                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
            }
        }
    }

    @EventTarget(priority = -5)
    fun onSendPacket(event: PacketEvent) {
        val packet = event.packet

        if (fastBreak.get()) {
            if (packet is C07PacketPlayerDigging) {
                if (packet.status == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                    sendPacket(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,
                            packet.position.add(0, 500, 0),
                            packet.facing
                        )
                    )
                }
            }
        }

        if (fabricatedPlace.get()) {
            if (packet is C08PacketPlayerBlockPlacement) {
                packet.facingX = 0.5f
                packet.facingY = 0.5f
                packet.facingZ = 0.5f
            }
        }

        if (packet is C07PacketPlayerDigging) {
            if (packet.status == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                if (packet.facing != EnumFacing.DOWN || packet.position.x != 0 || packet.position.y != 0 || packet.position.z != 0) {
                    event.cancelEvent()
                    sendPacketNoEvent(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                        )
                    )
                }
            }
        }

        if (packet is C06PacketPlayerPosLook) {
            if (packet.pitch > 90 || packet.pitch < -90) {
                packet.pitch = MathHelper.clamp_float(packet.pitch, -90f, 90f)
            }
        }
        if (packet is C02PacketUseEntity) {
            if (packet.action == C02PacketUseEntity.Action.ATTACK) {
                if (swing) swing = false
                else sendPacketNoEvent(C0APacketAnimation())
            }
        }
        if (packet is C0APacketAnimation) swing = true
    }

    @EventTarget(priority = -5)
    fun onHitBox(e: HitBoxEvent) {
        e.size = 0.0F
    }
}
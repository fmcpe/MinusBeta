/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.combat

import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.AttackEvent
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.UpdateEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.IntegerValue
import net.minusmc.minusbounce.value.ListValue

@ModuleInfo(name = "SuperKnockback", spacedName = "Super Knockback", description = "Increases knockback dealt to other entities.", category = ModuleCategory.COMBAT)
class SuperKnockback : Module() {
    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
    private val modeValue = ListValue("Mode", arrayOf("ExtraPacket", "Legit", "Silent", "Packet"), "ExtraPacket")
    private val delay = IntegerValue("Delay", 0, 0, 500, "ms")
    var forward: Float = 0.0F

    val timer = MSTimer()

    private var ticks = 0

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) {
            val backtrack = MinusBounce.moduleManager.getModule(BackTrack::class.java) ?: return
            if (event.targetEntity.hurtTime >= hurtTimeValue.get() || !timer.hasTimePassed(delay.get().toLong()) || (backtrack.state && backtrack.packets.isNotEmpty()) || !MovementUtils.isMoving || !mc.thePlayer.onGround)
                return

            when (modeValue.get().lowercase()) {
                "extrapacket" -> {
                    if (mc.thePlayer.isSprinting)
                        mc.thePlayer.isSprinting = true
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                    mc.thePlayer.serverSprintState = true
                }
                "silent" -> ticks = 1
                "legit" -> mc.thePlayer.reSprint = 2
                "packet" -> {
                    if(mc.thePlayer.isSprinting)
                        mc.thePlayer.isSprinting = true
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                    mc.thePlayer.serverSprintState = true
                }
            }
            timer.reset()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        when (modeValue.get().lowercase()) {
            "slient" -> if (ticks == 1) {
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                ticks = 2
            } else if (ticks == 2) {
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                ticks = 0
            }
        }
    }

    override fun onDisable() {
        mc.gameSettings.keyBindSprint.pressed = false
    }

    override val tag: String
        get() = modeValue.get()
}

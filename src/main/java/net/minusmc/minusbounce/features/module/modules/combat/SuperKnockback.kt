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
import net.minusmc.minusbounce.features.module.modules.movement.Sprint
import net.minusmc.minusbounce.features.module.modules.render.FreeCam
import net.minusmc.minusbounce.utils.ClientUtils
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.utils.timer.TimeUtils
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.IntegerValue
import net.minusmc.minusbounce.value.ListValue

@ModuleInfo(name = "SuperKnockback", spacedName = "Super Knockback", description = "Increases knockback dealt to other entities.", category = ModuleCategory.COMBAT)
class SuperKnockback : Module() {
    private val modeValue = ListValue("Mode", arrayOf("ExtraPacket", "Packet", "W-Tap", "Legit", "LegitFast"), "ExtraPacket")
    private val maxDelay: IntegerValue = object : IntegerValue("Legit-MaxDelay", 60, 0, 100, { modeValue.get() == "Legit" }) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minDelay.get()
            if (i > newValue) set(i)

            delay = TimeUtils.randomDelay(minDelay.get(), this.get())
        }
    }
    private val minDelay: IntegerValue = object : IntegerValue("Legit-MinDelay", 50, 0, 100, { modeValue.get() == "Legit" }) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxDelay.get()
            if (i < newValue) set(i)

            delay = TimeUtils.randomDelay(this.get(), maxDelay.get())
        }
    }
    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
    private val delayValue = IntegerValue("Delay", 0, 0, 500)
    private val onlyMoveValue = BoolValue("OnlyMove", true)
    private val onlyGroundValue = BoolValue("OnlyGround", false)

    private val timer = MSTimer()
    var delay = 0L
    var cancelSprint = false
    private val stopTimer = MSTimer()
    private var isHit = false
    private val attackTimer = MSTimer()

    override fun onEnable() {
        isHit = false
        if (modeValue.get() == "Legit" && MinusBounce.moduleManager.getModule(Sprint::class.java)?.state == false) {
            ClientUtils.displayChatMessage("§cError: You must turn on sprint to use the legit mode SuperKnockBack.")
            this.state = false
        }
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) {
            val player = mc.thePlayer ?: return
            if (event.targetEntity.hurtTime > hurtTimeValue.get()
                || !timer.hasTimePassed(delayValue.get().toLong())
                || (!MovementUtils.isMoving && onlyMoveValue.get())
                || (!mc.thePlayer.onGround && onlyGroundValue.get())
                || MinusBounce.moduleManager.getModule(FreeCam::class.java)?.state == true)
                return

            when (modeValue.get()) {
                "ExtraPacket" -> {
                    if (player.isSprinting) mc.netHandler.addToSendQueue(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING))
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING))
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING))
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING))
                    //player.isSprinting = true
                    player.serverSprintState = true
                }
                "Packet" -> {
                    if (player.isSprinting) mc.netHandler.addToSendQueue(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING))
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING))
                    player.serverSprintState = true
                }
                "W-Tap" -> {
                    if (mc.thePlayer.isSprinting) {
                        mc.thePlayer.isSprinting = false
                    }
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                    mc.thePlayer.serverSprintState = true
                }
                "Legit", "LegitFast" -> {
                    if (!isHit) {
                        isHit = true
                        attackTimer.reset()
                        delay = TimeUtils.randomDelay(minDelay.get(), maxDelay.get())
                    }
                }
            }
            timer.reset()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if  (modeValue.get() == "LegitFast") {
            if (isHit && stopTimer.hasTimePassed(80)) {
                isHit = false
                cancelSprint = true
                stopTimer.reset()
            }
        }
        if (modeValue.get() == "Legit") {
            if (isHit && attackTimer.hasTimePassed(delay / 2)) {
                isHit = false
                cancelSprint
                stopTimer.reset()
            }
            if (MinusBounce.moduleManager.getModule(Sprint::class.java)?.state == false) {
                ClientUtils.displayChatMessage("§cError: You must turn on sprint to use the legit mode SuperKnockBack.")
                this.state = false
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}

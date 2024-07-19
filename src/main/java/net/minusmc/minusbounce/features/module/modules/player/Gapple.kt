/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.player

import net.minecraft.init.Items
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.UpdateEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.ui.client.hud.element.elements.Notification
import net.minusmc.minusbounce.utils.InventoryUtils
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.FloatValue
import net.minusmc.minusbounce.value.IntegerValue
import net.minusmc.minusbounce.value.ListValue

@ModuleInfo(name = "Gapple", description = "Eat Gapples.", category = ModuleCategory.PLAYER)
class Gapple : Module() {
    val modeValue = ListValue("Mode", arrayOf("Auto", "Once", "Head"), "Once")
    // Auto Mode
    private val healthValue = FloatValue("Health", 10F, 1F, 20F)
    private val delayValue = IntegerValue("Delay", 150, 0, 1000, "ms")
    private val noAbsorption = BoolValue("NoAbsorption", true)
    private val grim = BoolValue("Grim", true)
    private val matrixInvalidSlotFixer = BoolValue("MatrixInvalidSlotFixer", true)
    private val timer = MSTimer()

    private var prevSlot = -1

    override fun onEnable() {
        prevSlot = -1
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        when(modeValue.get().lowercase()){
            "once" -> {
                doEat(true)
                state = false
            }
            "auto" -> {
                if (!timer.hasTimePassed(delayValue.get().toLong()))
                    return
                if (mc.thePlayer.health <= healthValue.get()){
                    doEat(false)
                    timer.reset()
                }
            }
            "head" -> {
                if (!timer.hasTimePassed(delayValue.get().toLong()))
                    return
                if (mc.thePlayer.health <= healthValue.get()){
                    val headInHotbar = InventoryUtils.findItem(36, 45, Items.skull) ?: return
                    if(headInHotbar != -1) {
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(headInHotbar - 36))
                        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                        timer.reset()
                    }
                }
            }
        }
    }

    @EventTarget(priority = 1)
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (!mc.isSingleplayer && matrixInvalidSlotFixer.get() && packet is C09PacketHeldItemChange) {
            if (packet.slotId == prevSlot)
                event.cancelEvent()
            else
                prevSlot = packet.slotId
        }
    }

    private fun doEat(warn: Boolean) {
        if (noAbsorption.get() && !warn) {
            val abAmount = mc.thePlayer.absorptionAmount
            if (abAmount > 0)
                return
        }
        var gappleInHotbar: Int? = null
        try {
            gappleInHotbar = InventoryUtils.findItem(36, 45, Items.golden_apple) ?: return
        } catch (_: Exception) {}

        if (gappleInHotbar != -1) {
            if (gappleInHotbar != null) {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(gappleInHotbar - 36))
            }
            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
            repeat(35) {
                if (grim.get()) 
                    PacketUtils.sendPacketNoEvent(C06PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround))
                else
                    mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))

            }
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        }else if (warn)
            MinusBounce.hud.addNotification(Notification("No Gapple were found in hotbar.", Notification.Type.ERROR))
    }

    override val tag: String
        get() = modeValue.get()
}

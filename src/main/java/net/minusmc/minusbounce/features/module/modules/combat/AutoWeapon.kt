/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.combat

import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.UpdateEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.utils.item.ItemUtils
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.IntegerValue

@ModuleInfo(name = "AutoWeapon", spacedName = "Auto Weapon", description = "Automatically selects the best weapon in your hotbar.", category = ModuleCategory.COMBAT)
class AutoWeapon : Module() {

    private val silentValue = BoolValue("SpoofItem", false)
    private val ticksValue = IntegerValue("SpoofTicks", 10, 1, 20)
    private val spam = BoolValue("SpamSwitching", false)

    private var spoofedSlot = 0

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val attack = packet is C02PacketUseEntity && packet.action == ATTACK
        val block = packet is C08PacketPlayerBlockPlacement

        if (attack || block) {
            // Find the best weapon in hotbar (#Kotlin Style)
            val (slot, _) = (0..8)
                    .map { Pair(it, mc.thePlayer.inventory.getStackInSlot(it)) }
                    .filter { it.second != null && (it.second.item is ItemSword || it.second.item is ItemTool) }
                    .maxByOrNull {
                        (it.second.attributeModifiers["generic.attackDamage"].first()?.amount
                                ?: 0.0) + 1.25 * ItemUtils.getEnchantment(it.second, Enchantment.sharpness)
                    } ?: return

            if (slot == mc.thePlayer.inventory.currentItem || !spam.get())
                return

            if (silentValue.get()) {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot))
                spoofedSlot = ticksValue.get()
            } else {
                mc.thePlayer.inventory.currentItem = slot
                mc.playerController.updateController()
            }

            mc.netHandler.addToSendQueue(packet)
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Switch back to old item after some time
        if (spoofedSlot > 0) {
            if (spoofedSlot == 1)
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            spoofedSlot--
        }
    }
}

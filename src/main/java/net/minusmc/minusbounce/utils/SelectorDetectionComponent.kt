/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils

import net.minecraft.entity.EntityList
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.Listenable
import net.minusmc.minusbounce.event.PreUpdateEvent


object SelectorDetectionComponent : MinecraftInstance(), Listenable {
    override fun handleEvents() = true

    private var selector = false

    fun selector(): Boolean {
        return selector
    }

    fun selector(itemStack: ItemStack?): Boolean {
        return if (itemStack == null) {
            false
        } else if (itemStack == mc.thePlayer.inventory.itemStack) {
            selector()
        } else {
            !trueName(itemStack).contains(itemStack.displayName)
        }
    }

    fun selector(itemSlot: Int): Boolean {
        return selector(mc.thePlayer.inventory.getStackInSlot(itemSlot))
    }

    @EventTarget
    fun onUpdate(event: PreUpdateEvent){
        if (mc.thePlayer.inventory.itemStack != null) {
            val itemStack: ItemStack = mc.thePlayer.inventory.itemStack

            selector = !trueName(itemStack).contains(itemStack.displayName)
        } else {
            selector = false
        }
    }

    fun trueName(itemStack: ItemStack): String {
        var name = ("" + StatCollector.translateToLocal(itemStack.unlocalizedName + ".name")).trim { it <= ' ' }
        val s1 = EntityList.getStringFromID(itemStack.metadata)

        if (s1 != null) {
            name = name + " " + StatCollector.translateToLocal("entity.$s1.name")
        }

        return name
    }
}
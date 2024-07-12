package net.minusmc.minusbounce.utils

import net.minecraft.block.BlockIce
import net.minecraft.block.BlockPackedIce
import net.minecraft.block.BlockSlime
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemEnderPearl
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minusmc.minusbounce.utils.MinecraftInstance.Companion.mc

object PlayerUtils {
	fun getSlimeSlot(): Int {
        for(i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item != null) {
            	if (stack.item is ItemBlock) {
            		val item = stack.item as ItemBlock
	            	if (item.getBlock() is BlockSlime) return i - 36
            	}
            }
        }
        return -1
    }

    fun getPearlSlot(): Int {
        for(i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item is ItemEnderPearl) return i - 36
        }
        return -1
    }

    fun isHealPotion(stack: ItemStack): Boolean {
        val itempotion = ItemPotion()
        val effects = itempotion.getEffects(stack)
        for (effect in effects) {
            if (effect.effectName == "potion.heal") return true
        }
        return false
    }

    fun getHealPotion(): Int {
        for (i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if(stack != null && stack.item is ItemPotion && isHealPotion(stack)) return i - 36
        }
        return -1
    }

    val isOnEdge: Boolean
        get() = mc.thePlayer.onGround && !mc.thePlayer.isSneaking && !mc.gameSettings.keyBindSneak.isKeyDown && !mc.gameSettings.keyBindJump.isKeyDown && mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.entityBoundingBox.offset(0.0, -0.5, 0.0).expand(-0.001, 0.0, -0.001)).isEmpty()

    val isOnIce: Boolean
        get() {
            val player = mc.thePlayer
            val blockUnder = mc.theWorld.getBlockState(BlockPos(player.posX, player.posY - 1.0, player.posZ)).block
            return blockUnder is BlockIce || blockUnder is BlockPackedIce
        }

    val isBlockUnder: Boolean
        get() {
            if (mc.thePlayer == null) return false
            if (mc.thePlayer.posY < 0.0) {
                return false
            }
            var off = 0
            while (off < mc.thePlayer.posY.toInt() + 2) {
                val bb = mc.thePlayer.entityBoundingBox.offset(0.0, -off.toDouble(), 0.0)
                if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isNotEmpty()) {
                    return true
                }
                off += 2
            }
            return false
        }
}
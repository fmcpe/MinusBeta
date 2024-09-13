/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils.item

import net.minecraft.block.*
import net.minecraft.enchantment.Enchantment
import net.minecraft.init.Items
import net.minecraft.item.*
import net.minecraft.nbt.JsonToNBT
import net.minecraft.util.ResourceLocation
import net.minusmc.minusbounce.utils.PlayerUtils
import java.util.regex.Pattern
import kotlin.math.min


/**
 * @author MCModding4K
 */
object ItemUtils {
    private val WHITELISTED_ITEMS = listOf(
        Items.fishing_rod,
        Items.water_bucket,
        Items.bucket,
        Items.arrow,
        Items.bow,
        Items.snowball,
        Items.egg,
        Items.ender_pearl
    )

    fun useful(stack: ItemStack): Boolean {
        val item = stack.item

        if (item is ItemPotion) {
            return ItemPotion.isSplash(stack.metadata) && PlayerUtils.goodPotion(item.getEffects(stack)[0].potionID)
        }

        if (item is ItemBlock) {
            val block: Block = item.getBlock()
            if (block is BlockGlass || block is BlockStainedGlass || (block.isFullBlock && !(block is BlockTNT || block is BlockSlime || block is BlockFalling))) {
                return true
            }
        }

        return item is ItemSword ||
                item is ItemTool ||
                item is ItemArmor ||
                item is ItemFood ||
                WHITELISTED_ITEMS.contains(item)
    }

    /**
     * Allows you to create an item using the item json
     *
     * @param itemArguments arguments of item
     * @return created item
     * @author MCModding4K
     */
    fun createItem(itemArguments: String): ItemStack? {
        var itemArguments = itemArguments
        return try {
            itemArguments = itemArguments.replace('&', '§')
            var item: Item? = Item()
            var args: Array<String>? = null
            var i = 1
            var j = 0
            for (mode in 0..min(12.0, (itemArguments.length - 2).toDouble()).toInt()) {
                args = itemArguments.substring(mode).split(Pattern.quote(" ").toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val resourcelocation = ResourceLocation(args[0])
                item = Item.itemRegistry.getObject(resourcelocation)
                if (item != null) break
            }
            if (item == null) return null
            if (args!!.size >= 2 && args[1].matches("\\d+".toRegex())) i = args[1].toInt()
            if (args.size >= 3 && args[2].matches("\\d+".toRegex())) j = args[2].toInt()
            val itemstack = ItemStack(item, i, j)
            if (args.size >= 4) {
                val NBT = StringBuilder()
                for (nbtcount in 3 until args.size) NBT.append(" ").append(args[nbtcount])
                itemstack.tagCompound = JsonToNBT.getTagFromJson(NBT.toString())
            }
            itemstack
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }

    fun getEnchantment(itemStack: ItemStack?, enchantment: Enchantment): Int {
        if (itemStack == null || itemStack.enchantmentTagList == null || itemStack.enchantmentTagList.hasNoTags()) return 0
        for (i in 0 until itemStack.enchantmentTagList.tagCount()) {
            val tagCompound = itemStack.enchantmentTagList.getCompoundTagAt(i)
            if (tagCompound.hasKey("ench") && tagCompound.getShort("ench")
                    .toInt() == enchantment.effectId || tagCompound.hasKey("id") && tagCompound.getShort("id")
                    .toInt() == enchantment.effectId
            ) return tagCompound.getShort("lvl")
                .toInt()
        }
        return 0
    }

    fun getEnchantmentCount(itemStack: ItemStack?): Int {
        if (itemStack == null || itemStack.enchantmentTagList == null || itemStack.enchantmentTagList.hasNoTags()) return 0
        var c = 0
        for (i in 0 until itemStack.enchantmentTagList.tagCount()) {
            val tagCompound = itemStack.enchantmentTagList.getCompoundTagAt(i)
            if (tagCompound.hasKey("ench") || tagCompound.hasKey("id")) c++
        }
        return c
    }

    fun getItemDurability(stack: ItemStack?): Int {
        return if (stack == null) 0 else stack.maxDamage - stack.itemDamage
    }
}

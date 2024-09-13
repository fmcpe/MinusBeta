/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.player

import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.*
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.util.DamageSource
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.AttackEvent
import net.minusmc.minusbounce.event.EventTarget
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.event.PreMotionEvent
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.movement.InvMove
import net.minusmc.minusbounce.features.module.modules.world.Scaffold
import net.minusmc.minusbounce.utils.PacketUtils
import net.minusmc.minusbounce.utils.PlayerUtils
import net.minusmc.minusbounce.utils.SelectorDetectionComponent
import net.minusmc.minusbounce.utils.click.MathUtil
import net.minusmc.minusbounce.utils.item.ItemUtils
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.BoolValue
import net.minusmc.minusbounce.value.FloatRangeValue
import net.minusmc.minusbounce.value.IntegerValue


@Suppress("UNUSED_PARAMETER")
@ModuleInfo(
    name = "Manager",
    spacedName = "Manager",
    description = "Automatically throws away useless items",
    category = ModuleCategory.PLAYER
)
class Manager : Module() {
    private val delay = FloatRangeValue("Delay", 100f, 150f, 0f, 500f)

    private val legit = BoolValue("Legit", false)

    private val swordSlot = IntegerValue("Sword-Slot", 1, 1, 9)
    private val pickaxeSlot = IntegerValue("Pickaxe-Slot", 2, 1, 9)
    private val axeSlot = IntegerValue("Axe-Slot", 3, 1, 9)
    private val shovelSlot = IntegerValue("Shovel-Slot", 4, 1, 9)
    private val blockSlot = IntegerValue("Block-Slot", 5, 1, 9)
    private val potionSlot = IntegerValue("Potion-Slot", 6, 1, 9)
    private val foodSlot = IntegerValue("Food-Slot", 9, 1, 9)

    private val INVENTORY_ROWS = 4
    private val INVENTORY_COLUMNS = 9
    private val ARMOR_SLOTS = 4
    private val INVENTORY_SLOTS = (INVENTORY_ROWS * INVENTORY_COLUMNS) + ARMOR_SLOTS

    private val stopwatch = MSTimer()
    private var chestTicks: Int = 0
    private var attackTicks: Int = 0
    private var placeTicks: Int = 0

    @get:JvmName("isMoved")
    var moved: Boolean = false
        private set

    @get:JvmName("isOpen")
    var open: Boolean = false
        private set

    private var nextClick: Long = 0

    @EventTarget
    fun onMotion(event: PreMotionEvent){
        if (mc.thePlayer.ticksExisted <= 40) {
            return
        }
        if (mc.currentScreen is GuiChest) {
            this.chestTicks = 0
        } else {
            chestTicks++
        }

        this.attackTicks++
        this.placeTicks++

        // Calls stopwatch.reset() to simulate opening an inventory, checks for an open inventory to be legit.
        if (legit.get() && mc.currentScreen !is GuiInventory) {
            stopwatch.reset()
            return
        }

        if ((!stopwatch.hasTimePassed(this.nextClick) || chestTicks < 10 || this.attackTicks < 10) || this.placeTicks < 10) {
            this.closeInventory()
            return
        }
        
        if (MinusBounce.moduleManager[InvMove::class.java]?.state != true && mc.currentScreen !is GuiInventory) {
            return
        }

        this.moved = false

        var helmet = -1
        var chestplate = -1
        var leggings = -1
        var boots = -1

        var sword = -1
        var pickaxe = -1
        var axe = -1
        var shovel = -1
        var block = -1
        var potion = -1
        var food = -1

        for (i in 0 until INVENTORY_SLOTS) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i) ?: continue

            val item = stack.item

            if (!ItemUtils.useful(stack)) {
                this.throwItem(i)
            }

            if (item is ItemArmor) {
                val reduction = this.armorReduction(stack)

                when (item.armorType) {
                    0 -> if (helmet == -1 || reduction > armorReduction(
                            mc.thePlayer.inventory.getStackInSlot(
                                helmet
                            )
                        )
                    ) {
                        helmet = i
                    }

                    1 -> if (chestplate == -1 || reduction > armorReduction(
                            mc.thePlayer.inventory.getStackInSlot(
                                chestplate
                            )
                        )
                    ) {
                        chestplate = i
                    }

                    2 -> if (leggings == -1 || reduction > armorReduction(
                            mc.thePlayer.inventory.getStackInSlot(
                                leggings
                            )
                        )
                    ) {
                        leggings = i
                    }

                    3 -> if (boots == -1 || reduction > armorReduction(
                            mc.thePlayer.inventory.getStackInSlot(
                                boots
                            )
                        )
                    ) {
                        boots = i
                    }
                }
            }

            if (item is ItemSword) {
                if (sword == -1) {
                    sword = i
                } else if (damage(stack) > damage(
                        mc.thePlayer.inventory.getStackInSlot(
                            sword
                        )
                    )
                ) {
                    sword = i
                }

                if (i != sword) {
                    this.throwItem(i)
                }
            }

            if (item is ItemPickaxe) {
                if (pickaxe == -1) {
                    pickaxe = i
                } else if (mineSpeed(stack) > mineSpeed(
                        mc.thePlayer.inventory.getStackInSlot(
                            pickaxe
                        )
                    )
                ) {
                    pickaxe = i
                }

                if (i != pickaxe) {
                    this.throwItem(i)
                }
            }

            if (item is ItemAxe) {
                if (axe == -1) {
                    axe = i
                } else if (mineSpeed(stack) > mineSpeed(
                        mc.thePlayer.inventory.getStackInSlot(
                            axe
                        )
                    )
                ) {
                    axe = i
                }

                if (i != axe) {
                    this.throwItem(i)
                }
            }

            if (item is ItemSpade) {
                if (shovel == -1) {
                    shovel = i
                } else if (mineSpeed(stack) > mineSpeed(
                        mc.thePlayer.inventory.getStackInSlot(
                            shovel
                        )
                    )
                ) {
                    shovel = i
                }

                if (i != shovel) {
                    this.throwItem(i)
                }
            }

            if (item is ItemBlock) {
                if (block == -1) {
                    val blockStack =
                        mc.thePlayer.inventory.getStackInSlot(
                            blockSlot.get()
                                - 1
                        )

                    block = if (blockStack == null || blockStack.item !is ItemBlock) {
                        i
                    } else {
                        blockSlot.get()- 1
                    }
                }

                val currentStack = mc.thePlayer.inventory.getStackInSlot(block) ?: continue

                if (stack.stackSize > currentStack.stackSize) {
                    block = i
                }
            }

            if (item is ItemPotion) {
                if (potion == -1) {
                    val potionStack =
                        mc.thePlayer.inventory.getStackInSlot(
                            potionSlot.get()
                                - 1
                        )

                    potion = if (potionStack == null || potionStack.item !is ItemPotion) {
                        i
                    } else {
                        potionSlot.get()- 1
                    }
                }

                val currentStack = mc.thePlayer.inventory.getStackInSlot(potion) ?: continue

                val currentItemPotion = currentStack.item as ItemPotion

                var foundCurrent = false

                for (e in mc.thePlayer.activePotionEffects) {
                    if (currentItemPotion.getEffects(currentStack).contains(e) && e.duration > 0) {
                        foundCurrent = true
                        break
                    }
                }

                var found = false

                for (e in mc.thePlayer.activePotionEffects) {
                    if (item.getEffects(stack).contains(e) && e.duration > 0) {
                        found = true
                        break
                    }
                }

                if ((PlayerUtils.potionRanking(
                        item.getEffects(stack)[0].potionID
                    ) > PlayerUtils.potionRanking(
                        currentItemPotion.getEffects(currentStack)[0].potionID
                    ) || foundCurrent) && !found
                ) {
                    potion = i
                }
            }

            if (item is ItemFood) {
                if (food == -1) {
                    val foodStack =
                        mc.thePlayer.inventory.getStackInSlot(
                            foodSlot.get() - 1
                        )

                    food = if (foodStack == null || foodStack.item !is ItemFood) {
                        i
                    } else {
                        foodSlot.get() - 1
                    }
                }

                val currentStack = mc.thePlayer.inventory.getStackInSlot(food) ?: continue

                val currentItemFood = currentStack.item as ItemFood

                if (item.getSaturationModifier(stack) > currentItemFood.getSaturationModifier(currentStack)) {
                    food = i
                }
            }
        }

        for (i in 0 until INVENTORY_SLOTS) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i) ?: continue

            val item = stack.item

            if (item is ItemArmor) {
                when (item.armorType) {
                    0 -> if (i != helmet) {
                        this.throwItem(i)
                    }

                    1 -> if (i != chestplate) {
                        this.throwItem(i)
                    }

                    2 -> if (i != leggings) {
                        this.throwItem(i)
                    }

                    3 -> if (i != boots) {
                        this.throwItem(i)
                    }
                }
            }
        }

        if (helmet != -1 && helmet != 39) {
            this.equipItem(helmet)
        }

        if (chestplate != -1 && chestplate != 38) {
            this.equipItem(chestplate)
        }

        if (leggings != -1 && leggings != 37) {
            this.equipItem(leggings)
        }

        if (boots != -1 && boots != 36) {
            this.equipItem(boots)
        }

        if (sword != -1 && sword != swordSlot.get()- 1) {
            this.moveItem(sword, swordSlot.get()- 37)
        }

        if (pickaxe != -1 && pickaxe != pickaxeSlot.get()- 1) {
            this.moveItem(pickaxe, pickaxeSlot.get()- 37)
        }

        if (axe != -1 && axe != axeSlot.get()- 1) {
            this.moveItem(axe, axeSlot.get()- 37)
        }

        if (shovel != -1 && shovel != shovelSlot.get()- 1) {
            this.moveItem(shovel, shovelSlot.get()- 37)
        }

        if (block != -1 && block != blockSlot.get()
                - 1 && MinusBounce.moduleManager[Scaffold::class.java]?.state != true
        ) {
            this.moveItem(block, blockSlot.get()- 37)
        }

        if (potion != -1 && potion != potionSlot.get()- 1) {
            this.moveItem(potion, potionSlot.get()- 37)
        }

        if (food != -1 && food != foodSlot.get()- 1) {
            this.moveItem(food, foodSlot.get()- 37)
        }
        if (this.canOpenInventory() && !this.moved) {
            this.closeInventory()
        }
    }

    @EventTarget
    fun onAttack(event: AttackEvent){
        this.attackTicks = 0
    }

    override fun onDisable() {
        if (this.canOpenInventory()) {
            this.closeInventory()
        }
    }

    private fun openInventory() {
        if (!this.open) {
            PacketUtils.sendPacket(C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT))
            this.open = true
        }
    }

    private fun closeInventory() {
        if (this.open) {
            PacketUtils.sendPacket(C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId))
            this.open = false
        }
    }

    private fun canOpenInventory(): Boolean {
        return MinusBounce.moduleManager[InvMove::class.java]?.state == true && mc.currentScreen !is GuiInventory
    }

    private fun throwItem(slot: Int) {
        if ((!this.moved || this.nextClick <= 0) && !SelectorDetectionComponent.selector(slot)) {
            if (this.canOpenInventory()) {
                this.openInventory()
            }

            mc.playerController.windowClick(
                mc.thePlayer.inventoryContainer.windowId,
                this.slot(slot), 1, 4, mc.thePlayer
            )

            this.nextClick = Math.round(
                MathUtil.getRandom(
                    delay.getMinValue(),
                    delay.getMaxValue()
                )
            ).toLong()
            stopwatch.reset()
            this.moved = true
        }
    }

    private fun moveItem(slot: Int, destination: Int) {
        if ((!this.moved || this.nextClick <= 0) && !SelectorDetectionComponent.selector(slot)) {
            if (this.canOpenInventory()) {
                this.openInventory()
            }

            mc.playerController.windowClick(
                mc.thePlayer.inventoryContainer.windowId,
                this.slot(slot),
                this.slot(destination), 2, mc.thePlayer
            )

            this.nextClick = Math.round(
                MathUtil.getRandom(
                    delay.getMinValue(),
                    delay.getMaxValue()
                )
            ).toLong()
            stopwatch.reset()
            this.moved = true
        }
    }

    private fun equipItem(slot: Int) {
        if ((!this.moved || this.nextClick <= 0) && !SelectorDetectionComponent.selector(slot)) {
            if (this.canOpenInventory()) {
                this.openInventory()
            }

            mc.playerController.windowClick(
                mc.thePlayer.inventoryContainer.windowId,
                this.slot(slot), 0, 1, mc.thePlayer
            )

            this.nextClick = Math.round(
                MathUtil.getRandom(
                    delay.getMinValue(),
                    delay.getMaxValue()
                )
            ).toLong()
            stopwatch.reset()
            this.moved = true
        }
    }

    private fun damage(stack: ItemStack): Float {
        val sword = stack.item as ItemSword
        val level = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack)
        return (sword.damageVsEntity + level * 1.25).toFloat()
    }

    private fun mineSpeed(stack: ItemStack): Float {
        val item = stack.item
        var level = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack)

        level = when (level) {
            1 -> 30
            2 -> 69
            3 -> 120
            4 -> 186
            5 -> 271
            else -> 0
        }

        return when (item) {
            is ItemPickaxe -> {
                item.toolMaterial.efficiencyOnProperMaterial + level
            }

            is ItemSpade -> {
                item.toolMaterial.efficiencyOnProperMaterial + level
            }

            is ItemAxe -> {
                item.toolMaterial.efficiencyOnProperMaterial + level
            }

            else -> 0f
        }
    }

    private fun armorReduction(stack: ItemStack): Int {
        val armor = stack.item as ItemArmor
        return armor.damageReduceAmount + EnchantmentHelper.getEnchantmentModifierDamage(
            arrayOf(stack),
            DamageSource.generic
        )
    }

    private fun slot(slot: Int): Int {
        if (slot >= 36) {
            return 8 - (slot - 36)
        }

        if (slot < 9) {
            return slot + 36
        }

        return slot
    }

    @EventTarget
    fun onPacket(event: PacketEvent){
        if (event.packet is C08PacketPlayerBlockPlacement) {
            this.placeTicks = 0
        }
    }
}

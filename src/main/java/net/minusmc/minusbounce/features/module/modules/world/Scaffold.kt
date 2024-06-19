package net.minusmc.minusbounce.features.module.modules.world

import net.minecraft.block.BlockAir
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.injection.access.StaticStorage
import net.minusmc.minusbounce.injection.implementations.IEntityPlayerSP
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.block.BlockUtils
import net.minusmc.minusbounce.utils.block.PlaceInfo
import net.minusmc.minusbounce.utils.extensions.eyes
import net.minusmc.minusbounce.utils.extensions.plus
import net.minusmc.minusbounce.utils.extensions.times
import net.minusmc.minusbounce.utils.misc.RandomUtils
import net.minusmc.minusbounce.utils.movement.MovementFixType
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.*
import kotlin.math.*


@ModuleInfo("Scaffold", "Scaffold", "Make ur balls floating in the mid-air", ModuleCategory.WORLD)
class Scaffold: Module(){

    private val modes = ListValue("Mode", arrayOf("Normal", "Snap", "Telly"), "Normal")

    private val delayValue = IntRangeValue("Delay", 0, 0, 0, 10)
    private val sprint = ListValue("Sprint", arrayOf("Normal", "VulcanToggle", "Bypass", "Omni", "Off"), "Normal")

    private val speed = FloatRangeValue("Speed", 90f, 90f, 0f, 180f)

    private val eagleValue = ListValue("Eagle", arrayOf("Off", "DelayedBlocks", "DelayedTimer"), "Off")
    private val eagleEdgeDistanceValue = FloatRangeValue("EagleEdgeDistance", 0f, 0f, 0f, 0.2f) { !eagleValue.get().equals("off", true) }
    private val eagleBlocksValue = IntegerValue("EagleBlocks", 0, 1, 10) { eagleValue.get().equals("delayedblocks", true) }
    private val eagleDelayValue = IntegerValue("EagleDelay", 0, 0, 20) { eagleValue.get().equals("delayedtimer", true) }
    private val eagleSilent = BoolValue("Silent", false) { !eagleValue.get().equals("Off", true) }

    private val towerModeValue = ListValue("Tower", arrayOf("Off", "Vanilla", "Legit", "Matrix", "Vulcan", "Verus", "Air"), "Off")
    private val rayTraceValue = ListValue("RayTrace", arrayOf("Off", "Normal", "Strict"), "Off")
    private val sameYValue = ListValue("SameY", arrayOf("Off", "Same", "AutoJump"), "Off")

    private val timer = FloatValue("Timer", 1f, 0f, 5f)
    private val safeWalk = BoolValue("SafeWalk", false)
    private val movementCorrection = BoolValue("MovementCorrection", true)

    private val counter = BoolValue("Counter", false)

    /* Values */
    private var targetBlock: Vec3? = null
    private var placeInfo: PlaceInfo? = null
    private var blockPlace: BlockPos? = null
    private var hitVec: Vec3? = null
    private var targetYaw = 0f
    private var targetPitch = 0f
    private var ticksOnAir = 0
    private var startY = 0.0

    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false

    private val eagleDelayTimer = MSTimer()
    private var itemStack: ItemStack? = null

    override fun onDisable() {
        mc.gameSettings.keyBindSneak.pressed = false
        mc.gameSettings.keyBindSprint.pressed = false
        mc.timer.timerSpeed = 1f
        RotationUtils.setRotations((mc.thePlayer as IEntityPlayerSP).serverRotation)
    }

    override fun onEnable() {
        targetYaw = mc.thePlayer.rotationYaw - 180
        targetPitch = 90f

        startY = floor(mc.thePlayer.posY)
        targetBlock = null
        hitVec = rayTrace(Rotation(targetYaw, targetPitch))?.hitVec
    }

    /* Init */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Eagle
        if (!eagleValue.equals("Off")) {
            var dif = 0.5
            val blockPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)

            for (facingType in StaticStorage.facings()) {
                if (facingType == EnumFacing.UP || facingType == EnumFacing.DOWN) continue

                val placeInfo = blockPos.offset(facingType)
                if (BlockUtils.isReplaceable(blockPos)) {
                    var calcDif = if (facingType == EnumFacing.NORTH || facingType == EnumFacing.SOUTH)
                        abs(placeInfo.z + 0.5 - mc.thePlayer.posZ)
                    else
                        abs(placeInfo.x + 0.5 - mc.thePlayer.posX)

                    calcDif -= 0.5

                    if (calcDif < dif)
                        dif = calcDif
                }
            }

            val canSneak = when (eagleValue.get()) {
                "DelayedBlocks" -> placedBlocksWithoutEagle >= eagleBlocksValue.get()
                "DelayedTimer" -> eagleDelayTimer.hasTimePassed(eagleDelayValue.get() * 100)
                else -> false
            }

            if (canSneak) {
                val shouldEagle = BlockUtils.isReplaceable(blockPos) || dif < RandomUtils.nextFloat(eagleEdgeDistanceValue.getMinValue(), eagleEdgeDistanceValue.getMaxValue())

                if (eagleSilent.get()) {
                    if (eagleSneaking != shouldEagle)
                        mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, if (shouldEagle) C0BPacketEntityAction.Action.START_SNEAKING else C0BPacketEntityAction.Action.STOP_SNEAKING))

                    eagleSneaking = shouldEagle
                } else
                    mc.gameSettings.keyBindSneak.pressed = shouldEagle

                placedBlocksWithoutEagle = 0
            } else
                placedBlocksWithoutEagle++
        }
    }
    @EventTarget
    fun onMove(event: MoveEvent) {
        event.isSafeWalk = safeWalk.get()
    }

    @EventTarget(priority = -1)
    fun onPreMotion(event: PreMotionEvent){
        if (targetBlock == null || placeInfo == null || blockPlace == null)
            return

        mc.timer.timerSpeed = timer.get()

        when (sprint.get().lowercase()) {
            "normal" -> mc.gameSettings.keyBindSprint.pressed = true
            "omni" -> mc.thePlayer.isSprinting = true
            "vulcantoggle" -> {
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
            }
            "bypass" -> if (MovementUtils.isMoving && mc.thePlayer.isSprinting && mc.thePlayer.onGround) {
                val direction = MovementUtils.getRawDirection()
                val x = mc.thePlayer.posX + sin(direction) * 0.221
                val z = mc.thePlayer.posZ - cos(direction) * 0.221
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, event.y, z, false))
            }
            else -> mc.thePlayer.isSprinting = false
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent){
        val packet = event.packet

        if (packet is S2FPacketSetSlot) {
            if (packet.func_149174_e() == null) {
                event.cancelEvent()
                return
            }
            try {
                val slot = packet.func_149173_d() - 36
                if (slot < 0) return
                val itemStack = mc.thePlayer.inventory.getStackInSlot(slot)
                val item = packet.func_149174_e().item

                if ((itemStack == null && packet.func_149174_e().stackSize <= 6 && item is ItemBlock && !InventoryUtils.isBlockListBlock(item.block)) ||
                    (itemStack != null && abs(itemStack.stackSize - packet.func_149174_e().stackSize) <= 6) ||
                    (packet.func_149174_e() == null)
                ) {
                    event.cancelEvent()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent){
        val blockSlot = InventoryUtils.findAutoBlockBlock()
        if (blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = blockSlot - 36
            itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).stack
        }

        if (BlockUtils.blockRelativeToPlayer(0, -1, 0) is BlockAir) {
            ticksOnAir++
        } else {
            ticksOnAir = 0
        }

        targetBlock = BlockUtils.getPlacePossibility(0.0, 0.0, 0.0) ?: return
        placeInfo = BlockUtils.getEnumFacing(targetBlock!!) ?: return
        blockPlace = placeInfo?.blockPos ?: return

        calculateRotations()

        if (sameYValue.get().equals("AutoJump", true)) {
            mc.gameSettings.keyBindJump.pressed = mc.thePlayer.onGround && MovementUtils.isMoving || mc.gameSettings.keyBindJump.isPressed
        }

        // Same Y
        val sameY = !sameYValue.get().equals("Off", true) && !mc.gameSettings.keyBindJump.isKeyDown && MovementUtils.isMoving

        if (startY - 1 != floor(targetBlock!!.yCoord) && sameY) {
            return
        }

        if (ticksOnAir > RandomUtils.nextInt(delayValue.getMinValue(), delayValue.getMaxValue()) && isObjectMouseOverBlock(placeInfo!!.enumFacing, blockPlace!!, rayTraceValue.get() == "Strict")) {
            /* I solved the rayTrace.*/
            if (mc.playerController.onPlayerRightClick(
                    mc.thePlayer,
                    mc.theWorld,
                    mc.thePlayer.inventory.getCurrentItem(),
                    blockPlace!!,
                    placeInfo!!.enumFacing,
                    hitVec ?: mc.objectMouseOver.hitVec
                )
            ) {
                mc.thePlayer.swingItem()
            }
            ticksOnAir = 0
        } else if (Math.random() > 0.98 && mc.rightClickDelayTimer <= 0) {
            PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(itemStack))
            mc.rightClickDelayTimer = 0
        }

        //For Same Y
        if (mc.thePlayer.onGround || mc.gameSettings.keyBindJump.isKeyDown && !MovementUtils.isMoving) {
            startY = floor(mc.thePlayer.posY)
        }

        if (mc.thePlayer.posY < startY) {
            startY = mc.thePlayer.posY
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (modes.get().equals("telly", true) && mc.thePlayer.onGround && MovementUtils.isMoving) {
            mc.thePlayer.jump()
        }
    }

    /**
     * Gets the block relative to the player from the offset
     *
     * @return block relative to the player
     */

    private fun calculateRotations() {
        when (modes.get().lowercase()) {
            "normal" -> if (ticksOnAir > 0 && !isObjectMouseOverBlock(placeInfo!!.enumFacing, blockPlace!!, true)) {
                ClientUtils.displayChatMessage("""
                    Debug:
                    Obj BlockPos: ${mc.objectMouseOver.blockPos}
                    BlockPos: $blockPlace
                """.trimIndent())
                getRotations()
            }
            "snap" -> {
                getRotations()

                if (ticksOnAir <= 0 || isObjectMouseOverBlock(placeInfo!!.enumFacing, blockPlace!!, true)) {
                    targetYaw = MovementUtils.getRawDirection().toFloat()
                }
            }

            "telly" -> if (RotationUtils.offGroundTicks >= 3) {
                if (!isObjectMouseOverBlock(placeInfo!!.enumFacing, blockPlace!!, true)) {
                    getRotations()
                }

            } else {
                getRotations()
                targetYaw = mc.thePlayer.rotationYaw
            }
        }

        /* Setting rotations */
        RotationUtils.setRotations(
            Rotation(targetYaw, targetPitch),
            0,
            RandomUtils.nextFloat(speed.getMinValue(), speed.getMaxValue()),
            if (movementCorrection.get()) MovementFixType.FULL
            else MovementFixType.NONE
        )
    }

    private fun getRotations() {
        val center = Vec3(blockPlace) + 0.5 + Vec3(placeInfo?.enumFacing?.directionVec) * 0.5
        val playerYaw = mc.thePlayer.rotationYaw.roundToInt()
        val hitVecList = mutableListOf<Vec3>()

        for (possibleYaw in playerYaw - 180..playerYaw + 180 step 45){
            for (possiblePitch in -90..90 step 1){
                val result = rayTrace(Rotation(possibleYaw.toFloat(), possiblePitch.toFloat())) ?: continue
                result.hitVec ?: continue

                if (result.blockPos == blockPlace && result.sideHit == placeInfo?.enumFacing && result.sideHit != EnumFacing.DOWN) {
                    hitVecList.add(result.hitVec)
                }
            }
        }

        hitVec = hitVecList.minByOrNull { hitVec ->
            val rotations = RotationUtils.toRotation(hitVec)
            val x = targetYaw - rotations.yaw
            val y = targetPitch - rotations.pitch
            val z = hitVec.xCoord - center.xCoord
            val t = hitVec.yCoord - center.yCoord
            val e = hitVec.zCoord - center.zCoord
            sqrt(x * x + y * y + z * z + t * t + e * e)
        } ?: return

        RotationUtils.toRotation(hitVec ?: return).let {
            targetYaw = it.yaw
            targetPitch = it.pitch
        }
    }


    @JvmOverloads
    fun isObjectMouseOverBlock(
        facing: EnumFacing,
        block: BlockPos,
        strict: Boolean = when (rayTraceValue.get().lowercase()) {
            "normal" -> false
            "strict" -> true
            else -> true
        },
        rotation: Rotation? = null,
        obj: MovingObjectPosition? = rayTrace(rotation) ?: mc.objectMouseOver,
    ): Boolean{
        obj?.hitVec ?: return false

        return obj.blockPos == block && (!strict || obj.sideHit == facing)
    }


    private fun rayTrace(rotation: Rotation?): MovingObjectPosition? {
        rotation ?: return null
        mc.theWorld ?: return null
        val entity = mc.renderViewEntity ?: return null
        val vec3 = entity.eyes
        val vec32 = vec3 + (RotationUtils.getVectorForRotation(rotation) * if (mc.playerController.currentGameType.isCreative) 5.0 else 4.5)
        return entity.worldObj.rayTraceBlocks(vec3, vec32, false, false, true)
    }

    /**
     * Render counter (made it after we had done item selector)
     */
//    @EventTarget
//    fun onRender2D(event: Render2DEvent) {
//        val sc = ScaledResolution(mc)
//
//    }

}
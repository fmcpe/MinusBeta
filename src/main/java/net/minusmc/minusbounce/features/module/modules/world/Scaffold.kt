package net.minusmc.minusbounce.features.module.modules.world

import net.minecraft.block.BlockAir
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.util.*
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
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt


@ModuleInfo("Scaffold", "Scaffold", "Make ur balls floating in the mid-air", ModuleCategory.WORLD)
class Scaffold: Module(){

    private val modes = ListValue("Mode", arrayOf("Normal", "Snap", "Telly", "Legit"), "Normal")

    private val delayValue = IntRangeValue("Delay", 0, 0, 0, 10)
    private val sprint = ListValue("Sprint", arrayOf("Normal", "VulcanToggle", "Omni", "Off"), "Normal")

    private val speed = FloatRangeValue("Speed", 90f, 90f, 0f, 180f)

    private val eagleValue = ListValue("Eagle", arrayOf("Off", "DelayedBlocks", "DelayedTimer"), "Off")
    private val eagleEdgeDistanceValue = FloatRangeValue("EagleEdgeDistance", 0f, 0f, 0f, 0.2f) { !eagleValue.get().equals("off", true) }
    private val eagleBlocksValue = IntegerValue("EagleBlocks", 0, 1, 10) { eagleValue.get().equals("delayedblocks", true) }
    private val eagleDelayValue = IntegerValue("EagleDelay", 0, 0, 20) { eagleValue.get().equals("delayedtimer", true) }
    private val eagleSilent = BoolValue("Silent", false) { !eagleValue.get().equals("Off", true) }

    private val towerModeValue = ListValue("Tower", arrayOf("Off", "Vanilla", "Legit", "Matrix", "Vulcan", "Verus", "Air"), "Off")
    private val sameYValue = ListValue("SameY", arrayOf("Off", "Same", "AutoJump"), "Off")

    private val timer = FloatValue("Timer", 1f, 0f, 5f)
    private val safeWalk = BoolValue("SafeWalk", false)
    private val movementCorrection = BoolValue("MovementCorrection", true)

    private val counter = BoolValue("Counter", false)

    /* Values */
    private var targetBlock: BlockPos? = null
    private var placeInfo: PlaceInfo? = null
    private var blockPlace: BlockPos? = null
    private var hitVec: Vec3?= null
    private var targetYaw = 0f
    private var targetPitch = 0f
    private var ticksOnAir = 0
    private var startY = 0.0
    private var previousXPos = 0.0
    private var previousZPos = 0.0

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
            else -> {
                mc.gameSettings.keyBindSprint.pressed = false
                mc.thePlayer.isSprinting = false
            }
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

        placeInfo = this.getPlacePossibility() ?: return
        targetBlock = placeInfo?.blockPos?.offset(placeInfo?.enumFacing)
        blockPlace = placeInfo?.blockPos ?: return

        calculateRotations()

        if (sameYValue.get().equals("AutoJump", true)) {
            mc.gameSettings.keyBindJump.pressed = mc.thePlayer.onGround && MovementUtils.isMoving || mc.gameSettings.keyBindJump.isPressed
        }

        // Same Y
        val sameY = !sameYValue.get().equals("Off", true) && !mc.gameSettings.keyBindJump.isKeyDown && MovementUtils.isMoving

        if (startY - 1 != floor(targetBlock?.y?.toDouble() ?: return) && sameY) {
            return
        }

        if (ticksOnAir > RandomUtils.nextInt(delayValue.getMinValue(), delayValue.getMaxValue()) && isObjectMouseOverBlock(placeInfo?.enumFacing ?: return, blockPlace ?: return)) {
            mc.rightClickDelayTimer = 0
            mc.rightClickMouse()
            ticksOnAir = 0
        }

        /**
         * SameY
         */
        if (mc.thePlayer.onGround || mc.gameSettings.keyBindJump.isKeyDown && !MovementUtils.isMoving) {
            startY = floor(mc.thePlayer.posY)
        }

        if (mc.thePlayer.posY < startY) {
            startY = mc.thePlayer.posY
        }
    }

    /***
     * @author fmcpe
     *
     * calculateRealY
     * 6/21/2024
     */
    private fun calculateRealY(world: WorldClient, bb: AxisAlignedBB, motionX: Double, motionZ: Double, multiplier: Double): Double {
        var realY = -0.08
        val offset = bb.offset(motionX * multiplier, -0.08, motionZ * multiplier)
        val abb = bb.offset(motionX * multiplier, 0.0, motionZ * multiplier)

        for (box in world.getCollidingBoundingBoxes(mc.thePlayer, offset)) {
            realY = box.calculateYOffset(abb, realY)
        }

        return realY
    }

    private fun getPlacePossibility(): PlaceInfo? {
        var placeRotation: PlaceInfo? = null

        val possibilities = mutableListOf<Vec3>()

        for (x in -5..5) {
            for (y in -5..5) {
                for (z in -5..5) {
                    val block = BlockUtils.blockRelativeToPlayer(x, y, z)
                    if (block is BlockAir) continue
                    for (xAdd in -1..1 step 1){
                        for (yAdd in -1..1 step 1){
                            for (zAdd in -1..1 step 1){
                                if(xAdd != 0) possibilities.add(offset(x + xAdd, y, z))
                                if(yAdd != 0) possibilities.add(offset(x, y + yAdd, z))
                                if(zAdd != 0) possibilities.add(offset(x, y, z + zAdd))
                            }
                        }
                    }
                }
            }
        }

        possibilities.removeIf {
            mc.thePlayer.getDistance(it.xCoord, it.yCoord, it.zCoord) > 5 ||
                    mc.theWorld.getBlockState(BlockPos(it.xCoord, it.yCoord, it.zCoord)).block !is BlockAir
        }

        for (side in StaticStorage.facings()) {
            val neighbor = BlockPos(
                possibilities.minByOrNull {
                    val x = mc.thePlayer.posX - it.xCoord
                    val y = mc.thePlayer.posY - 1 - it.yCoord
                    val z = mc.thePlayer.posZ - it.zCoord
                    sqrt(x * x + y * y + z * z)
                } ?: return null
            ).offset(side)

            if (BlockUtils.isClickable(neighbor)) {
                placeRotation = PlaceInfo(neighbor, side.opposite)
            }
        }

        return placeRotation
    }

    private fun offset(x: Int, y: Int, z: Int): Vec3{
        return Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z)
    }


    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (modes.get().equals("telly", true) && mc.thePlayer.onGround && MovementUtils.isMoving) {
            mc.thePlayer.jump()
        }
    }

    /**
     *  @author fmcpe
     *  Rotation Update
     *
     *  6/21/2024
     *  Legit Mode
     *  0.288 Exception + Out Of Edge Prediction
     *
     *  Gets the block relative to the player from the offset
     *  @return block relative to the player
     */
    private fun calculateRotations() {
        when (modes.get().lowercase()) {
            "normal" -> if (ticksOnAir > 0 && !isObjectMouseOverBlock(placeInfo!!.enumFacing, blockPlace!!)) {
                getRotations()
            }
            "snap" -> {
                getRotations()

                if (ticksOnAir <= 0 || isObjectMouseOverBlock(placeInfo!!.enumFacing, blockPlace!!)) {
                    targetYaw = MovementUtils.getRawDirection().toFloat()
                }
            }

            "legit" -> {
                if (ticksOnAir > 0 && !isObjectMouseOverBlock(placeInfo!!.enumFacing, blockPlace!!)) {
                    getLegitPosibility()
                }
            }

            "telly" -> if (RotationUtils.offGroundTicks >= 3) {
                if (!isObjectMouseOverBlock(placeInfo!!.enumFacing, blockPlace!!)) getRotations()
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

    private fun getRotations(){
        hitVec = Vec3(blockPlace) + 0.5 + Vec3(placeInfo?.enumFacing?.directionVec) * 0.5
        val playerYaw = mc.thePlayer.rotationYaw.roundToInt()

        for (yaw in (playerYaw - 180)..(playerYaw + 180) step 45){
            for (pitch in 90 downTo 30){
                val result = rayTrace(Rotation(yaw.toFloat(), pitch.toFloat())) ?: continue

                if (result.blockPos == blockPlace && result.sideHit == placeInfo?.enumFacing) {
                    hitVec = result.hitVec

                    RotationUtils.toRotation(hitVec ?: return).let {
                        targetYaw = it.yaw
                        targetPitch = it.pitch
                    }

                    return
                }
            }
        }

        RotationUtils.toRotation(hitVec ?: return).let {
            targetYaw = it.yaw
            targetPitch = it.pitch
        }
    }

    /**
     * @author fmcpe
     * Legit Mode / Rotation
     *
     * 6/21/2024
     * Legit Mode
     * 0.300 Exception + Out Of Edge Prediction
     */
    private fun getLegitPosibility(){
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return
        val hitVecList = mutableListOf<Vec3>()
        var xPos = mc.thePlayer.posX
        var zPos = mc.thePlayer.posZ

        val realYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw)
        if(
            (realYaw in -77.5..77.5) &&
            (   realYaw in -102.5..102.5 ||
                    realYaw in -12.5..12.5 ||
                    realYaw < -167.5 ||
                    realYaw > 167.5
                    )
        ) {
            xPos += mc.thePlayer.posX - previousXPos
            zPos += mc.thePlayer.posZ - previousZPos
        }

        previousXPos = mc.thePlayer.posX
        previousZPos = mc.thePlayer.posZ
        if (xPos > (blockPlace?.x?.plus(0.300) ?: return) ||
            xPos < (blockPlace?.x?.minus(1.300) ?: return) ||
            zPos > (blockPlace?.z?.plus(0.300) ?: return) ||
            zPos < (blockPlace?.z?.minus(1.300) ?: return)
        ) {
            if(
                if (!player.onGround) player.motionY > 0 else
                    (calculateRealY(world, bb, motionX, motionZ, 0.91 * 0.6) <= -0.015625 ||
                    calculateRealY(world, bb, motionX, motionZ, 1.0) <= -0.015625)
            ){
                for (pitch in 90 downTo 30){
                    rayTrace(
                        Rotation(
                            MathHelper.wrapAngleTo180_float(
                                mc.thePlayer.rotationYaw - 180f,
                            ),
                            pitch.toFloat())
                    )?.let {
                        if (it.blockPos == blockPlace && it.sideHit == placeInfo?.enumFacing){
                            hitVecList.add(it.hitVec)
                        }
                    }
                }

                hitVec = hitVecList.minByOrNull { hitVec ->
                    val rotations = RotationUtils.toRotation(hitVec)
                    val yaw = targetYaw - rotations.yaw
                    val pitch = targetPitch - rotations.pitch
                    val x = hitVec.xCoord - blockPlace!!.x
                    val y = hitVec.yCoord - blockPlace!!.y
                    val z = hitVec.zCoord - blockPlace!!.z
                    sqrt(x * x + y * y + z * z + yaw * yaw + pitch * pitch)
                } ?: return

                RotationUtils.toRotation(hitVec ?: return).let {
                    targetYaw = it.yaw
                    targetPitch = it.pitch
                    ClientUtils.displayChatMessage(Rotation(targetYaw, targetPitch).toString())
                }
            }
        }
    }


    /**
     * @author fmcpe
     *
     * 6/21/2024
     * Simple Raytrace
     * From MCP
     *
     * isObjectMouseOverBlock()
     * rayTrace()
     */
    @JvmOverloads
    fun isObjectMouseOverBlock(
        facing: EnumFacing,
        block: BlockPos,
        rotation: Rotation? = null,
        obj: MovingObjectPosition? = rayTrace(rotation) ?: mc.objectMouseOver,
    ): Boolean{
        obj?.hitVec ?: return false

        return obj.blockPos == block && obj.sideHit == facing
    }
    private fun rayTrace(rotation: Rotation?): MovingObjectPosition? {
        return mc.renderViewEntity.worldObj.rayTraceBlocks(
            mc.renderViewEntity.eyes,
            mc.renderViewEntity.eyes + (
                RotationUtils.getVectorForRotation(
                    rotation ?: return null
                ) * if (mc.playerController.currentGameType.isCreative) 5.0 else 4.5),
            false,
            false,
            true
        )
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
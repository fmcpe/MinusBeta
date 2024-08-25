package net.minusmc.minusbounce.features.module.modules.world

import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.util.*
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.injection.access.StaticStorage
import net.minusmc.minusbounce.injection.implementations.IEntityPlayerSP
import net.minusmc.minusbounce.ui.font.Fonts
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.InventoryUtils.BLOCK_BLACKLIST
import net.minusmc.minusbounce.utils.MovementUtils.isMoving
import net.minusmc.minusbounce.utils.RotationUtils.getRotationDifference
import net.minusmc.minusbounce.utils.block.BlockUtils
import net.minusmc.minusbounce.utils.block.BlockUtils.rayTrace
import net.minusmc.minusbounce.utils.block.PlaceInfo
import net.minusmc.minusbounce.utils.extensions.*
import net.minusmc.minusbounce.utils.misc.RandomUtils
import net.minusmc.minusbounce.utils.movement.MovementFixType
import net.minusmc.minusbounce.utils.render.RenderUtils
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.*


@Suppress("UNUSED_PARAMETER")
@ModuleInfo("Scaffold", "Scaffold", "Use huge balls to rolling on mid-air", ModuleCategory.WORLD)
class Scaffold: Module(){
    private val modes = ListValue("Mode", arrayOf("Normal", "Snap", "Telly", "None", "Legit", "StaticGod"), "Normal")
    private val searchModeValue = ListValue("AimMode", arrayOf("Area", "Center", "TryRotation"), "Center")
    private val yawOffset = ListValue("OffsetYaw", arrayOf("Dynamic", "Side"), "Dynamic") { searchModeValue.get() == "TryRotation" }
    private val reset = BoolValue("RotationActivateReset", false) { modes.get() == "Telly" || modes.get() == "Snap" }
    private val rayTrace = ListValue("RayTraceMethod", arrayOf("Calculate", "Client", "None"), "Client")
    private val ticks = IntegerValue("Ticks", 3, 0, 10) { modes.get() == "Telly" }
    private val delayValue = IntRangeValue("Delay", 0, 0, 0, 10)
    private val sprint = ListValue("Sprint", arrayOf("Normal", "Legit", "VulcanToggle", "Omni", "Off"), "Normal")

    private val speed = FloatRangeValue("Speed", 90f, 90f, 0f, 180f)

    private val eagleValue = ListValue("Eagle", arrayOf("Off", "Normal"), "Off")
    private val eagleEdgeDistanceValue = FloatRangeValue("EagleEdgeDistance", 0f, 0f, 0f, 0.2f) { !eagleValue.get().equals("off", true) }
    private val eagleBlocksValue = IntegerValue("EagleBlocks", 0, 1, 10) { eagleValue.get().equals("normal", true) }
    private val eagleSilent = BoolValue("Silent", false) { !eagleValue.get().equals("Off", true) }

    private val towerModeValue = ListValue("Tower", arrayOf("Off", "Air", "Legit", "MMC", "Matrix", "NCP", "Normal", "Vanilla", "Vulcan", "WatchDog"), "Off")
    private val sameYValue = ListValue("SameY", arrayOf("Off", "Same", "AutoJump"), "Off")

    private val timer = FloatValue("Timer", 1f, 0f, 5f)
    private val safeWalk = BoolValue("SafeWalk", false)
    private val predict = BoolValue("Predict", false)
    private val adStrafe = ListValue("ADStrafe", arrayOf("Always", "Edge", "None"))
    private val jumpAutomatically = BoolValue("JumpAutomatically", false)
    private val movementCorrection = BoolValue("MovementCorrection", true)

    private val counter = BoolValue("Counter", false)

    /* Values */
    private var targetBlock: BlockPos? = null
    private var offset: EnumFacingOffset? = null
    private var blockPlace: BlockPos? = null
    private var side: EnumFacing? = null
    private var enumFacing: EnumFacing? = null

    private var adStrafeDirection: Boolean = false
    private val adStrafeTimer = MSTimer()
    private var xPos: Double = 0.0
    private var zPos: Double = 0.0
    private var startY = 0.0
    private var targetYaw = 0f
    private var targetPitch = 0f
    private var ticksOnAir = 0

    private var lastX: Double = 0.0
    private var lastZ: Double = 0.0

    // Render thingy
    private var progress = 0f
    private var lastMS = 0L

    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false

    override fun onDisable() {
        mc.gameSettings.keyBindUseItem.pressed = false
        mc.gameSettings.keyBindSneak.pressed = false
        mc.gameSettings.keyBindSprint.pressed = false
        mc.timer.timerSpeed = 1f
        RotationUtils.setRotations((mc.thePlayer as IEntityPlayerSP).serverRotation)
    }

    override fun onEnable() {
        targetYaw = mc.thePlayer.rotationYaw - 180
        targetPitch = 80.34F
        startY = floor(mc.thePlayer.posY)
        lastMS = System.currentTimeMillis()
    }

    /* Init */
    private fun calculateSneaking() {
        // Eagle
        if (!eagleValue.equals("Off") && mc.thePlayer.onGround) {
            var dif = 0.5
            val edge = RandomUtils.nextFloat(eagleEdgeDistanceValue.getMinValue(), eagleEdgeDistanceValue.getMaxValue())
            val blockPos = blockPlace ?: return

            if (edge > 0.0F) {
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
            }

            if (placedBlocksWithoutEagle >= eagleBlocksValue.get()) {
                val shouldEagle = BlockUtils.isReplaceable(blockPos) ||  (edge > 0 && dif < edge)

                if (eagleSilent.get()) {
                    if (eagleSneaking != shouldEagle) {
                        mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, if (shouldEagle) C0BPacketEntityAction.Action.START_SNEAKING else C0BPacketEntityAction.Action.STOP_SNEAKING))
                    }

                    eagleSneaking = shouldEagle
                } else {
                    mc.gameSettings.keyBindSneak.pressed = shouldEagle
                }

                placedBlocksWithoutEagle = 0
            } else {
                placedBlocksWithoutEagle++
            }
        }
    }

    @EventTarget(priority = -1)
    fun onPreMotion(event: PreMotionEvent){
        if (targetBlock == null || blockPlace == null)
            return

        mc.timer.timerSpeed = timer.get()

        when (sprint.get().lowercase()) {
            "normal" -> {
                mc.gameSettings.keyBindSprint.pressed = true
            }
            "legit" -> {
                val player = MathHelper.wrapAngleTo180_float(MovementUtils.getPlayerDirection())
                val target = MathHelper.wrapAngleTo180_float(RotationUtils.targetRotation?.yaw ?: return)

                mc.gameSettings.keyBindSprint.pressed = if(RotationUtils.active) abs(player - target) > 90 - 22.5 else true
            }
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

        when(towerModeValue.get().lowercase()){
            "air" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.ticksExisted % 2 == 0 && blockNear(2)) {
                    mc.thePlayer.motionY = 0.42
                    mc.thePlayer.onGround = true
                }
            }

            "legit" ->{
                if (mc.thePlayer.onGround && mc.gameSettings.keyBindJump.isKeyDown) {
                    mc.thePlayer.jump()
                }
            }

            "matrix" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && isBlockUnder(2.0, false) && mc.thePlayer.motionY < 0.2) {
                    mc.thePlayer.motionY = 0.42
                    mc.thePlayer.onGround = true
                }
            }

            "ncp" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && blockNear(2)) {
                    PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(null))

                    if (mc.thePlayer.posY % 1 <= 0.00153598) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, floor(mc.thePlayer.posY), mc.thePlayer.posZ)
                        mc.thePlayer.motionY = 0.42
                    } else if (mc.thePlayer.posY % 1 < 0.1 && RotationUtils.offGroundTicks != 0) {
                        mc.thePlayer.motionY = 0.0
                        mc.thePlayer.setPosition(mc.thePlayer.posX, floor(mc.thePlayer.posY), mc.thePlayer.posZ)
                    }
                }
            }

            "normal" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.42
                    }
                }
            }

            "vanilla" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && blockNear(2)) {
                    mc.thePlayer.motionY = 0.42
                }
            }

            "vulcan" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && blockNear(2) && RotationUtils.offGroundTicks > 3) {
                    val itemStack = mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem]

                    if (itemStack == null || (itemStack.stackSize > 2)) {
                        PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(null))
                    }
                    mc.thePlayer.motionY = 0.42
                }
            }

            "watchdog" -> {
                if (!mc.gameSettings.keyBindJump.isKeyDown || !isMoving) {
                    return
                }

                if (mc.thePlayer.onGround) {
                    mc.thePlayer.motionY = MovementUtils.getJumpBoostModifier(0.42F)
                    mc.thePlayer.motionX *= .65
                    mc.thePlayer.motionZ *= .65
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent){
        val packet = event.packet

        when(towerModeValue.get().lowercase()){
            "mmc" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown && packet is C08PacketPlayerBlockPlacement) {
                    if (packet.getPosition() == BlockPos(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY - 1.4,
                            mc.thePlayer.posZ
                        )
                    ) {
                        mc.gameSettings.keyBindSprint.pressed = false
                        mc.thePlayer.isSprinting = false
                        mc.thePlayer.motionY = 0.42
                    }
                }
            }

            "normal", "watchdog" -> {
                if (mc.thePlayer.motionY > -0.0784000015258789 && packet is C08PacketPlayerBlockPlacement) {
                    if (packet.getPosition() == BlockPos(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY - 1.4,
                            mc.thePlayer.posZ
                        )
                    ) {
                        mc.thePlayer.motionY = -0.0784000015258789
                    }
                }
            }
        }

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

                if ((itemStack == null && packet.func_149174_e().stackSize <= 6 && item is ItemBlock && item.block !in BLOCK_BLACKLIST) ||
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
    fun onClick(e: PreUpdateEvent){
        if (BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ).getBlock() is BlockAir) {
            ticksOnAir++
        } else {
            ticksOnAir = 0
        }

        val blockSlot = InventoryUtils.findBlockInHotbar() ?: return
        if (blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = blockSlot - 36
        }

        targetBlock = this.getPlacePossibility() ?: return
        offset = this.getEnumFacing(targetBlock?.toVec()!!) ?: return
        side = offset?.enumFacing?.opposite ?: return
        enumFacing = offset?.enumFacing ?: return

        val offset = this.offset ?: return
        blockPlace = targetBlock?.add(offset.offset.xCoord.toInt(), offset.offset.yCoord.toInt(), offset.offset.zCoord.toInt()) ?: return

        calculateSneaking()

        calculateRotations()

        if (sameYValue.get().equals("AutoJump", true)) {
            if(mc.thePlayer.onGround && (isMoving || mc.gameSettings.keyBindJump.isPressed)){
                mc.thePlayer.jump()
            }
        }

        if (startY - 1 != floor(targetBlock?.y?.toDouble() ?: return) && sameY) {
            return
        }

        val eyes = mc.thePlayer.eyes
        val look = RotationUtils.getVectorForRotation(currentRotation)
        val vec = eyes + (look * maxReach)

        val placeOn = blockPlace ?: return
        val theWorld = mc.theWorld ?: return
        val placeOnState = theWorld.getBlockState(placeOn)
        val collisionBox = placeOnState.block.getCollisionBoundingBox(theWorld, placeOn, placeOnState) ?: return

        val rayTrace = when(this.rayTrace.get().lowercase()){
            "calculate" -> collisionBox.calculateIntercept(eyes, vec) ?: return
            "client" -> mc.objectMouseOver
            else -> MovingObjectPosition(Vec3(placeOn.x + 0.5, placeOn.y + 0.5, placeOn.z + 0.5), enumFacing, placeOn)
        }

        if (mc.thePlayer.inventory.currentItem == InventoryUtils.serverSlot &&
            !BadPacketUtils.bad(slot = false, attack = true, swing = false, block = false, inventory = true) &&
            ticksOnAir > RandomUtils.nextInt(delayValue.getMinValue(), delayValue.getMaxValue()) &&
            when(this.rayTrace.get().lowercase()){
                "calculate" -> rayTrace.sideHit == enumFacing
                else -> rayTrace.sideHit == enumFacing && rayTrace.blockPos == placeOn
            } &&
            mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
        ){
            val itemstack = mc.thePlayer.inventory.getCurrentItem()
            if (mc.playerController.onPlayerRightClick(
                    mc.thePlayer,
                    mc.theWorld, itemstack,
                    blockPlace,
                    enumFacing,
                    modifyVec(rayTrace.hitVec, enumFacing ?: return, Vec3(blockPlace))
                )
            ) {
                mc.netHandler.addToSendQueue(C0APacketAnimation())
            }

            if (itemstack.stackSize == 0) {
                mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = null
            }

            ticksOnAir = 0
        }

        if (mc.thePlayer.onGround || mc.gameSettings.keyBindJump.isKeyDown && !isMoving) {
            startY = floor(mc.thePlayer.posY)
        }

        if(mc.thePlayer.posY < startY){
            startY = mc.thePlayer.posY
        }
    }

    /***
     * @author fmcpe
     *
     * calculateRealY
     * 6/21/2024
     */
    private fun calculateRealY(world: WorldClient, bb: AxisAlignedBB, motionX: Double, motionZ: Double, multiplier: Double, realY: Double = -0.8): Double {
        var y = realY
        val offset = bb.offset(motionX * multiplier, -0.08, motionZ * multiplier)
        val abb = bb.offset(motionX * multiplier, 0.0, motionZ * multiplier)

        for (box in world.getCollidingBoundingBoxes(mc.thePlayer, offset)) {
            y = box.calculateYOffset(abb, y)
        }

        return y
    }

    private fun modifyVec(original: Vec3, direction: EnumFacing, pos: Vec3): Vec3 {
        val x = original.xCoord
        val y = original.yCoord
        val z = original.zCoord

        val side = direction.opposite

        return when (side.axis ?: return original) {
            EnumFacing.Axis.Y -> Vec3(x, pos.yCoord + side.directionVec.y.coerceAtLeast(0), z)
            EnumFacing.Axis.X -> Vec3(pos.xCoord + side.directionVec.x.coerceAtLeast(0), y, z)
            EnumFacing.Axis.Z -> Vec3(x, y, pos.zCoord + side.directionVec.z.coerceAtLeast(0))
        }
    }

    private fun getPlacePossibility(): BlockPos? {
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

        return BlockPos(
            possibilities.minByOrNull {
                val x = mc.thePlayer.posX - it.xCoord
                val y = mc.thePlayer.posY - 1 - it.yCoord
                val z = mc.thePlayer.posZ - it.zCoord
                sqrt(x * x + y * y + z * z)
            } ?: return null
        )
    }

    private fun offset(x: Int, y: Int, z: Int): Vec3{
        return Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z)
    }

    /**
     * Checks if the player is near a block
     *
     * @return block near
     */
    private fun blockNear(range: Int): Boolean {
        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    if (blockRelativeToPlayer(x, y, z) !is BlockAir) return true
                }
            }
        }

        return false
    }

    /**
     * Gets the block relative to the player from the offset
     *
     * @return block relative to the player
     */
    private fun blockRelativeToPlayer(offsetX: Int, offsetY: Int, offsetZ: Int): Block {
        return mc.theWorld.getBlockState(BlockPos(mc.thePlayer).add(offsetX, offsetY, offsetZ)).block
    }

    /**
     * Checks if there is a block under the player
     *
     * @return block under
     */
    private fun isBlockUnder(height: Double, boundingBox: Boolean): Boolean {
        if (boundingBox) {
            var offset = 0
            while (offset < height) {
                val bb = mc.thePlayer.entityBoundingBox.offset(0.0, -offset.toDouble(), 0.0)

                if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isNotEmpty()) {
                    return true
                }
                offset += 2
            }
        } else {
            var offset = 0
            while (offset < height) {
                if (blockRelativeToPlayer(0, -offset, 0).isFullBlock) {
                    return true
                }
                offset++
            }
        }
        return false
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        // Boing Boing
        val bb = mc.thePlayer.entityBoundingBox.offset(0.0, -0.5, 0.0).expand(-0.001, 0.0, -0.001)
        if (
            jumpAutomatically.get()
            && isMoving
            && mc.thePlayer.onGround
            && !mc.thePlayer.isSneaking
            && !mc.gameSettings.keyBindSneak.isKeyDown
            && !mc.gameSettings.keyBindJump.isKeyDown &&
            mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()
        ){
            mc.thePlayer.jump()
        }

        if(!movementCorrection.get()){
            MovementUtils.useDiagonalSpeed()
        }
    }

    @EventTarget
    fun onInput(event: MoveInputEvent){
        val blockSlot = InventoryUtils.findBlockInHotbar() ?: return
        if (blockSlot != -1) {
            if (safeWalk.get() && mc.thePlayer.onGround && mc.theWorld.getCollidingBoundingBoxes(
                    mc.thePlayer, mc.thePlayer.entityBoundingBox.addCoord(
                        mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ
                    ).expand(-0.175, 0.0, -0.175)
                ).isEmpty()
            ) {
                event.sneak = true
            }

            if (
                modes.get().equals("telly", true) &&
                mc.thePlayer.onGround &&
                isMoving &&
                if(reset.get()) !RotationUtils.active else true
            ) {
                event.jump = true
            }

            when (adStrafe.get().lowercase()) {
                "always" -> {
                    if (adStrafeTimer.hasTimeElapsed(125.0, true)) {
                        adStrafeDirection = !adStrafeDirection
                    }

                    if (isMoving && !mc.gameSettings.keyBindLeft.isKeyDown && !mc.gameSettings.keyBindRight.isKeyDown) {
                        event.strafe = if(adStrafeDirection) 1.0F else -1.0F
                    }
                }
                "edge" -> {
                    val b = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.5, mc.thePlayer.posZ)
                    if(b.getBlock() is BlockAir &&
                        mc.currentScreen == null &&
                        !mc.gameSettings.keyBindJump.isKeyDown &&
                        event.forward != 0.0F
                    ){
                        when (
                            EnumFacing.getHorizontal(
                                MathHelper.floor_double(
                                    (targetYaw * 4.0f / 360.0f).toDouble() + 0.5) and 3
                            )
                        ){
                            EnumFacing.EAST -> event.strafe = if(b.z + 0.5 > mc.thePlayer.posZ) 1.0f else -1.0f
                            EnumFacing.WEST -> event.strafe = if(b.z + 0.5 < mc.thePlayer.posZ) 1.0f else -1.0f
                            EnumFacing.SOUTH -> event.strafe = if(b.x + 0.5 < mc.thePlayer.posX) 1.0f else -1.0f
                            else -> event.strafe = if(b.x + 0.5 > mc.thePlayer.posX) 1.0f else -1.0f
                        }
                    }
                }
            }
        }
    }

    /**
     *  @return block relative to the player
     */
    private fun calculateRotations() {
        /* Falling prediction */
        val c1 = calculateRealY(mc.theWorld, bb, motionX, motionZ, 0.546) <= -0.015625
        val a1 = calculateRealY(mc.theWorld, bb, motionX, motionZ, 1.0)
        val c2 = a1 <= -0.015625
        val c3 = calculateRealY(mc.theWorld, bb, motionX, motionZ, 1.09, a1) <= -0.015625

        if (predict.get() && !(c1 && c2 && c3 && mc.thePlayer.onGround)){
            setRotation()
            return
        }

        val blockPlace = blockPlace ?: return
        val enumFacing = enumFacing ?: return

        when (modes.get().lowercase()) {
            "staticgod" -> {
                targetYaw = mc.thePlayer.rotationYaw - 45
                targetPitch = 75.6F
            }
            "snap" -> {
                getRotations()

                if (ticksOnAir <= 0 || isObjectMouseOverBlock(enumFacing, blockPlace)) {
                    if (reset.get()) {
                        RotationUtils.active = false
                        return
                    } else {
                        targetYaw = mc.thePlayer.rotationYaw
                    }
                }
            }

            "legit" -> {
                /* Player Position Update (Edge Exception + Legit Mode) */
                xPos = mc.thePlayer.posX
                zPos = mc.thePlayer.posZ

                if (!this.buildForward()) {
                    xPos += mc.thePlayer.posX - lastX
                    zPos += mc.thePlayer.posZ - lastZ
                }

                lastX = mc.thePlayer.posX
                lastZ = mc.thePlayer.posZ

                val miX = blockPlace.x - 0.288
                val maX = blockPlace.x + 1.288

                val miZ = blockPlace.z - 0.288
                val maZ = blockPlace.z + 1.288

                if (ticksOnAir > 0 && !isObjectMouseOverBlock(enumFacing, blockPlace)) {
                    if (xPos !in miX..maX || zPos !in miZ..maZ) getRotations()
                }
            }

            "telly" -> {
                if (RotationUtils.offGroundTicks >= ticks.get()) {
                    if (!isObjectMouseOverBlock(enumFacing, blockPlace)) {
                        getRotations()
                    }
                } else if (isMoving) {
                    if (reset.get()) {
                        RotationUtils.active = false
                        return
                    } else {
                        getRotations()
                        targetYaw = mc.thePlayer.rotationYaw
                    }
                }
            }

            else -> if (ticksOnAir > 0 && !isObjectMouseOverBlock(enumFacing, blockPlace)) {
                getRotations()
            }
        }

        setRotation()
    }

    /* Setting rotations */
    private fun setRotation(){
        RotationUtils.setRotations(
            Rotation(targetYaw, targetPitch),
            2,
            RandomUtils.nextFloat(speed.getMinValue(), speed.getMaxValue()),
            if (movementCorrection.get() && !modes.get().equals("none", true)) MovementFixType.FULL
            else MovementFixType.NONE,
            true,
        )
    }

    private fun getEnumFacing(position: Vec3): EnumFacingOffset? {
        for (x2 in -1..1 step 2) {
            if (PlayerUtils.block(position.xCoord + x2, position.yCoord, position.zCoord) !is BlockAir) {
                return if (x2 > 0) {
                    EnumFacingOffset(EnumFacing.WEST, Vec3(x2.toDouble(), 0.0, 0.0))
                } else {
                    EnumFacingOffset(EnumFacing.EAST, Vec3(x2.toDouble(), 0.0, 0.0))
                }
            }
        }

        for (y2 in -1..1 step 2) {
            if (PlayerUtils.block(position.xCoord, position.yCoord + y2, position.zCoord) !is BlockAir) {
                if (y2 < 0) {
                    return EnumFacingOffset(EnumFacing.UP, Vec3(0.0, y2.toDouble(), 0.0))
                }
            }
        }

        for (z2 in -1..1 step 2) {
            if (PlayerUtils.block(position.xCoord, position.yCoord, position.zCoord + z2) !is BlockAir) {
                return if (z2 < 0) {
                    EnumFacingOffset(EnumFacing.SOUTH, Vec3(0.0, 0.0, z2.toDouble()))
                } else {
                    EnumFacingOffset(EnumFacing.NORTH, Vec3(0.0, 0.0, z2.toDouble()))
                }
            }
        }

        return null
    }

    data class EnumFacingOffset(var enumFacing: EnumFacing, val offset: Vec3)

    /**
     * @author fmcpe
     *
     * 6/21/2024
     * Raytrace
     * From MCP
     */
    @JvmOverloads
    fun isObjectMouseOverBlock(
        facing: EnumFacing,
        block: BlockPos,
        rotation: Rotation? = null,
        obj: MovingObjectPosition? = rayTrace(rotation),
    ): Boolean{
        if (obj != null) {
            return obj.blockPos == block && obj.sideHit == facing
        }

        return false
    }

    private fun buildForward(realYaw: Float = MathHelper.wrapAngleTo180_float(currentRotation.yaw)): Boolean {
        return (realYaw in -77.5..77.5) && (realYaw in -102.5..102.5 || realYaw in -12.5..12.5 || realYaw < -167.5 || realYaw > 167.5)
    }

    private fun getRotations(){
        val eyes = mc.thePlayer.eyes
        var placeRotation: PlaceRotation? = null
        val blockPos = targetBlock ?: return
        val neighborBlock = blockPlace ?: return

        when (searchModeValue.get().lowercase()) {
            "area" -> for (x in 0.1..0.9) {
                for (y in 0.1..0.9) {
                    for (z in 0.1..0.9) {
                        val currentPlaceRotation = findTargetPlace(blockPos, neighborBlock, Vec3(x, y, z), side ?: return, eyes, maxReach.toFloat()) ?: continue

                        if (placeRotation == null || getRotationDifference(currentPlaceRotation.rotation, currentRotation) < getRotationDifference(placeRotation.rotation, currentRotation))
                            placeRotation = currentPlaceRotation
                    }
                }
            }

            "center" -> {
                placeRotation = findTargetPlace(blockPos, neighborBlock, Vec3(0.5, 0.5, 0.5), side ?: return, eyes, maxReach.toFloat()) ?: return
            }

            "tryrotation" -> {
                val possibleYaw = when (yawOffset.get()){
                    "Dynamic" -> doubleArrayOf(0.0, 45.0, 135.0, 180.0)
                    else -> doubleArrayOf(45.0, 135.0)
                }

                val yawList = mutableListOf<Double>()

                for (i in possibleYaw){
                    yawList.add(mc.thePlayer.rotationYaw - i)
                    yawList.add(mc.thePlayer.rotationYaw + i)
                }

                for (yaw in yawList) {
                    for (pitch in -90.0..-30.0) {
                        val rotation = Rotation(yaw.toFloat(), -pitch.toFloat())
                        val raytrace = rayTrace(rotation)!!

                        val currentPlaceRotation =
                            PlaceRotation(PlaceInfo(raytrace.blockPos, raytrace.sideHit, raytrace.hitVec), rotation)

                        if (isObjectMouseOverBlock(enumFacing ?: return, blockPlace ?: return, obj = raytrace)) {
                            if (placeRotation == null || getRotationDifference(
                                    currentPlaceRotation.rotation,
                                    currentRotation
                                ) < getRotationDifference(placeRotation.rotation, currentRotation)
                            ) {
                                placeRotation = currentPlaceRotation
                            }
                        }
                    }
                }
            }
        }

        targetYaw = placeRotation?.rotation?.yaw ?: return
        targetPitch = placeRotation.rotation.pitch
    }

    private fun findTargetPlace(blockPos: BlockPos, neighborBlock: BlockPos, vec3: Vec3, side: EnumFacing, eyes: Vec3, maxReach: Float): PlaceRotation? {
        mc.theWorld ?: return null

        val vec = (Vec3(blockPos) + vec3).addVector(
            side.directionVec.x * vec3.xCoord, side.directionVec.y  * vec3.yCoord, side.directionVec.z * vec3.zCoord
        )

        val distance = eyes.distanceTo(vec)

        if (distance > maxReach || mc.theWorld.rayTraceBlocks(eyes, vec, false, true, false) != null)
            return null

        val rotation = RotationUtils.toRotation(vec, false)
        val raytrace = rayTrace(rotation) ?: return null

        return if (raytrace.blockPos == neighborBlock && raytrace.sideHit == side.opposite) {
            val placeInfo = PlaceInfo(neighborBlock, side.opposite, modifyVec(raytrace.hitVec, side.opposite, Vec3(neighborBlock)))
            PlaceRotation(placeInfo, rotation)
        } else null
    }

    /**
     * Render counter
     */
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        progress = (System.currentTimeMillis() - lastMS).toFloat() / 100F
        if (progress >= 1) progress = 1f

        val scaledResolution = ScaledResolution(mc)
        val info = "$blocksAmount Blocks"
        val infoWidth = Fonts.fontSFUI40.getStringWidth(info)

        if(counter.get()){
            GlStateManager.translate(0.0, (-14F - (progress * 4F)).toDouble(), 0.0)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glColor4f(0.15F, 0.15F, 0.15F, progress)
            GL11.glBegin(GL11.GL_TRIANGLE_FAN)
            GL11.glVertex2d((scaledResolution.scaledWidth / 2 - 3).toDouble(), (scaledResolution.scaledHeight - 60).toDouble())
            GL11.glVertex2d((scaledResolution.scaledWidth / 2).toDouble(), (scaledResolution.scaledHeight - 57).toDouble())
            GL11.glVertex2d((scaledResolution.scaledWidth / 2 + 3).toDouble(), (scaledResolution.scaledHeight - 60).toDouble())
            GL11.glEnd()
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_BLEND)
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
            RenderUtils.drawRoundedRect((scaledResolution.scaledWidth / 2 - (infoWidth / 2) - 4).toFloat(), (scaledResolution.scaledHeight - 60).toFloat(), (scaledResolution.scaledWidth / 2 + (infoWidth / 2) + 4).toFloat(), (scaledResolution.scaledHeight - 74).toFloat(), 2F, Color(0.15F, 0.15F, 0.15F, progress).rgb)
            GlStateManager.resetColor()
            Fonts.fontSFUI35.drawCenteredString(info, (scaledResolution.scaledWidth / 2).toFloat() + 0.1F, (scaledResolution.scaledHeight - 70).toFloat(), Color(1F, 1F, 1F, 0.8F * progress).rgb, false)
            GlStateManager.translate(0.0, (14F + (progress * 4F)).toDouble(), 0.0)
        }
    }

    /**
     * @return hotbar blocks amount
     */
    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack
                if (itemStack != null && itemStack.item is ItemBlock) {
                    val block = (itemStack.item as ItemBlock).getBlock()
                    if (!BLOCK_BLACKLIST.contains(block) && block.isFullCube) amount += itemStack.stackSize
                }
            }
            return amount
        }

    private val currentRotation: Rotation
        get() = RotationUtils.targetRotation ?: Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)

    private val isLookingDiagonally: Boolean
        get() {
            // Round the rotation to the nearest multiple of 45 degrees so that way we check if the player faces diagonally
            val yaw = round(abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw)).roundToInt() / 45f) * 45f
            return (yaw == 45f || yaw == 135f) && isMoving
        }
    private val maxReach: Double
        get() = mc.playerController.blockReachDistance.toDouble()

    /**
     * @return sameY
     */
    private val sameY: Boolean
        get() = !sameYValue.get().equals("Off", true) && !mc.gameSettings.keyBindJump.isKeyDown && isMoving
}

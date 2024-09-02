package net.minusmc.minusbounce.features.module.modules.world

import net.minecraft.block.*
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.util.*
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.combat.KillAura
import net.minusmc.minusbounce.injection.access.StaticStorage
import net.minusmc.minusbounce.injection.implementations.IEntityPlayerSP
import net.minusmc.minusbounce.ui.font.Fonts
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.InventoryUtils.BLOCK_BLACKLIST
import net.minusmc.minusbounce.utils.InventoryUtils.invalidBlocks
import net.minusmc.minusbounce.utils.MovementUtils.isMoving
import net.minusmc.minusbounce.utils.RotationUtils.getRotationDifference
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
    private val modes = ListValue("Mode", arrayOf("Normal", "Snap", "Telly", "Legit"), "Normal")
    private val searchModeValue = ListValue("AimMode", arrayOf("Area", "Center", "TryRotation"), "Center")
    private val yawOffset = ListValue("OffsetYaw", arrayOf("Dynamic", "Side"), "Dynamic") { searchModeValue.get() == "TryRotation" }
    private val reset = BoolValue("RotationActivateReset", false) { modes.get() == "Telly" || modes.get() == "Snap" }
    private val rayTrace = ListValue("RayTraceMethod", arrayOf("Calculate", "Client", "None"), "Client")
    private val ticks = IntegerValue("Ticks", 3, 0, 10) { modes.get() == "Telly" }
    private val delayValue = IntRangeValue("Delay", 0, 0, 0, 10)
    private val sprint = ListValue("Sprint", arrayOf("Normal", "Legit", "VulcanToggle", "Omni", "Off"), "Normal")
    private val noExpand = BoolValue("NoExpand", true)

    private val startSneak = BoolValue("StartSneak", true)
    private val speed = FloatRangeValue("Speed", 90f, 90f, 0f, 180f)

    private val eagleValue = ListValue("Eagle", arrayOf("Off", "Normal"), "Off")
    private val eagleEdgeDistanceValue = FloatRangeValue("EagleEdgeDistance", 0f, 0f, 0f, 0.2f) { !eagleValue.get().equals("off", true) }
    private val eagleBlocksValue = IntegerValue("EagleBlocks", 0, 0, 10) { eagleValue.get().equals("normal", true) }
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
    private var blockPlace: BlockPos? = null
    private var side: EnumFacing? = null
    private var enumFacing: EnumFacing? = null

    private var adStrafeDirection: Boolean = false
    private val startTime = MSTimer()
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
        targetYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - 180F
        targetPitch = 82F
        startY = floor(mc.thePlayer.posY)
        lastMS = System.currentTimeMillis()
        startTime.reset()
    }

    /* Init */
    private fun calculateSneaking() {
        if (eagleValue.get() == "Normal" && mc.thePlayer.onGround) {
            var dif = 0.5
            val edge = RandomUtils.nextFloat(eagleEdgeDistanceValue.getMinValue(), eagleEdgeDistanceValue.getMaxValue())
            val blockPos = BlockPos(mc.thePlayer).offset(EnumFacing.DOWN)

            if (edge > 0.0F) {
                for (facingType in StaticStorage.facings()) {
                    if (facingType == EnumFacing.UP || facingType == EnumFacing.DOWN) continue

                    val placeInfo = blockPos.offset(facingType)
                    if (blockPos.getBlock() is BlockAir) {
                        var calcDif = if (facingType == EnumFacing.NORTH || facingType == EnumFacing.SOUTH) {
                            abs(placeInfo.z + 0.5 - mc.thePlayer.posZ)
                        } else {
                            abs(placeInfo.x + 0.5 - mc.thePlayer.posX)
                        }

                        calcDif -= 0.5

                        if (calcDif < dif) {
                            dif = calcDif
                        }
                    }
                }
            }

            if (placedBlocksWithoutEagle >= eagleBlocksValue.get()) {
                val shouldEagle = blockPos.getBlock() is BlockAir || (edge > 0 && dif < edge)

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

    @EventTarget(priority = -5)
    fun onTick(event: EventTick){
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
        if (BlockPos(mc.thePlayer).offset(EnumFacing.DOWN).getBlock() is BlockAir) {
            ticksOnAir++
        } else {
            ticksOnAir = 0
        }

        val blockSlot = InventoryUtils.findBlockInHotbar() ?: return
        if (blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = blockSlot - 36
        }

        blockPlace = this.getPlacePossibility() ?: blockPlace
        enumFacing = this.getPlaceSide(blockPlace ?: return) ?: enumFacing
        side = enumFacing?.opposite ?: return
        targetBlock = blockPlace?.offset(enumFacing ?: return)

        if((mc.thePlayer.motionX != 0.0 || mc.thePlayer.motionZ != 0.0) && startTime.hasTimePassed(200)){
            if(mc.thePlayer.motionX == 0.0 && mc.thePlayer.motionY == 0.0){
                startTime.reset()
            }
        }

        calculateRotations()

        setRotation()

        calculateSneaking()

        if (sameYValue.get().equals("AutoJump", true)) {
            if(mc.thePlayer.onGround && (isMoving || mc.gameSettings.keyBindJump.isPressed)){
                mc.thePlayer.jump()
            }
        }

        if (startY - 1 != floor(targetBlock?.y?.toDouble() ?: return) && sameY) {
            return
        }

        val (yaw, pitch) = RotationUtils.targetRotation ?: return

        val eyes = mc.thePlayer.eyes
        val rotX = yaw * Math.PI / 180f
        val rotY = pitch * Math.PI / 180f
        val look = Vec3(-cos(rotY) * sin(rotX), -sin(rotY), cos(rotY) * cos(rotX))
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

        if (!BadPacketUtils.bad(slot = false, attack = true, swing = false, block = false, inventory = true) &&
            ticksOnAir >= RandomUtils.nextInt(delayValue.getMinValue(), delayValue.getMaxValue()) &&
            ticksOnAir > if(noExpand.get()) 0 else { -1 } &&
            when(this.rayTrace.get().lowercase()){
                "calculate" -> rayTrace.sideHit == enumFacing
                else -> rayTrace.blockPos == blockPlace && rayTrace.sideHit == enumFacing
            } &&
            mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
            mc.theWorld.getBlockState(blockPlace).block.material != Material.air
        ){
            val itemstack = mc.thePlayer.inventory.getCurrentItem()
            if (mc.playerController.onPlayerRightClick(
                    mc.thePlayer,
                    mc.theWorld, itemstack,
                    blockPlace,
                    enumFacing,
                    rayTrace.hitVec
                )
            ) {
                mc.netHandler.addToSendQueue(C0APacketAnimation())
            }

            if (itemstack.stackSize == 0) {
                mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = null
            }

            mc.sendClickBlockToController(mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown && mc.inGameHasFocus)
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
        val playerPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ)
        val positions = ArrayList<Vec3>()
        val hashMap = HashMap<Vec3, BlockPos>()

        for (x in playerPos.x - 5..playerPos.x + 5) {
            for (y in playerPos.y - 1..playerPos.y) {
                for (z in playerPos.z - 5..playerPos.z + 5) {
                    if (isValidBock(BlockPos(x, y, z))) {
                        val blockPos = BlockPos(x, y, z)
                        val block = mc.theWorld.getBlockState(blockPos).block
                        val ex = MathHelper.clamp_double(
                            mc.thePlayer.posX, blockPos.x.toDouble(),
                            blockPos.x + block.blockBoundsMaxX
                        )
                        val ey = MathHelper.clamp_double(
                            mc.thePlayer.posY, blockPos.y.toDouble(),
                            blockPos.y + block.blockBoundsMaxY
                        )
                        val ez = MathHelper.clamp_double(
                            mc.thePlayer.posZ, blockPos.z.toDouble(),
                            blockPos.z + block.blockBoundsMaxZ
                        )
                        val vec3 = Vec3(ex, ey, ez)
                        positions.add(vec3)
                        hashMap[vec3] = blockPos
                    }
                }
            }
        }
        return hashMap[positions.minByOrNull { mc.thePlayer.getDistanceSq(it.xCoord, it.yCoord, it.zCoord) }]
    }

    private fun isValidBock(blockPos: BlockPos?): Boolean {
        val block = Minecraft.getMinecraft().theWorld.getBlockState(blockPos).block
        return (block !is BlockLiquid && block !is BlockAir && block !is BlockChest
                && block !is BlockFurnace)
    }

    private fun isPosSolid(pos: BlockPos?): Boolean {
        val block = Minecraft.getMinecraft().theWorld.getBlockState(pos).block
        return ((block.material.isSolid || !block.isTranslucent || block is BlockLadder
                || block is BlockCarpet || block is BlockSnow || block is BlockSkull)
                && !block.material.isLiquid && block !is BlockContainer)
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

            if(startSneak.get() && !startTime.hasTimePassed(200L) && mc.thePlayer.posX != 0.0 && mc.thePlayer.posZ != 0.0){
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

    @EventTarget(priority = -10)
    fun corrention(e: MoveInputEvent){
        val forward = e.forward
        val strafe = e.strafe
        if(movementCorrection.get() && RotationUtils.active){
            val yaw = RotationUtils.targetRotation?.yaw ?: return

            val r = Math.toRadians((mc.thePlayer.rotationYaw - yaw).toDouble()).toFloat()
            e.forward = round(forward * cos(r) + strafe * sin(r)) + 0.0F
            e.strafe = round(strafe * cos(r) - forward * sin(r)) + 0.0F
        }
    }

    @EventTarget(priority = -10)
    fun corrention(e: StrafeEvent){
        if(movementCorrection.get() && RotationUtils.active){
            e.yaw = RotationUtils.targetRotation?.yaw ?: return
        }
    }

    @EventTarget(priority = -10)
    fun corrention(e: JumpEvent){
        if(movementCorrection.get() && RotationUtils.active){
            e.yaw = RotationUtils.targetRotation?.yaw ?: return
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

        val killAura = MinusBounce.moduleManager[KillAura::class.java]

        if(killAura != null && killAura.state && killAura.target != null){
            return
        }

        val blockPlace = blockPlace ?: return
        val enumFacing = enumFacing ?: return

        when (modes.get().lowercase()) {
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

                val miX = blockPlace.x - 0.28
                val maX = blockPlace.x + 1.28

                val miZ = blockPlace.z - 0.28
                val maZ = blockPlace.z + 1.28

                if (xPos !in miX..maX || zPos !in miZ..maZ) getRotations()
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
    }

    /* Setting rotations */
    private fun setRotation(){
        RotationUtils.setRotations(
            Rotation(targetYaw, targetPitch),
            speed = RandomUtils.nextFloat(speed.getMinValue(), speed.getMaxValue()),
            fixType = MovementFixType.NONE,
            silent = true
        )
    }

    private fun getPlaceSide(blockPos: BlockPos): EnumFacing? {
        val positions = ArrayList<Vec3>()
        val hashMap = HashMap<Vec3, EnumFacing>()
        val playerPos = BlockPos(mc.thePlayer)
        if (!isPosSolid(blockPos.add(0, 1, 0)) && !blockPos.add(0, 1, 0).equalsBlockPos(playerPos)
        ) {
            val vec4 = this.getBestHitFeet(blockPos.add(0, 1, 0))
            positions.add(vec4)
            hashMap[vec4] = EnumFacing.UP
        }
        if (!isPosSolid(blockPos.add(0, -1, 0)) && !blockPos.add(0, -1, 0).equalsBlockPos(playerPos)
        ) {
            val vec4 = this.getBestHitFeet(blockPos.add(0, -1, 0))
            positions.add(vec4)
            hashMap[vec4] = EnumFacing.DOWN
        }
        if (!isPosSolid(blockPos.add(1, 0, 0)) && !blockPos.add(1, 0, 0).equalsBlockPos(playerPos)) {
            val vec4 = this.getBestHitFeet(blockPos.add(1, 0, 0))
            positions.add(vec4)
            hashMap[vec4] = EnumFacing.EAST
        }
        if (!isPosSolid(blockPos.add(-1, 0, 0)) && !blockPos.add(-1, 0, 0).equalsBlockPos(playerPos)) {
            val vec4 = this.getBestHitFeet(blockPos.add(-1, 0, 0))
            positions.add(vec4)
            hashMap[vec4] = EnumFacing.WEST
        }
        if (!isPosSolid(blockPos.add(0, 0, 1)) && !blockPos.add(0, 0, 1).equalsBlockPos(playerPos)) {
            val vec4 = this.getBestHitFeet(blockPos.add(0, 0, 1))
            positions.add(vec4)
            hashMap[vec4] = EnumFacing.SOUTH
        }
        if (!isPosSolid(blockPos.add(0, 0, -1)) && !blockPos.add(0, 0, -1).equalsBlockPos(playerPos)) {
            val vec4 = this.getBestHitFeet(blockPos.add(0, 0, -1))
            positions.add(vec4)
            hashMap[vec4] = EnumFacing.NORTH
        }

        if (positions.isNotEmpty()) {
            positions.sortBy { mc.thePlayer.getDistance(it.xCoord, it.yCoord, it.zCoord) }

            val vec5 = this.getBestHitFeet(blockPos)
            if (mc.thePlayer.getDistance(vec5.xCoord, vec5.yCoord, vec5.zCoord) >= mc.thePlayer.getDistance(positions[0].xCoord, positions[0].yCoord, positions[0].zCoord)
            ) {
                return hashMap[positions[0]]
            }
        }

        return null
    }

    private fun getBestHitFeet(blockPos: BlockPos): Vec3 {
        val block = mc.theWorld.getBlockState(blockPos).block
        val ex = MathHelper.clamp_double(
            mc.thePlayer.posX, blockPos.x.toDouble(),
            blockPos.x + block.blockBoundsMaxX
        )
        val ey = MathHelper.clamp_double(
            mc.thePlayer.posY, blockPos.y.toDouble(),
            blockPos.y + block.blockBoundsMaxY
        )
        val ez = MathHelper.clamp_double(
            mc.thePlayer.posZ, blockPos.z.toDouble(),
            blockPos.z + block.blockBoundsMaxZ
        )
        return Vec3(ex, ey, ez)
    }

    private fun BlockPos.equalsBlockPos(blockPos: BlockPos): Boolean {
        return this.x == blockPos.x && (this.y == blockPos.y) && (this.z == blockPos.z)
    }

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
        rotation: Rotation? = currentRotation,
        obj: MovingObjectPosition? = rayTrace(rotation),
    ): Boolean{
        if (obj != null) {
            return obj.blockPos == block && obj.sideHit == facing
        }

        return false
    }

    private fun buildForward(): Boolean {
        val realYaw = MathHelper.wrapAngleTo180_float(currentRotation.yaw)
        return (realYaw > 77.5 && realYaw < 102.5) || (realYaw > 167.5 || realYaw < -167.0) || (realYaw < -77.5 && realYaw > -102.5 || realYaw > -12.5 && realYaw < 12.5)
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
                    "Dynamic" -> arrayOf(0.0, 45.0, 135.0, 180.0)
                    else -> arrayOf(45.0, 135.0)
                }

                val playerPosition = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.5, mc.thePlayer.posZ)

                val rotationList = mutableListOf<PlaceRotation>()

                for (yaw in possibleYaw.flatMap {yaw -> (-1..1 step 2).map { mc.thePlayer.rotationYaw + yaw * it } }) {
                    for (pitch in max(currentRotation.pitch - 50.0, -90.0)..min(currentRotation.pitch + 50.0, 90.0) step 0.05) {
                        val rotation = Rotation(yaw.toFloat(), pitch.toFloat())
                        rotation.fixedSensitivity()

                        val eye = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)
                        val look = RotationUtils.getVectorForRotation(rotation)
                        val vec = eye + (look * mc.playerController.blockReachDistance.toDouble())
                        val hitBlock = mc.theWorld.rayTraceBlocks(eye, vec, false, false, true)
                        val currentPlaceRotation = PlaceRotation(PlaceInfo(hitBlock.blockPos, hitBlock.sideHit, hitBlock.hitVec), rotation)

                        if (hitBlock.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                            && eye.distanceTo(currentPlaceRotation.placeInfo.vec3) < maxReach
                            && mc.theWorld.rayTraceBlocks(eye, currentPlaceRotation.placeInfo.vec3, false, true, false) == null
                            && isValidBock(hitBlock.blockPos)
                            && !rotationList.contains(currentPlaceRotation)
                            && hitBlock.blockPos.equalsBlockPos(blockPlace ?: continue)
                            && hitBlock.sideHit.axis != EnumFacing.Axis.Y
                            && hitBlock.blockPos.y <= playerPosition.y
                        ) {
                            rotationList.add(currentPlaceRotation)
                        }
                    }
                }

                placeRotation = rotationList.minByOrNull {
                    val d = getRotationDifference(it.rotation, currentRotation)
                    val x = it.placeInfo.vec3.xCoord - it.placeInfo.blockPos.x + 0.5
                    val y = it.placeInfo.vec3.yCoord - it.placeInfo.blockPos.y + 0.5
                    val z = it.placeInfo.vec3.zCoord - it.placeInfo.blockPos.z + 0.5
                    sqrt(d + x * x + y * y + z * z)
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
                    amount += if (!invalidBlocks.contains(block)) itemStack.stackSize else 0
                }
            }
            return amount
        }

    private val currentRotation: Rotation
        get() = RotationUtils.targetRotation ?: Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)

    private val maxReach: Double
        get() = mc.playerController.blockReachDistance.toDouble()

    /**
     * @return sameY
     */
    private val sameY: Boolean
        get() = !sameYValue.get().equals("Off", true) && !mc.gameSettings.keyBindJump.isKeyDown && isMoving
}

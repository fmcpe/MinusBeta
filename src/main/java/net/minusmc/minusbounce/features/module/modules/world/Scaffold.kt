/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 *
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 */
package net.minusmc.minusbounce.features.module.modules.world

import net.minecraft.block.BlockBush
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks.air
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.stats.StatList
import net.minecraft.util.*
import net.minecraft.world.WorldSettings
import net.minecraftforge.event.ForgeEventFactory
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.features.module.Module
import net.minusmc.minusbounce.features.module.ModuleCategory
import net.minusmc.minusbounce.features.module.ModuleInfo
import net.minusmc.minusbounce.features.module.modules.movement.Fly
import net.minusmc.minusbounce.features.module.modules.movement.Speed
import net.minusmc.minusbounce.ui.font.Fonts
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.InventoryUtils.Companion.CLICK_TIMER
import net.minusmc.minusbounce.utils.PacketUtils.sendPacket
import net.minusmc.minusbounce.utils.RotationUtils.faceBlock
import net.minusmc.minusbounce.utils.RotationUtils.getRotationDifference
import net.minusmc.minusbounce.utils.RotationUtils.getVectorForRotation
import net.minusmc.minusbounce.utils.RotationUtils.setRotations
import net.minusmc.minusbounce.utils.RotationUtils.toRotation
import net.minusmc.minusbounce.utils.block.BlockUtils
import net.minusmc.minusbounce.utils.block.BlockUtils.isReplaceable
import net.minusmc.minusbounce.utils.block.PlaceInfo
import net.minusmc.minusbounce.utils.extensions.*
import net.minusmc.minusbounce.utils.timer.MSTimer
import net.minusmc.minusbounce.utils.timer.TickTimer
import net.minusmc.minusbounce.utils.timer.TimeUtils.Companion.randomDelay
import net.minusmc.minusbounce.value.*
import kotlin.math.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import javax.vecmath.Color3f

@ModuleInfo(
    name = "Scaffold",
    description = "Automatically places blocks beneath your feet.",
    category = ModuleCategory.WORLD,
    keyBind = Keyboard.KEY_I
)
class Scaffold : Module() {
    /**
     * TOWER MODES & SETTINGS
     */

    // TODO: (Scaffold & Tower) Optimize & Improve Code

    // -->

//     private val towerMode = ListValue(
//         "TowerMode",
//         arrayOf(
//             "None",
//             "Jump",
//             "MotionJump",
//             "Motion",
//             "ConstantMotion",
//             "MotionTP",
//             "Packet",
//             "Teleport",
//             "AAC3.3.9",
//             "AAC3.6.4"
//         ),
//         "None"
//     )

//     private val stopWhenBlockAbove = BoolValue("StopWhenBlockAbove", false) { towerMode.get() != "None" }

//     private val onJump = BoolValue("TowerOnJump", true) { towerMode.get() != "None" }
//     private val matrix = BoolValue("TowerMatrix", false) { towerMode.get() != "None" }
//     private val placeMode = ListValue(
//         "TowerPlaceTiming",
//         arrayOf("Pre", "Post"),
//         "Post"
//     ) { towerMode.get() != "Packet" && towerMode.get() != "None" }

//     // Jump mode
//     private val jumpMotion = FloatValue("JumpMotion", 0.42f, 0.3681289f,0.79f) { towerMode.get() == "MotionJump" }
//     private val jumpDelay = IntegerValue("JumpDelay", 0, 0,20) { towerMode.get() == "MotionJump" || towerMode.get() == "Jump" }

//     // ConstantMotion
//     private val constantMotion = FloatValue("ConstantMotion", 0.42f, 0.1f,1f) { towerMode.get() == "ConstantMotion" }
//     private val constantMotionJumpGround = FloatValue(
//         "ConstantMotionJumpGround",
//         0.79f,
//         0.76f,1f
//     ) { towerMode.get() == "ConstantMotion" }
//     private val constantMotionJumpPacket = BoolValue("JumpPacket", true) { towerMode.get() == "ConstantMotion" }

//     // Teleport
//     private val teleportHeight = FloatValue("TeleportHeight", 1.15f, 0.1f,5f) { towerMode.get() == "Teleport" }
//     private val teleportDelay = IntegerValue("TeleportDelay", 0, 0,20) { towerMode.get() == "Teleport" }
//     private val teleportGround = BoolValue("TeleportGround", true) { towerMode.get() == "Teleport" }
//     private val teleportNoMotion = BoolValue("TeleportNoMotion", false) { towerMode.get() == "Teleport" }

//     // <--

//     /**
//      * SCAFFOLD MODES & SETTINGS
//      */

//     // -->

//     private val scaffoldMode = ListValue(
//         "ScaffoldMode",
//         arrayOf("Normal", "Rewinside", "Expand", "Telly", "GodBridge"),
//         "Normal"
//     )

//     // Expand
//     private val omniDirectionalExpand = BoolValue("OmniDirectionalExpand", false) { scaffoldMode.get() == "Expand" }
//     private val expandLength = IntegerValue("ExpandLength", 1, 1,6) { scaffoldMode.get() == "Expand" }

//     // Placeable delay
//     private val placeDelayValue = BoolValue("PlaceDelay", true) { scaffoldMode.get() != "GodBridge" }
//     private val delayValue = IntRangeValue("Delay", 0, 1000, 0, 0) { placeDelayValue.get() }

//     // Autoblock
//     private val autoBlock = ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
//     private val sortByHighestAmount = BoolValue("SortByHighestAmount", false) { autoBlock.get() != "Off" }

//     // Basic stuff
//     val sprint = BoolValue("Sprint", false)
//     private val swing = BoolValue("Swing", true)
//     private val down = BoolValue("Down", true) { scaffoldMode.get() !in arrayOf("GodBridge", "Telly") }

//     private val ticksUntilRotation = IntegerValue("TicksUntilRotation", 3, 1,5, {scaffoldMode.get() == "Telly"})

//     // GodBridge mode subvalues
//     private val useStaticRotation = BoolValue("UseStaticRotation", false) { scaffoldMode.get() == "GodBridge" }
//     private val autoJump = BoolValue("autoJump.get()", true) { scaffoldMode.get() == "GodBridge" }
//     private val jumpAutomatically = BoolValue("JumpAutomatically", true) { scaffoldMode.get() == "GodBridge" && autoJump.get() }
//     private val blocks = IntRangeValue("BlocksToJump", 0, 8, 0, 0) { scaffoldMode.get() == "GodBridge" && !jumpAutomatically.get() && autoJump.get() }

//     // Telly mode subvalues
//     private val startHorizontally = BoolValue("StartHorizontally", true) { scaffoldMode.get() == "Telly" }
//     private val horizontal = IntRangeValue("HorizontalPlacements", 0, 10, 0, 0) { scaffoldMode.get() == "Telly" }
//     private val vertical = IntRangeValue("VerticalPlacements", 0, 10, 0, 0) { scaffoldMode.get() == "Telly" }
//     private val jump = IntRangeValue("HorizontalPlacements", 0, 10, 0, 0) { scaffoldMode.get() == "Telly" }

//     private val allowClutching = BoolValue("AllowClutching", true) { scaffoldMode.get() !in arrayOf("Telly", "Expand") }
//     private val horizontalClutchBlocks = IntegerValue("HorizontalClutchBlocks", 3, 1,5) {allowClutching.get() && scaffoldMode.get() !in arrayOf("Telly", "Expand")}
//     private val verticalClutchBlocks = IntegerValue("VerticalClutchBlocks", 2, 1,3) {allowClutching.get() && scaffoldMode.get() !in arrayOf("Telly", "Expand")}

//     // Eagle
//     private val eagleValue =
//         ListValue("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal") { scaffoldMode.get() != "GodBridge" }
//     private val adjustedSneakSpeed = BoolValue("AdjustedSneakSpeed", true) { eagleValue.get() == "Silent" }
//     private val eagleSpeed = FloatValue("EagleSpeed", 0.3f, 0.3f,1.0f) { eagleValue.get() != "Off" }
//     private val blocksToEagle = IntegerValue("BlocksToEagle", 0, 0,10) { eagleValue.get() != "Off" }
//     private val edgeDistance = FloatValue(
//         "EagleEdgeDistance",
//         0f,
//         0f,0.5f
//     ) { eagleValue.get() != "Off" }

//     // Rotation Options
//     private val rotationMode = ListValue("Rotations", arrayOf("Off", "Normal", "Stabilized", "GodBridge"), "Normal")
//     private val silentRotation = BoolValue("SilentRotation", true) { rotationMode.get() != "Off" }
//     private val strafe = BoolValue("Strafe", false) { rotationMode.get() != "Off" && silentRotation.get() }
//     private val keepRotation = BoolValue("KeepRotation", true) { rotationMode.get() != "Off" && silentRotation.get() }
//     private val keepTicks = IntegerValue("KeepTicks", 1, 1,20) { rotationMode.get() != "Off" && scaffoldMode.get() != "Telly" && silentRotation.get() }

//     // Search options
//     private val searchMode = ListValue("SearchMode", arrayOf("Area", "Center"), "Area") { scaffoldMode.get() != "GodBridge" }
//     private val minDist = FloatValue("MinDist", 0f, 0f,0.2f) { scaffoldMode.get() !in arrayOf("GodBridge", "Telly") }

//     // Turn Speed
//     private val turnSpeed = FloatRangeValue("TurnSpeed", 0f, 180f, 0f, 0f) { rotationMode.get() != "Off" }

//     // Game
//     private val timer = FloatValue("Timer", 1f, 0.1f,10f)
//     private val speedModifier = FloatValue("SpeedModifier", 1f, 0f,2f)

//     // Safety
//     private val sameY = BoolValue("SameY", false) { scaffoldMode.get() != "GodBridge" }
//     private val safeWalkValue = BoolValue("SafeWalk", true) { scaffoldMode.get() != "GodBridge" }
//     private val airSafe = BoolValue("AirSafe", false) { safeWalkValue.get() }

//     // Visuals
//     private val counterDisplay = BoolValue("Counter", true)
//     private val mark = BoolValue("Mark", false)
//     private val safetyLines = BoolValue("SafetyLines", false) { isGodBridgeEnabled }

//     // Target placement
//     private var placeRotation: PlaceRotation? = null

//     // Launch position
//     private var launchY = 0
//     private val shouldKeepLaunchPosition
//         get() = sameY.get() && scaffoldMode.get() != "GodBridge"

//     // Delay
//     private val delayTimer = object : DelayTimer(delayValue.getMinValue(), delayValue.getMaxValue(), MSTimer()) {
//         override fun hasTimePassed() = !placeDelayValue.get() || super.hasTimePassed()
//     }
    
//     // Eagle
//     private var placedBlocksWithoutEagle = 0
//     var eagleSneaking = false
//     private val isEagleEnabled
//         get() = eagleValue.get() != "Off" && !shouldGoDown && scaffoldMode.get() != "GodBridge"

//     // Downwards
//     private val shouldGoDown
//         get() = down.get() && !sameY.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && scaffoldMode.get() !in arrayOf(
//             "GodBridge",
//             "Telly"
//         ) && blocksAmount > 1

//     // Current rotation
//     private val currRotation
//         get() = RotationUtils.targetRotation ?: mc.thePlayer.rotation

//     // GodBridge
//     private var blocksPlacedUntilJump = 0

//     private val isManualJumpOptionActive
//         get() = scaffoldMode.get() == "GodBridge" && !jumpAutomatically.get()

//     private var blocksToJump = randomDelay(blocks.getMinValue(), blocks.getMaxValue())

//     private val isGodBridgeEnabled
//         get() = scaffoldMode.get() == "GodBridge" || scaffoldMode.get() == "Normal" && rotationMode.get() == "GodBridge"

//     private val isLookingDiagonally: Boolean
//         get() {
//             val player = mc.thePlayer ?: return false

//             // Round the rotation to the nearest multiple of 45 degrees so that way we check if the player faces diagonally
//             val yaw = round(abs(MathHelper.wrapAngleTo180_float(player.rotationYaw)).roundToInt() / 45f) * 45f

//             return floatArrayOf(
//                 45f,
//                 135f
//             ).any { yaw == it } && player.movementInput.moveForward != 0f && player.movementInput.moveStrafe == 0f
//         }

//     // Telly
//     private var offGroundTicks = 0
//     private var ticksUntilJump = 0
//     private var blocksUntilAxisChange = 0
//     private var jumpTicks = randomDelay(jump.getMinValue(), jump.getMaxValue())
//     private var horizontalPlacements = randomDelay(horizontal.getMinValue(), horizontal.getMaxValue())
//     private var verticalPlacements = randomDelay(vertical.getMinValue(), vertical.getMaxValue())
//     private val shouldPlaceHorizontally
//         get() = scaffoldMode.get() == "Telly" && MovementUtils.isMoving && (startHorizontally.get() && blocksUntilAxisChange <= horizontalPlacements || !startHorizontally.get() && blocksUntilAxisChange > verticalPlacements)

//     // <--

//     // Enabling module
//     override fun onEnable() {
//         val player = mc.thePlayer ?: return

//         launchY = player.posY.roundToInt()
//         blocksUntilAxisChange = 0
//     }

//     /**
//      * TOWER SETTINGS
//      */

//     // Target block
//     private var placeInfo: PlaceInfo? = null

//     // Rotation lock
//     private var lockRotation: Rotation? = null

//     // Mode stuff
//     private val tickTimer = TickTimer()
//     private var jumpGround = 0.0

//     // Events
//     @EventTarget
//     private fun onUpdate(event: UpdateEvent) {
//         val player = mc.thePlayer ?: return

//         if (mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR)
//             return

//         mc.timer.timerSpeed = timer.get()

//         // Telly
//         if (mc.thePlayer.onGround) {
//             offGroundTicks = 0
//             ticksUntilJump++
//         } else {
//             offGroundTicks++
//         }

//         if (shouldGoDown) {
//             mc.gameSettings.keyBindSneak.pressed = false
//         }

//         // Eagle
//         if (isEagleEnabled) {
//             var dif = 0.5
//             val blockPos = BlockPos(player).down()

//             for (side in EnumFacing.values()) {
//                 if (side.axis == EnumFacing.Axis.Y) {
//                     continue
//                 }

//                 val neighbor = blockPos.offset(side)

//                 if (isReplaceable(neighbor)) {
//                     val calcDif = (if (side.axis == EnumFacing.Axis.Z) {
//                         abs(neighbor.z + 0.5 - player.posZ)
//                     } else {
//                         abs(neighbor.x + 0.5 - player.posX)
//                     }) - 0.5

//                     if (calcDif < dif) {
//                         dif = calcDif
//                     }
//                 }
//             }

//             if (placedBlocksWithoutEagle >= blocksToEagle.get()) {
//                 val shouldEagle = isReplaceable(blockPos) || dif < edgeDistance.get()
//                 if (eagleValue.get() == "Silent") {
//                     if (eagleSneaking != shouldEagle) {
//                         sendPacket(
//                             C0BPacketEntityAction(
//                                 player,
//                                 if (shouldEagle) C0BPacketEntityAction.Action.START_SNEAKING else C0BPacketEntityAction.Action.STOP_SNEAKING
//                             ), 
//                             true
//                         )

//                         // Adjust speed when silent sneaking
//                         if (adjustedSneakSpeed.get() && shouldEagle) {
//                             player.motionX *= eagleSpeed.get()
//                             player.motionZ *= eagleSpeed.get()
//                         }
//                     }

//                     eagleSneaking = shouldEagle
//                 } else {
//                     mc.gameSettings.keyBindSneak.pressed = shouldEagle
//                     eagleSneaking = shouldEagle
//                 }
//                 placedBlocksWithoutEagle = 0
//             } else {
//                 placedBlocksWithoutEagle++
//             }
//         }

//         if (player.onGround) {
//             // Still a thing?
//             if (scaffoldMode.get() == "Rewinside") {
//                 MovementUtils.strafe(0.2F)
//                 player.motionY = 0.0
//             }
//         }
//     }

//     @EventTarget
//     fun onStrafe(event: StrafeEvent) {
//         val player = mc.thePlayer

//         // Jumping needs to be done here, so it doesn't get detected = movement-sensitive anti-cheats.
//         if (scaffoldMode.get() == "Telly" && player.onGround && MovementUtils.isMoving && currRotation == player.rotation && ticksUntilJump >= jumpTicks) {
//             player.tryJump()

//             ticksUntilJump = 0
//             jumpTicks = randomDelay(jump.getMinValue(), jump.getMaxValue())
//         }
//     }

//     @EventTarget
//     fun onMotion(event: MotionEvent) {
//         val rotation = RotationUtils.targetRotation

//         if (event.eventState == EventState.POST) {
//             update()

//             if (rotationMode.get() != "Off" && rotation != null) {
//                 // Keep aiming at the target spot even if we have already placed
//                 // Prevents rotation correcting itself after a bit of bridging
//                 // Instead of doing it in the first place.
//                 // Normally a rotation utils recode is needed to rotate regardless of placeRotation being null or not, but whatever.
//                 val placeRotation = this.placeRotation?.rotation ?: rotation

//                 val pitch = if (scaffoldMode.get() == "GodBridge" && useStaticRotation.get()) {
//                     if (placeRotation == this.placeRotation?.rotation) {
//                         if (isLookingDiagonally) 75.6f else 73.5f
//                     } else placeRotation.pitch
//                 } else {
//                     placeRotation.pitch
//                 }

//                 val targetRotation = Rotation(placeRotation.yaw, pitch)
//                 targetRotation.fixedSensitivity()

//                 val ticks = if (keepRotation.get()) {
//                     if (scaffoldMode.get() == "Telly") 1 else keepTicks.get()
//                 } else {
//                     RotationUtils.keepLength
//                 }

//                 if (RotationUtils.keepLength != 0 || keepRotation.get()) {
//                     setRotation(targetRotation, ticks)
//                 }
//             }
//         }

//         /**
//          * TOWER FUNCTION
//          */
//         if (towerMode.get() == "None") return
//         if (onJump.get() && !mc.gameSettings.keyBindJump.isKeyDown) return

//         // Lock Rotation
//         if (keepRotation.get() && lockRotation != null) setRotations(lockRotation!!)

//         mc.timer.timerSpeed = timer.get()
//         val eventState = event.eventState

//         // Force use of POST event when Packet mode is selected, it doesn't work with PRE mode
//         if (eventState.stateName == (if (towerMode.get() == "Packet") "POST" else placeMode.get().uppercase()))
//             placeInfo?.let { place(it) }

//         if (eventState == EventState.PRE) {
//             update()
//             tickTimer.update()

//             if (!stopWhenBlockAbove.get() || BlockUtils.getBlock(BlockPos(mc.thePlayer).up(2)) == air) move()

//             val blockPos = BlockPos(mc.thePlayer).down()
//             if (blockPos.getBlock() == air) {
//                 if (search(blockPos)) {
//                     val vecRotation = faceBlock(blockPos)
//                     if (vecRotation != null) {
//                         setRotations(vecRotation.rotation)
//                         placeInfo!!.vec3 = vecRotation.vec
//                     }
//                 }
//             }
//         }
//     }

//     // TOWER FUNCTION
//     //Send jump packets, bypasses Hypixel.
//     private fun fakeJump() {
//         mc.thePlayer.isAirBorne = true
//         mc.thePlayer.triggerAchievement(StatList.jumpStat)
//     }

//     /**
//      *
//      * TOWER FUNCTION
//      *
//      * Move player
//      */
//     private fun move() {
//         val thePlayer = mc.thePlayer ?: return

//         if (blocksAmount <= 0)
//             return

//         when (towerMode.get().lowercase()) {
//             "jump" -> if (thePlayer.onGround && tickTimer.hasTimePassed(jumpDelay.get())) {
//                 fakeJump()
//                 thePlayer.tryJump()
//             } else if (!thePlayer.onGround) {
//                 thePlayer.isAirBorne = false
//                 tickTimer.reset()
//             }

//             "motion" -> if (thePlayer.onGround) {
//                 fakeJump()
//                 thePlayer.motionY = 0.42
//             } else if (thePlayer.motionY < 0.1) {
//                 thePlayer.motionY = -0.3
//             }

//             // Old Name (Jump)
//             "motionjump" -> if (thePlayer.onGround && tickTimer.hasTimePassed(jumpDelay.get())) {
//                 fakeJump()
//                 thePlayer.motionY = jumpMotion.get().toDouble()
//                 tickTimer.reset()
//             }

//             "motiontp" -> if (thePlayer.onGround) {
//                 fakeJump()
//                 thePlayer.motionY = 0.42
//             } else if (thePlayer.motionY < 0.23) {
//                 thePlayer.setPosition(thePlayer.posX, truncate(thePlayer.posY), thePlayer.posZ)
//             }

//             "packet" -> if (thePlayer.onGround && tickTimer.hasTimePassed(2)) {
//                 fakeJump()
//                 sendPackets(
//                     C04PacketPlayerPosition(
//                         thePlayer.posX,
//                         thePlayer.posY + 0.42,
//                         thePlayer.posZ,
//                         false
//                     ),
//                     C04PacketPlayerPosition(
//                         thePlayer.posX,
//                         thePlayer.posY + 0.753,
//                         thePlayer.posZ,
//                         false
//                     )
//                 )
//                 thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 1.0, thePlayer.posZ)
//                 tickTimer.reset()
//             }

//             "teleport" -> {
//                 if (teleportNoMotion) {
//                     thePlayer.motionY = 0.0
//                 }
//                 if ((thePlayer.onGround || !teleportGround) && tickTimer.hasTimePassed(teleportDelay)) {
//                     fakeJump()
//                     thePlayer.setPositionAndUpdate(
//                         thePlayer.posX, thePlayer.posY + teleportHeight, thePlayer.posZ
//                     )
//                     tickTimer.reset()
//                 }
//             }

//             "constantmotion" -> {
//                 if (thePlayer.onGround) {
//                     if (constantMotionJumpPacket) {
//                         fakeJump()
//                     }
//                     jumpGround = thePlayer.posY
//                     thePlayer.motionY = constantMotion.toDouble()
//                 }
//                 if (thePlayer.posY > jumpGround + constantMotionJumpGround) {
//                     if (constantMotionJumpPacket) {
//                         fakeJump()
//                     }
//                     thePlayer.setPosition(
//                         thePlayer.posX, truncate(thePlayer.posY), thePlayer.posZ
//                     ) // TODO: toInt() required?
//                     thePlayer.motionY = constantMotion.toDouble()
//                     jumpGround = thePlayer.posY
//                 }
//             }

//             "aac3.3.9" -> {
//                 if (thePlayer.onGround) {
//                     fakeJump()
//                     thePlayer.motionY = 0.4001
//                 }
//                 mc.timer.timerSpeed = 1f
//                 if (thePlayer.motionY < 0) {
//                     thePlayer.motionY -= 0.00000945
//                     mc.timer.timerSpeed = 1.6f
//                 }
//             }

//             "aac3.6.4" -> if (thePlayer.ticksExisted % 4 == 1) {
//                 thePlayer.motionY = 0.4195464
//                 thePlayer.setPosition(thePlayer.posX - 0.035, thePlayer.posY, thePlayer.posZ)
//             } else if (thePlayer.ticksExisted % 4 == 0) {
//                 thePlayer.motionY = -0.5
//                 thePlayer.setPosition(thePlayer.posX + 0.035, thePlayer.posY, thePlayer.posZ)
//             }
//         }
//     }

//     /**
//      *
//      * TOWER FUNCTION
//      *
//      * Search for placeable block
//      *
//      * @param blockPosition pos
//      * @return
//      */
//     private fun search(blockPosition: BlockPos): Boolean {
//         val thePlayer = mc.thePlayer ?: return false
//         if (!isReplaceable(blockPosition)) {
//             return false
//         }

//         val eyesPos = thePlayer.eyes
//         var placeRotation: PlaceRotation? = null
//         for (facingType in EnumFacing.values()) {
//             val neighbor = blockPosition.offset(facingType)
//             if (!BlockUtils.isReplaceable(neighbor)) {
//                 continue
//             }
//             val dirVec = Vec3(facingType.directionVec)

//             for (x in 0.1..0.9) {
//                 for (y in 0.1..0.9) {
//                     for (z in 0.1..0.9) {
//                         val posVec = Vec3(blockPosition).addVector(
//                             if (matrix.get()) 0.5 else x, if (matrix.get()) 0.5 else y, if (matrix.get()) 0.5 else z
//                         )

//                         val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
//                         val hitVec = posVec + (dirVec * 0.5)

//                         if (eyesPos.distanceTo(hitVec) > 4.25
//                             || distanceSqPosVec > eyesPos.squareDistanceTo(posVec + dirVec)
//                             || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null
//                         ) continue

//                         // face block
//                         val rotation = toRotation(hitVec, false)

//                         val rotationVector = getVectorForRotation(rotation)
//                         val vector = eyesPos + (rotationVector * 4.25)

//                         val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true) ?: continue

//                         if (obj.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || obj.blockPos != neighbor)
//                             continue

//                         if (placeRotation == null || getRotationDifference(rotation) < getRotationDifference(
//                                 placeRotation.rotation
//                             )
//                         )
//                             placeRotation = PlaceRotation(PlaceInfo(neighbor, facingType.opposite, hitVec), rotation)
//                     }
//                 }
//             }
//         }

//         placeRotation ?: return false

//         val fixedSensitivityRotation = placeRotation.rotation
//         fixedSensitivityRotation.fixedSensitivity()
//         setRotations(fixedSensitivityRotation)
//         lockRotation = fixedSensitivityRotation

//         placeInfo = placeRotation.placeInfo
//         return true
//     }

//     @EventTarget
//     fun onTick(event: TickEvent) {
//         val target = placeRotation?.placeInfo

//         if (extraClicks) {
//             val doubleClick = if (simulateDoubleClicking) RandomUtils.nextInt(-1, 1) else 0

//             repeat(extraClick.clicks + doubleClick) {
//                 extraClick.clicks--

//                 doPlaceAttempt()
//             }
//         }

//         if (target == null) {
//             if (placeDelayValue.get()) {
//                 delayTimer.reset()
//             }
//             return
//         }

//         val raycastProperly = !(scaffoldMode.get() == "Expand" && expandLength > 1 || shouldGoDown) && rotationMode.get() != "Off"

//         performBlockRaytrace(currRotation, mc.playerController.blockReachDistance).let {
//             if (rotationMode.get() == "Off" || it != null && it.blockPos == target.blockPos && (!raycastProperly || it.sideHit == target.enumFacing)) {
//                 val result = if (raycastProperly && it != null) {
//                     PlaceInfo(it.blockPos, it.sideHit, it.hitVec)
//                 } else {
//                     target
//                 }

//                 place(result)
//             }
//         }
//     }

//     @EventTarget
//     fun onSneakSlowDown(event: SneakSlowDownEvent) {
//         if (!isEagleEnabled || eagle != "Normal") {
//             return
//         }

//         event.forward *= eagleSpeed / 0.3f
//         event.strafe *= eagleSpeed / 0.3f
//     }

//     fun update() {
//         val player = mc.thePlayer ?: return
//         val holdingItem = player.heldItem?.item is ItemBlock

//         if (!holdingItem && (autoBlock == "Off" || InventoryUtils.findBlockInHotbar() == null)) {
//             return
//         }

//         findBlock(scaffoldMode.get() == "Expand" && expandLength > 1, searchMode == "Area")
//     }

//     private fun setRotation(rotation: Rotation, ticks: Int) {
//         val player = mc.thePlayer ?: return

//         if (silentRotation.get()) {
//             if (scaffoldMode.get() == "Telly" && MovementUtils.isMoving) {
//                 if (offGroundTicks < ticksUntilRotation.get() && ticksUntilJump >= jumpTicks) {
//                     return
//                 }
//             }

//             setTargetRotation(
//                 rotation,
//                 ticks,
//                 strafe,
//                 resetSpeed = minTurnSpeed to maxTurnSpeed,
//                 angleThresholdForReset = angleThresholdUntilReset,
//                 smootherMode = this.smootherMode
//             )

//         } else {
//             rotation.toPlayer(player)
//         }
//     }

//     // Search for new target block
//     private fun findBlock(expand: Boolean, area: Boolean) {
//         val player = mc.thePlayer ?: return

//         val blockPosition = if (shouldGoDown) {
//             if (player.posY == player.posY.roundToInt() + 0.5) {
//                 BlockPos(player.posX, player.posY - 0.6, player.posZ)
//             } else {
//                 BlockPos(player.posX, player.posY - 0.6, player.posZ).down()
//             }
//         } else if (shouldKeepLaunchPosition && launchY <= player.posY) {
//             BlockPos(player.posX, launchY - 1.0, player.posZ)
//         } else if (player.posY == player.posY.roundToInt() + 0.5) {
//             BlockPos(player)
//         } else {
//             BlockPos(player).down()
//         }

//         if (!expand && (!isReplaceable(blockPosition) || search(
//                 blockPosition,
//                 !shouldGoDown,
//                 area,
//                 shouldPlaceHorizontally
//             ))
//         ) {
//             /*if (mode != "GodBridge" || MathHelper.wrapAngleTo180_float(currRotation.yaw.toInt().toFloat()) in arrayOf(-135f, -45f, 45f, 135f)) {
//                 placeRotation = null
//             }*/
//             return
//         }

//         if (expand) {
//             val yaw = player.rotationYaw.toRadiansD()
//             val x = if (omniDirectionalExpand.get()) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
//             val z = if (omniDirectionalExpand.get()) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z

//             repeat(expandLength.get()) {
//                 if (search(blockPosition.add(x * it, 0, z * it), false, area))
//                     return
//             }
//             return
//         }

//         val (f, g) = if (scaffoldMode.get() == "Telly") 5 to 3 else if (allowClutching.get()) horizontalClutchBlocks.get() to verticalClutchBlocks.get() else 1 to 1

//         (-f..f).flatMap { x ->
//             (0 downTo -g).flatMap { y ->
//                 (-f..f).map { z ->
//                     Vec3i(x, y, z)
//                 }
//             }
//         }.sortedBy {
//             BlockUtils.getCenterDistance(blockPosition.add(it))
//         }.forEach {
//             if (BlockUtils.isReplaceable(blockPosition.add(it)) || search(
//                     blockPosition.add(it),
//                     !shouldGoDown,
//                     area,
//                     shouldPlaceHorizontally
//                 )
//             ) {
//                 return
//             }
//         }
//     }

//     private fun place(placeInfo: PlaceInfo) {
//         val player = mc.thePlayer ?: return
//         val world = mc.theWorld ?: return

//         if (!delayTimer.hasTimePassed() || shouldKeepLaunchPosition && launchY - 1 != placeInfo.vec3.yCoord.toInt())
//             return

//         var stack = player.inventoryContainer.getSlot(serverSlot + 36).stack

//         //TODO: blacklist more blocks than only bushes
//         if (stack == null || stack.item !is ItemBlock || (stack.item as ItemBlock).block is BlockBush || stack.stackSize <= 0 || sortByHighestAmount.get()) {
//             val blockSlot = if (sortByHighestAmount.get()) {
//                 InventoryUtils.findLargestBlockStackInHotbar() ?: return
//             } else {
//                 InventoryUtils.findBlockInHotbar() ?: return
//             }

//             when (autoBlock.lowercase()) {
//                 "off" -> return

//                 "pick" -> {
//                     player.inventory.currentItem = blockSlot - 36
//                     mc.playerController.updateController()
//                 }

//                 "spoof", "switch" -> serverSlot = blockSlot - 36
//             }
//             stack = player.inventoryContainer.getSlot(blockSlot).stack
//         }

//         // Line 437-440
//         if ((stack.item as? ItemBlock)?.canPlaceBlockOnSide(
//                 world,
//                 placeInfo.blockPos,
//                 placeInfo.enumFacing,
//                 player,
//                 stack
//             ) == false
//         ) {
//             return
//         }

//         tryToPlaceBlock(stack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3)

//         if (autoBlock == "Switch")
//             serverSlot = player.inventory.currentItem

//         // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
//         switchBlockNextTickIfPossible(stack)
//     }

//     private fun doPlaceAttempt() {
//         val player = mc.thePlayer ?: return
//         val world = mc.theWorld ?: return

//         val stack = player.inventoryContainer.getSlot(serverSlot + 36).stack ?: return

//         if (stack.item !is ItemBlock || InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as ItemBlock).block)) {
//             return
//         }

//         val block = stack.item as ItemBlock

//         val raytrace = performBlockRaytrace(currRotation, mc.playerController.blockReachDistance) ?: return

//         val canPlaceOnUpperFace = block.canPlaceBlockOnSide(
//             world, raytrace.blockPos, EnumFacing.UP, player, stack
//         )

//         val shouldPlace = if (placementAttempt == "Fail") {
//             !block.canPlaceBlockOnSide(world, raytrace.blockPos, raytrace.sideHit, player, stack)
//         } else {
//             if (shouldKeepLaunchPosition) {
//                 raytrace.blockPos.y == launchY - 1 && !canPlaceOnUpperFace
//             } else if (shouldPlaceHorizontally) {
//                 !canPlaceOnUpperFace
//             } else {
//                 raytrace.blockPos.y <= player.posY.toInt() - 1 && !(raytrace.blockPos.y == player.posY.toInt() - 1 && canPlaceOnUpperFace && raytrace.sideHit == EnumFacing.UP)
//             }
//         }

//         if (raytrace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !shouldPlace) {
//             return
//         }

//         tryToPlaceBlock(stack, raytrace.blockPos, raytrace.sideHit, raytrace.hitVec, attempt = true)

//         // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
//         switchBlockNextTickIfPossible(stack)
//     }

//     // Disabling module
//     override fun onDisable() {
//         val player = mc.thePlayer ?: return

//         if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
//             mc.gameSettings.keyBindSneak.pressed = false
//             if (eagleSneaking && player.isSneaking) {
//                 sendPacket(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SNEAKING))
//             }
//         }

//         if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
//             mc.gameSettings.keyBindRight.pressed = false
//         }
//         if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
//             mc.gameSettings.keyBindLeft.pressed = false
//         }

//         mc.gameSettings.thirdPersonView = 0
//         lockRotation = null
//         placeRotation = null
//         mc.timer.timerSpeed = 1f

//         TickScheduler += {
//             serverSlot = player.inventory.currentItem
//         }
//     }

//     // Entity movement event
//     @EventTarget
//     fun onMove(event: MoveEvent) {
//         val player = mc.thePlayer ?: return

//         if (!safeWalkValue.get() || shouldGoDown) {
//             return
//         }

//         if (airSafe.get() || player.onGround) {
//             event.isSafeWalk = true
//         }
//     }

//     @EventTarget
//     fun onJump(event: JumpEvent) {
//         if (onJump.get()) {
//             if (scaffoldMode.get() == "GodBridge" && (autoJump.get() || jumpAutomatically) || sameY)
//                 return
//             if (towerMode.get() == "None" || towerMode.get() == "Jump")
//                 return

//             event.cancelEvent()
//         }
//     }

//     // Scaffold visuals
//     @EventTarget
//     fun onRender2D(event: Render2DEvent) {
//         if (counterDisplay.get()) {
//             GL11.glPushMatrix()

//             if (BlockOverlay.handleEvents() && BlockOverlay.info && BlockOverlay.currentBlock != null) GL11.glTranslatef(
//                 0f,
//                 15f,
//                 0f
//             )

//             val info = "Blocks: §7$blocksAmount"
//             val (width, height) = ScaledResolution(mc)

//             RenderUtils.drawBorderedRect(
//                 width / 2 - 2,
//                 height / 2 + 5,
//                 width / 2 + Fonts.font40.getStringWidth(info) + 2,
//                 height / 2 + 16,
//                 3,
//                 Color.BLACK.rgb,
//                 Color.BLACK.rgb
//             )

//             GlStateManager.resetColor()

//             Fonts.font40.drawString(
//                 info, width / 2, height / 2 + 7, Color.WHITE.rgb
//             )
//             GL11.glPopMatrix()
//         }
//     }

//     // Visuals
//     @EventTarget
//     fun onRender3D(event: Render3DEvent) {
//         val player = mc.thePlayer ?: return

//         displaySafetyLinesIfEnabled()

//         if (!mark.get()) {
//             return
//         }

//         repeat(if (scaffoldMode.get() == "Expand") expandLength + 1 else 2) {
//             val yaw = player.rotationYaw.toRadiansD()
//             val x = if (omniDirectionalExpand.get()) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
//             val z = if (omniDirectionalExpand.get()) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
//             val blockPos = BlockPos(
//                 player.posX + x * it,
//                 if (shouldKeepLaunchPosition && launchY <= player.posY) launchY - 1.0 else player.posY - (if (player.posY == player.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
//                 player.posZ + z * it
//             )
//             val placeInfo = PlaceInfo.get(blockPos)

//             if (isReplaceable(blockPos) && placeInfo != null) {
//                 RenderUtils.drawBlockBox(blockPos, Color(68, 117, 255, 100), false)
//                 return
//             }
//         }
//     }

//     /**
//      * Search for placeable block
//      *
//      * @param blockPosition pos
//      * @param raycast visible
//      * @param area spot
//      * @return
//      */

//     private fun search(
//         blockPosition: BlockPos,
//         raycast: Boolean,
//         area: Boolean,
//         horizontalOnly: Boolean = false,
//     ): Boolean {
//         val player = mc.thePlayer ?: return false

//         if (!isReplaceable(blockPosition)) {
//             return false
//         }

//         val maxReach = mc.playerController.blockReachDistance

//         val eyes = player.eyes
//         var placeRotation: PlaceRotation? = null

//         var currPlaceRotation: PlaceRotation?

//         var considerStableRotation: PlaceRotation? = null

//         for (side in EnumFacing.values().filter { !horizontalOnly || it.axis != EnumFacing.Axis.Y }) {
//             val neighbor = blockPosition.offset(side)

//             if (!BlockUtils.isReplaceable(neighbor)) {
//                 continue
//             }

//             if (isGodBridgeEnabled) {
//                 // Selection of these values only. Mostly used = Godbridgers.
//                 val list = floatArrayOf(-135f, -45f, 45f, 135f)

//                 // Selection of pitch values that should be OK in non-complex situations.
//                 val pitchList = 55.0..75.7 + if (isLookingDiagonally) 1.0 else 0.0

//                 for (yaw in list) {
//                     for (pitch in pitchList step 0.1) {
//                         val rotation = Rotation(yaw, pitch.toFloat())

//                         val raytrace = performBlockRaytrace(rotation, maxReach) ?: continue

//                         currPlaceRotation =
//                             PlaceRotation(PlaceInfo(raytrace.blockPos, raytrace.sideHit, raytrace.hitVec), rotation)

//                         if (raytrace.blockPos == neighbor && raytrace.sideHit == side.opposite) {
//                             val isInStablePitchRange = if (isLookingDiagonally) {
//                                 pitch >= 75.6
//                             } else {
//                                 pitch in 73.5..75.7
//                             }

//                             // The module should be looking to aim at (nearly) the upper face of the block. Provides stable bridging most of the time.
//                             if (isInStablePitchRange) {
//                                 considerStableRotation = compareDifferences(currPlaceRotation, considerStableRotation)
//                             }

//                             placeRotation = compareDifferences(currPlaceRotation, placeRotation)
//                         }
//                     }
//                 }

//                 continue
//             }

//             if (!area) {
//                 currPlaceRotation =
//                     findTargetPlace(blockPosition, neighbor, Vec3(0.5, 0.5, 0.5), side, eyes, maxReach, raycast)
//                         ?: continue

//                 placeRotation = compareDifferences(currPlaceRotation, placeRotation)
//             } else {
//                 for (x in 0.1..0.9) {
//                     for (y in 0.1..0.9) {
//                         for (z in 0.1..0.9) {
//                             currPlaceRotation =
//                                 findTargetPlace(blockPosition, neighbor, Vec3(x, y, z), side, eyes, maxReach, raycast)
//                                     ?: continue

//                             placeRotation = compareDifferences(currPlaceRotation, placeRotation)
//                         }
//                     }
//                 }
//             }
//         }

//         placeRotation = considerStableRotation ?: placeRotation

//         placeRotation ?: return false

//         if (useStaticRotation.get() && scaffoldMode.get() == "GodBridge") {
//             placeRotation = PlaceRotation(
//                 placeRotation.placeInfo,
//                 Rotation(placeRotation.rotation.yaw, if (isLookingDiagonally) 75.6f else 73.5f)
//             )
//         }

//         if (rotationMode.get() != "Off") {
//             var targetRotation = placeRotation.rotation

//             val info = placeRotation.placeInfo

//             if (scaffoldMode.get() == "GodBridge") {
//                 val shouldJumpForcefully = isManualJumpOptionActive && blocksPlacedUntilJump >= blocksToJump

//                 performBlockRaytrace(currRotation, maxReach)?.let {
//                     val isSneaking = player.movementInput.sneak

//                     if ((!isSneaking || MovementUtils.speed != 0f) && it.blockPos == info.blockPos && (it.sideHit != info.enumFacing || shouldJumpForcefully) && MovementUtils.isMoving && currRotation.yaw.roundToInt() % 45f == 0f) {
//                         if (!isSneaking && autoJump.get()) {
//                             if (player.onGround && !isLookingDiagonally) {
//                                 player.tryJump()
//                             }

//                             if (shouldJumpForcefully) {
//                                 blocksPlacedUntilJump = 0
//                                 blocksToJump = randomDelay(
//                                     minBlocksToJump.get(),
//                                     maxBlocksToJump.get()
//                                 )
//                             }
//                         }

//                         targetRotation = currRotation
//                     }
//                 }
//             }

//             val limitedRotation = RotationUtils.limitAngleChange(
//                 currRotation,
//                 targetRotation,
//                 RandomUtils.nextFloat(minTurnSpeed, maxTurnSpeed),
//                 smootherMode
//             )

//             setRotation(limitedRotation, if (scaffoldMode.get() == "Telly") 1 else keepTicks)
//         }
//         this.placeRotation = placeRotation
//         return true
//     }

//     /**
//      * For expand scaffold, fixes vector values that should match according to direction vector
//      */
//     private fun modifyVec(original: Vec3, direction: EnumFacing, pos: Vec3, shouldModify: Boolean): Vec3 {
//         if (!shouldModify) {
//             return original
//         }

//         val x = original.xCoord
//         val y = original.yCoord
//         val z = original.zCoord

//         val side = direction.opposite

//         return when (side.axis ?: return original) {
//             EnumFacing.Axis.Y -> Vec3(x, pos.yCoord + side.directionVec.y.coerceAtLeast(0), z)
//             EnumFacing.Axis.X -> Vec3(pos.xCoord + side.directionVec.x.coerceAtLeast(0), y, z)
//             EnumFacing.Axis.Z -> Vec3(x, y, pos.zCoord + side.directionVec.z.coerceAtLeast(0))
//         }

//     }

//     private fun findTargetPlace(
//         pos: BlockPos, offsetPos: BlockPos, vec3: Vec3, side: EnumFacing, eyes: Vec3, maxReach: Float, raycast: Boolean,
//     ): PlaceRotation? {
//         val world = mc.theWorld ?: return null

//         val vec = (Vec3(pos) + vec3).addVector(
//             side.directionVec.x * vec3.xCoord, side.directionVec.y * vec3.yCoord, side.directionVec.z * vec3.zCoord
//         )

//         val distance = eyes.distanceTo(vec)

//         if (raycast && (distance > maxReach || world.rayTraceBlocks(eyes, vec, false, true, false) != null)) {
//             return null
//         }

//         val diff = vec - eyes

//         if (side.axis != EnumFacing.Axis.Y) {
//             val dist = abs(if (side.axis == EnumFacing.Axis.Z) diff.zCoord else diff.xCoord)

//             if (dist < minDist.get() && scaffoldMode.get() != "Telly") {
//                 return null
//             }
//         }

//         var rotation = toRotation(vec, false)

//         rotation = when (rotationMode.get()) {
//             "Stabilized" -> Rotation(round(rotation.yaw / 45f) * 45f, rotation.pitch)
//             else -> rotation
//         }
        
//         rotation.fixedSensitivity()

//         // If the current rotation already looks at the target block and side, then return right here
//         performBlockRaytrace(currRotation, maxReach)?.let { raytrace ->
//             if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
//                 return PlaceRotation(
//                     PlaceInfo(
//                         raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
//                     ), currRotation
//                 )
//             }
//         }

//         val raytrace = performBlockRaytrace(rotation, maxReach) ?: return null

//         if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
//             return PlaceRotation(
//                 PlaceInfo(
//                     raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
//                 ), rotation
//             )
//         }

//         return null
//     }

//     private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): MovingObjectPosition? {
//         val player = mc.thePlayer ?: return null
//         val world = mc.theWorld ?: return null

//         val eyes = player.eyes
//         val rotationVec = getVectorForRotation(rotation)

//         val reach = eyes + (rotationVec * maxReach.toDouble())

//         return world.rayTraceBlocks(eyes, reach, false, false, true)
//     }

//     private fun compareDifferences(
//         new: PlaceRotation, old: PlaceRotation?, rotation: Rotation = currRotation,
//     ): PlaceRotation {
//         if (old == null || getRotationDifference(
//                 new.rotation,
//                 rotation
//             ) < getRotationDifference(
//                 old.rotation, rotation
//             )
//         ) {
//             return new
//         }

//         return old
//     }

//     private fun switchBlockNextTickIfPossible(stack: ItemStack) {
//         val player = mc.thePlayer ?: return

//         if (autoBlock !in arrayOf("Off", "Switch") && stack.stackSize <= 0) {
//             InventoryUtils.findAutoBlockBlock().let {
//                 TickScheduler += {
//                     if (autoBlock.get() == "Pick") {
//                         player.inventory.currentItem = it - 36
//                         mc.playerController.updateController()
//                     } else {
//                         serverSlot = it - 36
//                     }
//                 }
//             }
//         }
//     }

//     private fun displaySafetyLinesIfEnabled() {
//         if (!safetyLines || !isGodBridgeEnabled) {
//             return
//         }

//         val player = mc.thePlayer ?: return

//         // If player is not walking diagonally then continue
//         if (round(abs(MathHelper.wrapAngleTo180_float(player.rotationYaw)).roundToInt() / 45f) * 45f !in arrayOf(
//                 135f,
//                 45f
//             ) || player.movementInput.moveForward == 0f || player.movementInput.moveStrafe != 0f
//         ) {
//             val (posX, posY, posZ) = player.interpolatedPosition()

//             GL11.glPushMatrix()
//             GL11.glTranslated(-posX, -posY, -posZ)
//             GL11.glLineWidth(5.5f)
//             GL11.glDisable(GL11.GL_TEXTURE_2D)

//             val (yawX, yawZ) = player.horizontalFacing.directionVec.x * 1.5 to player.horizontalFacing.directionVec.z * 1.5

//             // The target rotation will either be the module's placeRotation or a forced rotation (usually that's where the GodBridge mode aims)
//             val targetRotation = run {
//                 val yaw = floatArrayOf(-135f, -45f, 45f, 135f).minByOrNull {
//                     abs(
//                         RotationUtils.getAngleDifference(
//                             it,
//                             MathHelper.wrapAngleTo180_float(currRotation.yaw)
//                         )
//                     )
//                 } ?: return

//                 placeRotation?.rotation ?: Rotation(yaw, 73f)
//             }

//             // Calculate color based on rotation difference
//             val color = getColorForRotationDifference(
//                 getRotationDifference(
//                     targetRotation,
//                     currRotation
//                 )
//             )

//             val main = BlockPos(player).down()

//             val pos = if (BlockUtils.isReplaceable(main)) {
//                 main
//             } else {
//                 (-1..1).flatMap { x ->
//                     (-1..1).map { z ->
//                         val neighbor = main.add(x, 0, z)

//                         neighbor to BlockUtils.getCenterDistance(neighbor)
//                     }
//                 }.filter { BlockUtils.isReplaceable(it.first) }.minByOrNull { it.second }?.first ?: main
//             }.up().getVec()

//             for (offset in 0..1) {
//                 for (i in -1..1 step 2) {
//                     for (x1 in 0.25..0.5 step 0.01) {
//                         val opposite = offset == 1

//                         val (offsetX, offsetZ) = if (opposite) 0.0 to x1 * i else x1 * i to 0.0
//                         val (lineX, lineZ) = if (opposite) yawX to 0.0 else 0.0 to yawZ

//                         val (x, y, z) = pos.add(Vec3(offsetX, -0.99, offsetZ))

//                         GL11.glBegin(GL11.GL_LINES)

//                         GL11.glColor3f(color.x, color.y, color.z)
//                         GL11.glVertex3d(x - lineX, y + 0.5, z - lineZ)
//                         GL11.glVertex3d(x + lineX, y + 0.5, z + lineZ)

//                         GL11.glEnd()
//                     }
//                 }
//             }
//             GL11.glEnable(GL11.GL_TEXTURE_2D)
//             GL11.glPopMatrix()
//         }
//     }

//     private fun getColorForRotationDifference(rotationDifference: Float): Color3f {
//         val maxDifferenceForGreen = 10.0f
//         val maxDifferenceForYellow = 40.0f

//         val interpolationFactor = when {
//             rotationDifference <= maxDifferenceForGreen -> 0.0f
//             rotationDifference <= maxDifferenceForYellow -> (rotationDifference - maxDifferenceForGreen) / (maxDifferenceForYellow - maxDifferenceForGreen)
//             else -> 1.0f
//         }

//         val green = 1.0f - interpolationFactor
//         val blue = 0.0f

//         return Color3f(interpolationFactor, green, blue)
//     }

//     private fun updatePlacedBlocksForTelly() {
//         if (blocksUntilAxisChange > horizontalPlacements + verticalPlacements) {
//             blocksUntilAxisChange = 0

//             horizontalPlacements =
//                 randomDelay(horizontal.getMinValue(), horizontal.getMaxValue())
//             verticalPlacements =
//                 randomDelay(vertical.getMinValue(), vertical.getMaxValue())
//             return
//         }

//         blocksUntilAxisChange++
//     }

//     private fun tryToPlaceBlock(
//         stack: ItemStack,
//         clickPos: BlockPos,
//         side: EnumFacing,
//         hitVec: Vec3,
//         attempt: Boolean = false,
//     ): Boolean {
//         val thePlayer = mc.thePlayer ?: return false

//         val prevSize = stack.stackSize

//         val clickedSuccessfully = thePlayer.onPlayerRightClick(clickPos, side, hitVec, stack)

//         if (clickedSuccessfully) {
//             if (!attempt) {
//                 delayTimer.reset()

//                 if (thePlayer.onGround) {
//                     thePlayer.motionX *= speedModifier.get()
//                     thePlayer.motionZ *= speedModifier.get()
//                 }
//             }

//             if (swing.get()) thePlayer.swingItem()
//             else sendPacket(C0APacketAnimation())

//             if (isManualJumpOptionActive && autoJump.get())
//                 blocksPlacedUntilJump++

//             updatePlacedBlocksForTelly()

//             if (stack.stackSize <= 0) {
//                 thePlayer.inventory.mainInventory[serverSlot] = null
//                 ForgeEventFactory.onPlayerDestroyItem(thePlayer, stack)
//             } else if (stack.stackSize != prevSize || mc.playerController.isInCreativeMode)
//                 mc.entityRenderer.itemRenderer.resetEquippedProgress()

//         } else {
//             if (thePlayer.sendUseItem(stack))
//                 mc.entityRenderer.itemRenderer.resetEquippedProgress2()
//         }

//         return clickedSuccessfully
//     }

//     /**
//      * Returns the amount of blocks
//      */
//     private val blocksAmount: Int
//         get() {
//             var amount = 0
//             for (i in 36..44) {
//                 val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue
//                 val item = stack.item
//                 if (item is ItemBlock) {
//                     val block = item.block
//                     val heldItem = mc.thePlayer.heldItem
//                     if (heldItem != null && heldItem == stack || block !in InventoryUtils.BLOCK_BLACKLIST && block !is BlockBush) {
//                         amount += stack.stackSize
//                     }
//                 }
//             }
//             return amount
//         }

//     override val tag
//         get() = if (towerMode.get() != "None") ("$scaffoldMode | $towerMode") else scaffoldMode.get()
// }
// open class DelayTimer(
//     private val minDelayValue: Int, private val maxDelayValue: Int = minDelayValue,
//     private val baseTimer: MSTimer = CLICK_TIMER
// ) {
//     private var delay = 0

//     open fun hasTimePassed() = baseTimer.hasTimePassed(delay)

//     fun resetDelay() {
//         delay = randomDelay(minDelayValue, maxDelayValue).toInt()
//     }

//     fun resetTimer() = baseTimer.reset()

//     fun reset() {
//         resetTimer()
//         resetDelay()
//     }
}
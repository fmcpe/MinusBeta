/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils

import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.util.*
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.utils.RaycastUtils.IEntityFilter
import net.minusmc.minusbounce.utils.RaycastUtils.raycastEntity
import net.minusmc.minusbounce.utils.extensions.*
import net.minusmc.minusbounce.utils.movement.MovementFixType
import java.util.*
import kotlin.math.*


object RotationUtils : MinecraftInstance(), Listenable {
    private val random = Random()
    private var keepLength = 0

    @JvmField
    var targetRotation: Rotation? = null

    @JvmField
    var offGroundTicks: Int = 0

    @JvmField
    var onGroundTicks: Int = 0

    private var active: Boolean = false
    private var smoothed: Boolean = false
    private var silent: Boolean = false
    private var lastRotations: Rotation? = null
    private var rotations: Rotation? = null
    private var rotationSpeed: Float = 0f
    private var set: Boolean = true
    var type: MovementFixType = MovementFixType.NONE

    private var x = random.nextDouble()
    private var y = random.nextDouble()
    private var z = random.nextDouble()

    @JvmStatic
    fun smooth() {
        if (!smoothed) {
            targetRotation = limitAngleChange(lastRotations ?: return, rotations ?: return , rotationSpeed - Math.random().toFloat())
        }

        mc.entityRenderer.getMouseOver(1.0F)
        smoothed = true
    }

    @EventTarget(priority = -5)
    fun onTick(event: PreUpdateEvent) {
        if (targetRotation == null || lastRotations == null || rotations == null || !active) {
            targetRotation = mc.thePlayer.rotation
            lastRotations = mc.thePlayer.rotation
            rotations = mc.thePlayer.rotation
        }

        if (active) {
            smooth()
        }

        if (random.nextGaussian() > 0.8) x = Math.random()
        if (random.nextGaussian() > 0.8) y = Math.random()
        if (random.nextGaussian() > 0.8) z = Math.random()
    }

    @EventTarget(priority = -5)
    fun onMotion(event: PreMotionEvent) {
        /* On / Off Ground Ticks*/
        if(event.onGround){
            offGroundTicks = 0
            onGroundTicks++
        } else {
            onGroundTicks = 0
            offGroundTicks++
        }

        if (active && targetRotation != null) {
            keepLength--

            // No Setting Rotation
            if(!set){
                lastRotations = targetRotation
                return
            }

            if(this.silent){
                targetRotation?.let{
                    event.yaw = it.yaw
                    event.pitch = it.pitch
                }
            } else {
                targetRotation!!.toPlayer(mc.thePlayer)
            }

            if (abs((targetRotation!!.yaw - mc.thePlayer.rotationYaw) % 360) < 1 && abs((targetRotation!!.pitch - mc.thePlayer.rotationPitch)) < 1) {
                active = false

                if(silent && set){
                    /* It will conflict with non-silent */
                    val targetRotation = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
                    targetRotation.fixedSensitivity(r = lastRotations ?: serverRotation)

                    mc.thePlayer.rotationYaw = targetRotation.yaw + MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - targetRotation.yaw)
                    mc.thePlayer.rotationPitch = targetRotation.pitch
                }
            }

            mc.thePlayer.renderYawOffset = targetRotation!!.yaw
            mc.thePlayer.rotationYawHead = targetRotation!!.yaw
            lastRotations = targetRotation
        } else {
            lastRotations = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        }

        if(keepLength <= 0) {
            rotations = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        }

        smoothed = false
    }

    @EventTarget(priority = -5)
    fun onInput(e: MoveInputEvent){
        if(e.forward == 0.0f && e.strafe == 0.0f) return

        val possible = mutableListOf<Input>()
        for (strafe in -1..1 step 1){
            for (forward in -1..1 step 1){
                if(strafe == 0 && forward == 0) continue

                possible.add(Input(strafe.toFloat(), forward.toFloat()))
            }
        }

        val predicted = mutableListOf<ListInput>()
        possible.forEach {
            predicted.add(ListInput(
                MathHelper.wrapAngleTo180_float(
                    MovementUtils.getRawDirection(
                        targetRotation?.yaw ?: return,
                        it.strafe,
                        it.forward
                    )
                ), it)
            )
        }

        val closest = predicted.minByOrNull {
            abs(it.yaw - MathHelper.wrapAngleTo180_float(
                MovementUtils.getRawDirection(
                    mc.thePlayer.rotationYaw,
                    e.strafe,
                    e.forward)
                )
            )
        } ?: return

        if(type == MovementFixType.FULL && active) {
            e.forward = closest.input.forward
            e.strafe = closest.input.strafe
        }
    }
    data class Input(val strafe: Float, val forward: Float)
    data class ListInput(val yaw: Float, val input: Input)

    @EventTarget(priority = -5)
    fun onStrafe(event: StrafeEvent){
        if(active && (type == MovementFixType.NORMAL || type == MovementFixType.FULL)) {
            event.yaw = targetRotation?.yaw ?: return
        }
    }

    @EventTarget(priority = -5)
    fun onJump(event: JumpEvent){
        if(active && (type == MovementFixType.NORMAL || type == MovementFixType.FULL)) {
            event.yaw = targetRotation?.yaw ?: return
        }
    }
    
    @EventTarget(priority = -5)
    fun onLook(event: LookEvent){
        if(active && targetRotation != null && lastRotations != null){
            event.yaw = targetRotation?.yaw ?: return
            event.pitch = targetRotation?.pitch ?: return
            event.lastYaw = lastRotations?.yaw ?: return
            event.lastPitch = lastRotations?.pitch ?: return
        }
    }

    /**
     * Set your target rotation
     *
     * @author fmcpe
     * @param rotation your target rotation
     */
    @JvmOverloads
    fun setRotations(
        rotation: Rotation,
        keepLength: Int = 2,
        speed: Float = 180f,
        fixType: MovementFixType = MovementFixType.FULL,
        silent: Boolean = true,
        set: Boolean = true
    ) {
        rotation.isNan() ?: return
        this.type = if(silent) fixType else MovementFixType.NONE
        this.rotationSpeed = speed
        this.rotations = rotation
        this.keepLength = keepLength
        this.silent = silent
        this.set = set
        active = true

        smooth()
    }

    /**
     * @return YESSSS!!!
     */
    override fun handleEvents() = true

    /**
     * Face block
     *
     * @param blockPos target block
     */
    fun faceBlock(blockPos: BlockPos?): VecRotation? {
        if (blockPos == null) return null
        var vecRotation: VecRotation? = null

        for (x in 0.1..0.9){
            for(y in 0.1..0.9){
                for(z in 0.1..0.9){
                    val eyesPos = Vec3(
                        mc.thePlayer.posX,
                        mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
                        mc.thePlayer.posZ
                    )
                    val posVec = Vec3(blockPos).addVector(x, y, z)
                    val dist = eyesPos.distanceTo(posVec)
                    val diffX = posVec.xCoord - eyesPos.xCoord
                    val diffY = posVec.yCoord - eyesPos.yCoord
                    val diffZ = posVec.zCoord - eyesPos.zCoord
                    val diffXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble()
                    val rotation = Rotation(
                        MathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                        MathHelper.wrapAngleTo180_float(-Math.toDegrees(atan2(diffY, diffXZ)).toFloat())
                    )
                    val rotationVector = getVectorForRotation(rotation)
                    val vector = eyesPos.addVector(
                        rotationVector.xCoord * dist, rotationVector.yCoord * dist,
                        rotationVector.zCoord * dist
                    )
                    val obj = mc.theWorld.rayTraceBlocks(
                        eyesPos, vector, false,
                        false, true
                    )
                    if (obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        val currentVec = VecRotation(posVec, rotation)
                        if (vecRotation == null || getRotationDifference(currentVec.rotation) < getRotationDifference(
                                vecRotation.rotation
                            )
                        ) vecRotation = currentVec
                    }
                }
            }
        }

        return vecRotation
    }

    /**
     * Creates a raytrace even when the target [blockPos] is not visible
     */
    fun performRaytrace(
        blockPos: BlockPos,
        rotation: Rotation,
        reach: Float = mc.playerController.blockReachDistance,
    ): MovingObjectPosition? {
        val world = mc.theWorld ?: return null
        val player = mc.thePlayer ?: return null

        val eyes = player.eyes

        return blockPos.getBlock()?.collisionRayTrace(
            world,
            blockPos,
            eyes,
            eyes + (getVectorForRotation(rotation) * reach.toDouble())
        )
    }

    fun performRayTrace(blockPos: BlockPos, vec: Vec3, eyes: Vec3 = mc.thePlayer.eyes) =
        mc.theWorld?.let { blockPos.getBlock()?.collisionRayTrace(it, blockPos, eyes, vec) }

    /**
     * Face target with bow
     *
     * @param target your enemy
     * @param silent client side rotations
     * @param predict predict new enemy position
     * @param predictSize predict size of predict
     */
    fun faceBow(target: Entity, silent: Boolean, predict: Boolean, predictSize: Float) {
        val player = mc.thePlayer
        val (posX, posY, posZ) = Vec3(
            target.posX + (if (predict) (target.posX - target.prevPosX) * predictSize else 0.0) - (player.posX + (if (predict) player.posX - player.prevPosX else 0.0)),
            target.entityBoundingBox.minY + (if (predict) (target.entityBoundingBox.minY - target.prevPosY) * predictSize else 0.0) + target.eyeHeight - 0.15 - (player.entityBoundingBox.minY + if (predict) player.posY - player.prevPosY else 0.0) - player.getEyeHeight(),
            target.posZ + (if (predict) (target.posZ - target.prevPosZ) * predictSize else 0.0) - (player.posZ + if (predict) player.posZ - player.prevPosZ else 0.0)
        )
        val posSqrt = sqrt(posX * posX + posZ * posZ)

        var velocity = player.itemInUseDuration / 20f
        velocity = (velocity * velocity + velocity * 2) / 3
        if (velocity > 1) velocity = 1f

        val rotation = Rotation((atan2(posZ, posX) * 180 / Math.PI).toFloat() - 90, -Math.toDegrees(atan((velocity * velocity - sqrt(velocity * velocity * velocity * velocity - 0.006f * (0.006f * (posSqrt * posSqrt) + 2 * posY * (velocity * velocity)))) / (0.006f * posSqrt))).toFloat())
        setRotations(rotation, silent = silent)
    }

    /**
     * Translate vec to rotation
     * Diff supported
     *
     * @param vec target vec
     * @param predict predict new location of your body
     * @return rotation
     */
    @JvmOverloads
    fun toRotation(vec: Vec3, predict: Boolean = false, diff: Vec3? = null): Rotation {
        val eyesPos = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)
        if (predict) eyesPos.addVector(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)

        val (diffX, diffY, diffZ) = diff ?: (vec - eyesPos)

        return Rotation(
            MathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
            MathHelper.wrapAngleTo180_float((-Math.toDegrees(atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)))).toFloat())
        )
    }

    /**
     * Get the center of a box
     *
     * @param bb your box
     * @return center of box
     */
    fun getCenter(bb: AxisAlignedBB): Vec3 {
        return Vec3(
            bb.minX + (bb.maxX - bb.minX) * 0.5,
            bb.minY + (bb.maxY - bb.minY) * 0.5,
            bb.minZ + (bb.maxZ - bb.minZ) * 0.5
        )
    }


    /**
     * Search good center
     *
     * @param bb enemy box
     * @param outborder outborder option
     * @param random random option
     * @param predict predict option
     * @param throughWalls throughWalls option
     * @return center
     */
    @JvmOverloads
    fun searchCenter(
        bb: AxisAlignedBB,
        random: Boolean,
        predict: Boolean,
        throughWalls: Boolean,
        distance: Float,
        randomMultiply: Float = 0f,
    ): VecRotation? {
        val randomVec = Vec3(
            bb.minX + (bb.maxX - bb.minX) * x * randomMultiply * Math.random(),
            bb.minY + (bb.maxY - bb.minY) * y * randomMultiply * Math.random(),
            bb.minZ + (bb.maxZ - bb.minZ) * z * randomMultiply * Math.random()
        )

        val randomRotation = toRotation(randomVec, predict)
        val eyes = mc.thePlayer.getPositionEyes(1f)
        var vecRotation: VecRotation? = null

        for (x in 0.0..1.0){
            for (y in 0.0..1.0){
                for (z in 0.0..1.0){
                    val vec3 = Vec3(bb.minX + (bb.maxX - bb.minX) * x, bb.minY + (bb.maxY - bb.minY) * y, bb.minZ + (bb.maxZ - bb.minZ) * z)
                    val currentVec = VecRotation(vec3, toRotation(Vec3(0.0, 0.0, 0.0), predict, getVectorForRotation(targetRotation ?: serverRotation) - vec3))
                    if (eyes.distanceTo(vec3) > distance) {
                        continue
                    }
                    if (throughWalls || isVisible(vec3)) {
                        if (
                            vecRotation == null ||
                            if (random) {
                                getRotationDifference(currentVec.rotation, randomRotation) < getRotationDifference(vecRotation.rotation, randomRotation)
                            } else {
                                getRotationDifference(currentVec.rotation) < getRotationDifference(vecRotation.rotation)
                            }
                        ) {
                            vecRotation = currentVec
                        }
                    }
                }
            }
        }

        return vecRotation
    }

    /**
     * Calculate difference between the client rotation and your entity
     *
     * @param entity your entity
     * @return difference between rotation
     */
    fun getRotationDifference(entity: Entity): Double {
        val rotation = toRotation(getCenter(entity.entityBoundingBox), true)
        return getRotationDifference(rotation, Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch))
    }

    /**
     * Calculate difference between the server rotation and your rotation
     *
     * @param rotation your rotation
     * @return difference between rotation
     */
    fun getRotationDifference(rotation: Rotation) = getRotationDifference(rotation, serverRotation)

    /**
     * Calculate difference between two rotations
     *
     * @param a rotation
     * @param b rotation
     * @return difference between rotation
     */
    fun getRotationDifference(a: Rotation, b: Rotation?): Double {
        return hypot(getAngleDifference(a.yaw, b!!.yaw).toDouble(), (a.pitch - b.pitch).toDouble())
    }

    /**
     * Limit your rotation using a turn speed
     *
     * @param currentRotation your current rotation
     * @param targetRotation your goal rotation
     * @param turnSpeed your turn speed
     * @return limited rotation
     */
    fun limitAngleChange(currentRotation: Rotation, targetRotation: Rotation, turnSpeed: Float): Rotation {
        var (yaw, pitch) = targetRotation
        val yDiff = getAngleDifference(targetRotation.yaw, currentRotation.yaw)
        val pDiff = getAngleDifference(targetRotation.pitch, currentRotation.pitch)

        val distance = sqrt(yDiff * yDiff + pDiff * pDiff)
        if(turnSpeed < 0 || distance <= 0) return currentRotation
        val maxYaw = turnSpeed * abs(yDiff / distance)
        val maxPitch = turnSpeed * abs(pDiff / distance)

        val yAdd = max(min(yDiff, maxYaw), -maxYaw)
        val pAdd = max(min(pDiff, maxPitch), -maxPitch)

        yaw = currentRotation.yaw + yAdd
        pitch = currentRotation.pitch + pAdd

        /* Randomize */
        for (i in 1.0..Minecraft.getDebugFPS() / 20.0 + Math.random() * 10.0 step 1.0) {
            if (abs(yAdd) + abs(pAdd) > 1) {
                yaw += (Math.random().toFloat() - 0.5f) / 1000f
                pitch -= Math.random().toFloat() / 200f
            }

            /* Fixing GCD */
            val rotation = Rotation(yaw, pitch)
            rotation.fixedSensitivity()

            /* Setting Rotation */
            yaw = rotation.yaw
            pitch = rotation.pitch
        }

        return Rotation(yaw, pitch)
    }

    /**
     * Calculate difference between two angle points
     *
     * @param a angle point
     * @param b angle point
     * @return difference between angle points
     */
    fun getAngleDifference(a: Float, b: Float): Float {
        return ((a - b) % 360f + 540f) % 360f - 180f
    }

    /**
     * Calculate rotation to vector
     *
     * @param rotation your rotation
     * @return target vector
     */
    @JvmStatic
    fun getVectorForRotation(rotation: Rotation): Vec3 {
        val rotX = rotation.yaw * Math.PI / 180f
        val rotY = rotation.pitch * Math.PI / 180f

        return Vec3(-cos(rotY) * sin(rotX), -sin(rotY), cos(rotY) * cos(rotX))
    }

    /**
     * Allows you to check if your crosshair is over your target entity
     *
     * @param targetEntity your target entity
     * @param blockReachDistance your reach
     * @return if crosshair is over target
     */
    fun isFaced(targetEntity: Entity, blockReachDistance: Double): Boolean {
        return raycastEntity(
            blockReachDistance,
            object : IEntityFilter {
                override fun canRaycast(entity: Entity?): Boolean {
                    return entity === targetEntity
                }
            }) != null
    }

    /**
     * Allows you to check if your enemy is behind a wall
     */
    fun isVisible(vec3: Vec3?): Boolean {
        val eyesPos = Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        )
        return mc.theWorld.rayTraceBlocks(eyesPos, vec3) == null
    }
}
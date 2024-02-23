/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.*
import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.utils.RaycastUtils.IEntityFilter
import net.minusmc.minusbounce.utils.RaycastUtils.raycastEntity
import net.minusmc.minusbounce.utils.movement.MovementFixType
import java.util.*
import kotlin.math.*


object RotationUtils : MinecraftInstance(), Listenable {
    private val random = Random()
    private var keepLength = 0

    @JvmField
    var targetRotation: Rotation? = null

    private var active: Boolean = false
    private var smoothed: Boolean = false
    private var lastRotations: Rotation? = null
    private var rotations: Rotation? = null
    private var rotationSpeed: Float = 0f
    var type: MovementFixType = MovementFixType.NONE

    private var x = random.nextDouble()
    private var y = random.nextDouble()
    private var z = random.nextDouble()

    @EventTarget(priority = -2)
    fun onStrafe(event: StrafeEvent){
        if(type == MovementFixType.NORMAL || type == MovementFixType.FULL) {
            if(active){
                event.yaw = targetRotation!!.yaw
            }
        }
    }

    @EventTarget(priority = -2)
    fun onJump(event: JumpEvent){
        if(type == MovementFixType.NORMAL || type == MovementFixType.FULL) {
            if(active){
                event.yaw = targetRotation!!.yaw
            }
        }
    }

    @EventTarget(priority = -2)
    fun onInput(event: MoveInputEvent){
        val forward = event.forward
        val strafe = event.strafe

        if(type == MovementFixType.FULL && active) {
            val offset = (mc.thePlayer.rotationYaw - targetRotation!!.yaw) * 0.01745329251994329576f

            event.forward = round(forward * cos(offset) + strafe * sin(offset))
            event.strafe = round(strafe * cos(offset) - forward * sin(offset))
        }
    }

    private fun correctDisabledRotations() {
        val targetRotation = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        targetRotation.fixedSensitivity(mc.gameSettings.mouseSensitivity, lastRotations)

        mc.thePlayer.rotationYaw = targetRotation.yaw + MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - targetRotation.yaw);
        mc.thePlayer.rotationPitch = targetRotation.pitch
    }

    @JvmStatic
    fun smooth() {
        if (!smoothed) {
            targetRotation = limitAngleChange(lastRotations!!, rotations!!, rotationSpeed + Math.random().toFloat())
        }

        smoothed = true

        /*
         * Updating MouseOver
         */
        mc.entityRenderer.getMouseOver(1.0F)
    }

    @EventTarget(priority = -2)
    fun onUpdate(event: PreUpdateEvent) {
        if (targetRotation == null || lastRotations == null || rotations == null || !active) {
            targetRotation = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
            lastRotations = targetRotation
            rotations = targetRotation
        }

        if (active) {
            smooth()
        }

        if (random.nextGaussian() > 0.8) x = Math.random()
        if (random.nextGaussian() > 0.8) y = Math.random()
        if (random.nextGaussian() > 0.8) z = Math.random()
    }

    @EventTarget(priority = -2)
    fun onMotion(event: PreMotionEvent) {
        if (active && targetRotation != null) {
            event.yaw = targetRotation!!.yaw
            event.pitch = targetRotation!!.pitch

            mc.thePlayer.renderYawOffset = targetRotation!!.yaw
            mc.thePlayer.rotationYawHead = targetRotation!!.yaw

            if (abs((targetRotation!!.yaw - mc.thePlayer.rotationYaw) % 360) < 1 && abs((targetRotation!!.pitch - mc.thePlayer.rotationPitch)) < 1) {
                active = false

                correctDisabledRotations()
            }

            lastRotations = targetRotation
        } else {
            lastRotations = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        }

        rotations = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        smoothed = false
    }

    @EventTarget(priority = -2)
    fun onLook(event: LookEvent){
        targetRotation?.let{
            event.yaw = it.yaw
            event.pitch = it.pitch
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
        rotation: Rotation?,
        keepLength: Int,
        speed: Float = 180f,
        fixType: MovementFixType = MovementFixType.NONE
    ) {
        if(rotation!!.isNan()){
            return
        }

        this.type = fixType
        this.rotationSpeed = speed
        this.rotations = rotation
        this.keepLength = keepLength
        active = true

        smooth()
    }

    /**
     * @return YESSSS!!!
     */
    override fun handleEvents() = true

    /**
     * @author aquavit
     *
     * epic skid moment
     */
    fun otherRotation(
        bb: AxisAlignedBB,
        vec: Vec3,
        predict: Boolean,
        throughWalls: Boolean,
        distance: Float
    ): Rotation {
        val eyesPos = Vec3(
            mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY +
                    mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ
        )
        val eyes = mc.thePlayer.getPositionEyes(1f)
        var vecRotation: VecRotation? = null
        var xSearch = 0.15
        while (xSearch < 0.85) {
            var ySearch = 0.15
            while (ySearch < 1.0) {
                var zSearch = 0.15
                while (zSearch < 0.85) {
                    val vec3 = Vec3(
                        bb.minX + (bb.maxX - bb.minX) * xSearch,
                        bb.minY + (bb.maxY - bb.minY) * ySearch, bb.minZ + (bb.maxZ - bb.minZ) * zSearch
                    )
                    val rotation = toRotation(vec3, predict)
                    val vecDist = eyes.distanceTo(vec3)
                    if (vecDist > distance) {
                        zSearch += 0.1
                        continue
                    }
                    if (throughWalls || isVisible(vec3)) {
                        val currentVec = VecRotation(vec3, rotation)
                        if (vecRotation == null) vecRotation = currentVec
                    }
                    zSearch += 0.1
                }
                ySearch += 0.1
            }
            xSearch += 0.1
        }
        if (predict) eyesPos.addVector(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)
        val diffX = vec.xCoord - eyesPos.xCoord
        val diffY = vec.yCoord - eyesPos.yCoord
        val diffZ = vec.zCoord - eyesPos.zCoord
        return Rotation(
            MathHelper.wrapAngleTo180_float(
                Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
            ),
            MathHelper.wrapAngleTo180_float(
                (-Math.toDegrees(
                    atan2(
                        diffY,
                        sqrt(diffX * diffX + diffZ * diffZ)
                    )
                )).toFloat()
            )
        )
    }

    /**
     * Face block
     *
     * @param blockPos target block
     */
    fun faceBlock(blockPos: BlockPos?): VecRotation? {
        if (blockPos == null) return null
        var vecRotation: VecRotation? = null
        var xSearch = 0.1
        while (xSearch < 0.9) {
            var ySearch = 0.1
            while (ySearch < 0.9) {
                var zSearch = 0.1
                while (zSearch < 0.9) {
                    val eyesPos = Vec3(
                        mc.thePlayer.posX,
                        mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
                        mc.thePlayer.posZ
                    )
                    val posVec = Vec3(blockPos).addVector(xSearch, ySearch, zSearch)
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
                    zSearch += 0.1
                }
                ySearch += 0.1
            }
            xSearch += 0.1
        }
        return vecRotation
    }

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
        val posX: Double = target.posX + (if (predict) (target.posX - target.prevPosX) * predictSize else 0.0) - (player.posX + (if (predict) player.posX - player.prevPosX else 0.0))
        val posY: Double =
            target.entityBoundingBox.minY + (if (predict) (target.entityBoundingBox.minY - target.prevPosY) * predictSize else 0.0) + target.eyeHeight - 0.15 - (player.entityBoundingBox.minY + if (predict) player.posY - player.prevPosY else 0.0) - player.getEyeHeight()
        val posZ: Double =
            target.posZ + (if (predict) (target.posZ - target.prevPosZ) * predictSize else 0.0) - (player.posZ + if (predict) player.posZ - player.prevPosZ else 0.0)
        val posSqrt = sqrt(posX * posX + posZ * posZ)
        var velocity = player.itemInUseDuration / 20f
        velocity = (velocity * velocity + velocity * 2) / 3
        if (velocity > 1) velocity = 1f
        val rotation = Rotation(
            (atan2(posZ, posX) * 180 / Math.PI).toFloat() - 90,
            -Math.toDegrees(atan((velocity * velocity - sqrt(velocity * velocity * velocity * velocity - 0.006f * (0.006f * (posSqrt * posSqrt) + 2 * posY * (velocity * velocity)))) / (0.006f * posSqrt)))
                .toFloat()
        )
        if (silent) setRotations(rotation) else limitAngleChange(
            Rotation(player.rotationYaw, player.rotationPitch), rotation, (10 +
                    Random().nextInt(6)).toFloat()
        ).toPlayer(mc.thePlayer)
    }

    /**
     * Translate vec to rotation
     *
     * @param vec target vec
     * @param predict predict new location of your body
     * @return rotation
     */
    fun toRotation(vec: Vec3, predict: Boolean): Rotation {
        val eyesPos = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)
        if (predict) eyesPos.addVector(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)
        val diffX = vec.xCoord - eyesPos.xCoord
        val diffY = vec.yCoord - eyesPos.yCoord
        val diffZ = vec.zCoord - eyesPos.zCoord
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
        outborder: Boolean,
        random: Boolean,
        predict: Boolean,
        throughWalls: Boolean,
        distance: Float,
        randomMultiply: Float = 0f,
        newRandom: Boolean = false
    ): VecRotation? {
        if (outborder) {
            val vec3 = Vec3(
                bb.minX + (bb.maxX - bb.minX) * (x * 0.3 + 1.0),
                bb.minY + (bb.maxY - bb.minY) * (y * 0.3 + 1.0),
                bb.minZ + (bb.maxZ - bb.minZ) * (z * 0.3 + 1.0)
            )
            return VecRotation(vec3, toRotation(vec3, predict))
        }
        val randomVec = Vec3(
            bb.minX + (bb.maxX - bb.minX) * x * randomMultiply * if (newRandom) Math.random() else 1.0,
            bb.minY + (bb.maxY - bb.minY) * y * randomMultiply * if (newRandom) Math.random() else 1.0,
            bb.minZ + (bb.maxZ - bb.minZ) * z * randomMultiply * if (newRandom) Math.random() else 1.0
        )
        val randomRotation = toRotation(randomVec, predict)
        val eyes = mc.thePlayer.getPositionEyes(1f)
        var vecRotation: VecRotation? = null
        var xSearch = 0.15
        while (xSearch < 0.85) {
            var ySearch = 0.15
            while (ySearch < 1.0) {
                var zSearch = 0.15
                while (zSearch < 0.85) {
                    val vec3 = Vec3(
                        bb.minX + (bb.maxX - bb.minX) * xSearch,
                        bb.minY + (bb.maxY - bb.minY) * ySearch, bb.minZ + (bb.maxZ - bb.minZ) * zSearch
                    )
                    val rotation = toRotation(vec3, predict)
                    val vecDist = eyes.distanceTo(vec3)
                    if (vecDist > distance) {
                        zSearch += 0.1
                        continue
                    }
                    if (throughWalls || isVisible(vec3)) {
                        val currentVec = VecRotation(vec3, rotation)
                        if (vecRotation == null || (if (random) getRotationDifference(
                                currentVec.rotation,
                                randomRotation
                            ) < getRotationDifference(
                                vecRotation.rotation,
                                randomRotation
                            ) else getRotationDifference(currentVec.rotation) < getRotationDifference(vecRotation.rotation))
                        ) vecRotation = currentVec
                    }
                    zSearch += 0.1
                }
                ySearch += 0.1
            }
            xSearch += 0.1
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
        val yawDifference = getAngleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDifference = getAngleDifference(targetRotation.pitch, currentRotation.pitch)

        return Rotation(
            currentRotation.yaw + if (yawDifference > turnSpeed) turnSpeed else max(yawDifference, -turnSpeed),
            currentRotation.pitch + if (pitchDifference > turnSpeed) turnSpeed else max(pitchDifference, -turnSpeed)
        )
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
        val yawCos = MathHelper.cos(-rotation.yaw * 0.017453292f - Math.PI.toFloat())
        val yawSin = MathHelper.sin(-rotation.yaw * 0.017453292f - Math.PI.toFloat())
        val pitchCos = -MathHelper.cos(-rotation.pitch * 0.017453292f)
        val pitchSin = MathHelper.sin(-rotation.pitch * 0.017453292f)
        return Vec3((yawSin * pitchCos).toDouble(), pitchSin.toDouble(), (yawCos * pitchCos).toDouble())
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

    /**
     * Set your target rotation
     *
     * @param rotation your target rotation
     */
    fun setRotations(rotation: Rotation) {
        setRotations(rotation, 0)
    }


    fun getRotationsEntity(entity: EntityLivingBase): Rotation {
        return getRotations(entity.posX, entity.posY + entity.eyeHeight - 0.4, entity.posZ)
    }

    fun getRotations(ent: Entity): Rotation {
        val x = ent.posX
        val z = ent.posZ
        val y = ent.posY + (ent.eyeHeight / 2.0f).toDouble()
        return getRotationFromPosition(x, z, y)
    }

    fun getRotations(posX: Double, posY: Double, posZ: Double): Rotation {
        val player = mc.thePlayer
        val x = posX - player.posX
        val y = posY - (player.posY + player.getEyeHeight().toDouble())
        val z = posZ - player.posZ
        val dist = MathHelper.sqrt_double(x * x + z * z).toDouble()
        val yaw = (atan2(z, x) * 180.0 / 3.141592653589793).toFloat() - 90.0f
        val pitch = (-(atan2(y, dist) * 180.0 / 3.141592653589793)).toFloat()
        return Rotation(yaw, pitch)
    }

    fun getRotationFromPosition(x: Double, z: Double, y: Double): Rotation {
        val xDiff = x - mc.thePlayer.posX
        val zDiff = z - mc.thePlayer.posZ
        val yDiff = y - mc.thePlayer.posY - 1.2
        val dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff).toDouble()
        val yaw = (atan2(zDiff, xDiff) * 180.0 / Math.PI).toFloat() - 90.0f
        val pitch = (-atan2(yDiff, dist) * 180.0 / Math.PI).toFloat()
        return Rotation(yaw, pitch)
    }

    fun calculate(from: Vec3?, to: Vec3): Rotation {
        val diff = to.subtract(from)
        val distance = hypot(diff.xCoord, diff.zCoord)
        val yaw = (MathHelper.atan2(diff.zCoord, diff.xCoord) * (180f / Math.PI)).toFloat() - 90.0f
        val pitch = (-(MathHelper.atan2(diff.yCoord, distance) * (180f / Math.PI))).toFloat()
        return Rotation(yaw, pitch)
    }

    fun calculate(to: Vec3): Rotation {
        return calculate(
            mc.thePlayer.positionVector.add(Vec3(0.0, mc.thePlayer.getEyeHeight().toDouble(), 0.0)),
            Vec3(to.xCoord, to.yCoord, to.zCoord)
        )
    }

    fun getAngles(entity: Entity?): Rotation? {
        if (entity == null) return null
        val thePlayer = mc.thePlayer
        val diffX = entity.posX - thePlayer.posX
        val diffY = entity.posY + entity.eyeHeight * 0.9 - (thePlayer.posY + thePlayer.getEyeHeight())
        val diffZ = entity.posZ - thePlayer.posZ
        val dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble() // @on
        val yaw = (atan2(diffZ, diffX) * 180.0 / Math.PI).toFloat() - 90.0f
        val pitch = -(atan2(diffY, dist) * 180.0 / Math.PI).toFloat()
        return Rotation(
            thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - thePlayer.rotationYaw),
            thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - thePlayer.rotationPitch)
        )
    }
}
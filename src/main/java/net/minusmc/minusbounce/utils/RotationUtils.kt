/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils

import com.google.common.base.Predicate
import com.google.common.base.Predicates
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

    var active: Boolean = false
    private var smoothed: Boolean = false
    private var silent: Boolean = false
    private var lastRotations: Rotation? = null
    private var rotations: Rotation? = null
    private var rotationSpeed: Float = 0f
    var type: MovementFixType = MovementFixType.NONE

    private var x = random.nextDouble()
    private var y = random.nextDouble()
    private var z = random.nextDouble()

    @JvmStatic
    fun smooth() {
        if (!smoothed) {
            targetRotation =
                limitAngleChange(lastRotations ?: return, rotations ?: return, rotationSpeed - Math.random().toFloat())
        }

        mc.entityRenderer.getMouseOver(1F)
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
    fun onMotion(event: PreMotionEvent) {/* On / Off Ground Ticks*/
        if (event.onGround) {
            offGroundTicks = 0
            onGroundTicks++
        } else {
            onGroundTicks = 0
            offGroundTicks++
        }

        if (active && targetRotation != null) {
            keepLength--

            if (this.silent) {
                targetRotation?.let {
                    event.yaw = it.yaw
                    event.pitch = it.pitch
                }
            } else {
                targetRotation!!.toPlayer(mc.thePlayer)
            }

            if (abs((targetRotation!!.yaw - mc.thePlayer.rotationYaw) % 360) < 1 && abs((targetRotation!!.pitch - mc.thePlayer.rotationPitch)) < 1) {
                active = false

                if (silent) {/* It will conflict with non-silent */
                    val targetRotation = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
                    targetRotation.fixedSensitivity(r = lastRotations!!)

                    mc.thePlayer.rotationYaw =
                        targetRotation.yaw + MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - targetRotation.yaw)
                    mc.thePlayer.rotationPitch = targetRotation.pitch
                }
            }

            mc.thePlayer.renderYawOffset = targetRotation!!.yaw
            mc.thePlayer.rotationYawHead = targetRotation!!.yaw
            lastRotations = targetRotation
        } else {
            lastRotations = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        }

        if (keepLength <= 0) {
            rotations = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        }

        smoothed = false
    }

    @EventTarget(priority = -5)
    fun onInput(e: MoveInputEvent) {
        if (type == MovementFixType.FULL && active) {
            val floats = this.handleSilentMove(e.strafe, e.forward)
            val diffForward: Float = e.lastForward - floats[1]
            val diffStrafe: Float = e.lastStrafe - floats[0]

            if (diffForward >= 2.0f) {
                floats[1] = 0.0f
            }
            if (diffForward <= -2.0f) {
                floats[1] = 0.0f
            }
            if (diffStrafe >= 2.0f) {
                floats[0] = 0.0f
            }
            if (diffStrafe <= -2.0f) {
                floats[0] = 0.0f
            }
            e.strafe = MathHelper.clamp_float(floats[0], -1.0f, 1.0f)
            e.forward = MathHelper.clamp_float(floats[1], -1.0f, 1.0f)
        }
    }

    data class Input(val strafe: Float, val forward: Float)

    @EventTarget(priority = -5)
    fun onStrafe(event: StrafeEvent) {
        if (active && (type == MovementFixType.NORMAL || type == MovementFixType.FULL)) {
            event.yaw = targetRotation?.yaw ?: return
        }
    }

    @EventTarget(priority = -5)
    fun onJump(event: JumpEvent) {
        if (active && (type == MovementFixType.NORMAL || type == MovementFixType.FULL)) {
            event.yaw = targetRotation?.yaw ?: return
        }
    }

    @EventTarget(priority = -5)
    fun onLook(event: LookEvent) {
        if (active && targetRotation != null && lastRotations != null) {
            event.yaw = targetRotation?.yaw ?: return
            event.pitch = targetRotation?.pitch ?: return
            event.lastYaw = lastRotations?.yaw ?: return
            event.lastPitch = lastRotations?.pitch ?: return
        }
    }

    private fun handleSilentMove(strafe: Float, forward: Float): FloatArray {
        val mc = Minecraft.getMinecraft()
        var newForward: Float
        var newStrafe: Float
        val realMotion = this.getMotion(0.22, strafe, forward, mc.thePlayer.rotationYaw)
        val array = doubleArrayOf(mc.thePlayer.posX, mc.thePlayer.posZ)
        val n = 0
        array[n] += realMotion[0]
        val n2 = 1
        array[n2] += realMotion[1]
        val possibleForwardStrafe = ArrayList<FloatArray>()
        var i = 0
        var b = false
        while (!b) {
            newForward = 0.0f
            newStrafe = 0.0f
            when (i) {
                0 -> {
                    newStrafe += strafe
                    newForward += forward
                    newStrafe -= forward
                    newForward += strafe
                    possibleForwardStrafe.add(floatArrayOf(newForward, newStrafe))
                }
                1 -> {
                    newStrafe -= forward
                    newForward += strafe
                    possibleForwardStrafe.add(floatArrayOf(newForward, newStrafe))
                }
                2 -> {
                    newStrafe -= strafe
                    newForward -= forward
                    newStrafe -= forward
                    newForward += strafe
                    possibleForwardStrafe.add(floatArrayOf(newForward, newStrafe))
                }
                3 -> {
                    newStrafe -= strafe
                    newForward -= forward
                    possibleForwardStrafe.add(floatArrayOf(newForward, newStrafe))
                }
                4 -> {
                    newStrafe -= strafe
                    newForward -= forward
                    newStrafe += forward
                    newForward -= strafe
                    possibleForwardStrafe.add(floatArrayOf(newForward, newStrafe))
                }
                5 -> {
                    newStrafe += forward
                    newForward -= strafe
                    possibleForwardStrafe.add(floatArrayOf(newForward, newStrafe))
                }
                6 -> {
                    newStrafe += strafe
                    newForward += forward
                    newStrafe += forward
                    newForward -= strafe
                    possibleForwardStrafe.add(floatArrayOf(newForward, newStrafe))
                }
                else -> {
                    newStrafe += strafe
                    newForward += forward
                    possibleForwardStrafe.add(floatArrayOf(newForward, newStrafe))
                    b = true
                }
            }
            ++i
        }
        var distance = 5000.0
        var floats = FloatArray(2)
        for (flo in possibleForwardStrafe) {
            if (flo[0] > 1.0f) {
                flo[0] = 1.0f
            } else if (flo[0] < -1.0f) {
                flo[0] = -1.0f
            }
            if (flo[1] > 1.0f) {
                flo[1] = 1.0f
            } else if (flo[1] < -1.0f) {
                flo[1] = -1.0f
            }
            val motion2 = this.getMotion(0.22, flo[1], flo[0], targetRotation!!.yaw)
            val n3 = 0
            motion2[n3] += mc.thePlayer.posX
            val n4 = 1
            motion2[n4] += mc.thePlayer.posZ
            val diffX = abs(array[0] - motion2[0])
            val diffZ = abs(array[1] - motion2[1])
            val d0 = diffX * diffX + diffZ * diffZ
            if (d0 < distance) {
                distance = d0
                floats = flo
            }
        }
        return floatArrayOf(floats[1], floats[0])
    }

    private fun getMotion(speed: Double, strafe: Float, forward: Float, yaw: Float): DoubleArray {
        val friction = speed.toFloat()
        val f1 = MathHelper.sin(yaw * Math.PI.toFloat() / 180.0f)
        val f2 = MathHelper.cos(yaw * Math.PI.toFloat() / 180.0f)
        val motionX = (strafe * friction * f2 - forward * friction * f1).toDouble()
        val motionZ = (forward * friction * f2 + strafe * friction * f1).toDouble()
        return doubleArrayOf(motionX, motionZ)
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
    ) {
        rotation.isNan() ?: return
        this.type = if (silent) fixType else MovementFixType.NONE
        this.rotationSpeed = speed
        this.rotations = rotation
        this.keepLength = keepLength
        this.silent = silent
        active = true

        smooth()
    }

    /**
     * @return YESSSS!!!
     */
    override fun handleEvents() = true

    fun rayTrace(range: Double, rotations: Rotation): Entity? {
        if (range == 3.0) {
            return mc.objectMouseOver.entityHit
        } else {
            val vec3 = mc.thePlayer.getPositionEyes(1.0f)
            val vec31 = getVectorForRotation(rotations)
            val vec32 = vec3.addVector(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range)
            var pointedEntity: Entity? = null
            val f = 1.0f
            val list: List<*> = mc.theWorld.getEntitiesInAABBexcluding(
                mc.renderViewEntity,
                mc.renderViewEntity.entityBoundingBox.addCoord(
                    vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range
                ).expand(f.toDouble(), f.toDouble(), f.toDouble()),
                Predicates.and(EntitySelectors.NOT_SPECTATING, Predicate { obj: Entity? -> obj!!.canBeCollidedWith() })
            )
            var d2 = range
            val var11 = list.iterator()

            while (true) {
                while (var11.hasNext()) {
                    val o = var11.next()!!
                    val entity1 = o as Entity
                    val f1 = entity1.collisionBorderSize
                    val axisalignedbb = entity1.entityBoundingBox.expand(f1.toDouble(), f1.toDouble(), f1.toDouble())
                    val movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32)
                    if (axisalignedbb.isVecInside(vec3)) {
                        if (d2 >= 0.0) {
                            pointedEntity = entity1
                            d2 = 0.0
                        }
                    } else if (movingobjectposition != null) {
                        val d3 = vec3.distanceTo(movingobjectposition.hitVec)
                        if (d3 < d2 || d2 == 0.0) {
                            if (entity1 === mc.renderViewEntity.ridingEntity) {
                                if (d2 == 0.0) {
                                    pointedEntity = entity1
                                }
                            } else {
                                pointedEntity = entity1
                                d2 = d3
                            }
                        }
                    }
                }

                return pointedEntity
            }
        }
    }

    /**
     * Face block
     *
     * @param blockPos target block
     */
    fun faceBlock(blockPos: BlockPos?): VecRotation? {
        if (blockPos == null) return null
        var vecRotation: VecRotation? = null

        for (x in 0.1..0.9) {
            for (y in 0.1..0.9) {
                for (z in 0.1..0.9) {
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
                        rotationVector.xCoord * dist, rotationVector.yCoord * dist, rotationVector.zCoord * dist
                    )
                    val obj = mc.theWorld.rayTraceBlocks(
                        eyesPos, vector, false, false, true
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
            world, blockPos, eyes, eyes + (getVectorForRotation(rotation) * reach.toDouble())
        )
    }

    fun performRayTrace(blockPos: BlockPos, vec: Vec3, eyes: Vec3 = mc.thePlayer.eyes) =
        mc.theWorld?.let { blockPos.getBlock()?.collisionRayTrace(it, blockPos, eyes, vec) }

    /**
     * Face target with bow
     *
     * @param target your enemy
     * @param silent client side rotations
     */
    fun faceBow(target: Entity, silent: Boolean) {
        var velocity = (72000 - mc.thePlayer.getItemInUseCount()) / 20.0f
        velocity = (velocity * velocity + velocity * 2.0f) / 3.0f
        if (velocity > 1.0f) {
            velocity = 1.0f
        }

        val d = mc.thePlayer.getDistanceToEntity(target) / 2.5
        val posX = target.posX + (target.posX - target.prevPosX) * d - mc.thePlayer.posX
        val posY =
            target.posY + (target.posY - target.prevPosY) * 1.0 + target.height * 0.5 - mc.thePlayer.posY - mc.thePlayer.getEyeHeight()
        val posZ = target.posZ + (target.posZ - target.prevPosZ) * d - mc.thePlayer.posZ

        val yaw = Math.toDegrees(atan2(posZ, posX)).toFloat() - 90.0f

        val hDistance = sqrt(posX * posX + posZ * posZ)
        val hDistanceSq = hDistance * hDistance
        val g = 0.006f

        val velocitySq = velocity * velocity
        val velocityPow4 = velocitySq * velocitySq

        val neededPitch = -Math.toDegrees(
            atan(
                (velocitySq - sqrt(
                    velocityPow4 - g * (g * hDistanceSq + 2.0 * posY * velocitySq)
                )) / (g * hDistance)
            )
        ).toFloat()

        setRotations(
            if (java.lang.Float.isNaN(neededPitch)) getRotations(target, 0.0, 0.0, 0.0) else Rotation(
                yaw, neededPitch
            ), silent = silent
        )
    }

    fun getRotations(ent: Entity, offsetX: Double, offsetY: Double, offsetZ: Double): Rotation {
        val eyeHeight = ent.eyeHeight.toDouble()
        var y = ent.posY
        val playerY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight().toDouble()
        if (playerY >= y + eyeHeight) {
            y += eyeHeight
            y -= 0.4
        } else if (!(playerY < y)) {
            y = playerY - 0.4
        }

        var best: Vec3 = getBestHitVec(ent)
        var nearest = 15.0
        val boundingBox = ent.entityBoundingBox

        for (x1 in boundingBox.minX..boundingBox.maxX step 0.07) {
            for (z1 in boundingBox.minZ..boundingBox.maxZ step 0.07) {
                for (y1 in boundingBox.minY..boundingBox.maxY step 0.07) {
                    val pos = Vec3(x1, y1, z1)
                    if (mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionEyes(1.0F), pos) == null) {
                        val eyes = mc.thePlayer.getPositionEyes(1.0f)
                        val dist =
                            sqrt((x1 - eyes.xCoord).pow(2.0) + (y1 - eyes.yCoord).pow(2.0) + (z1 - eyes.zCoord).pow(2.0))
                        if (dist <= nearest) {
                            nearest = dist
                            best = pos
                        }
                    }
                }
            }
        }

        return getRotationFromPosition(best.xCoord + offsetX, y - offsetY, best.zCoord + offsetZ)
    }

    fun getRotationFromPosition(x: Double, y: Double, z: Double): Rotation {
        val xDiff = x - mc.thePlayer.posX
        val zDiff = z - mc.thePlayer.posZ
        val yDiff = y - mc.thePlayer.posY - 1.2
        val dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff).toDouble()
        val yaw = (atan2(zDiff, xDiff) * 180.0 / 3.141592653589793).toFloat() - 90.0f
        val pitch = (-(atan2(yDiff, dist) * 180.0 / 3.141592653589793)).toFloat()
        return Rotation(yaw, pitch)
    }

    fun getBestHitVec(entity: Entity): Vec3 {
        val positionEyes = mc.thePlayer.getPositionEyes(1.0f)
        val f11 = entity.collisionBorderSize
        val entityBoundingBox = entity.entityBoundingBox.expand(f11.toDouble(), f11.toDouble(), f11.toDouble())
        val ex = MathHelper.clamp_double(positionEyes.xCoord, entityBoundingBox.minX, entityBoundingBox.maxX)
        val ey = MathHelper.clamp_double(positionEyes.yCoord, entityBoundingBox.minY, entityBoundingBox.maxY)
        val ez = MathHelper.clamp_double(positionEyes.zCoord, entityBoundingBox.minZ, entityBoundingBox.maxZ)
        return Vec3(ex, ey - 0.4, ez)
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
        val eyesPos =
            Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)
        if (predict) eyesPos.addVector(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)

        val (diffX, diffY, diffZ) = diff ?: (vec - eyesPos)

        return Rotation(
            MathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
            MathHelper.wrapAngleTo180_float(
                (-Math.toDegrees(
                    atan2(
                        diffY, sqrt(diffX * diffX + diffZ * diffZ)
                    )
                )).toFloat()
            )
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

        for (x in 0.0..1.0) {
            for (y in 0.0..1.0) {
                for (z in 0.0..1.0) {
                    val vec3 = Vec3(
                        bb.minX + (bb.maxX - bb.minX) * x,
                        bb.minY + (bb.maxY - bb.minY) * y,
                        bb.minZ + (bb.maxZ - bb.minZ) * z
                    )
                    val currentVec = VecRotation(
                        vec3, toRotation(
                            Vec3(0.0, 0.0, 0.0), predict, getVectorForRotation(targetRotation ?: serverRotation) - vec3
                        )
                    )
                    if (eyes.distanceTo(vec3) > distance) {
                        continue
                    }
                    if (throughWalls || isVisible(vec3)) {
                        if (vecRotation == null || if (random) {
                                getRotationDifference(currentVec.rotation, randomRotation) < getRotationDifference(
                                    vecRotation.rotation, randomRotation
                                )
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

        if (turnSpeed != 0.0F) {
            val yAdd = if (yDiff > turnSpeed) turnSpeed else yDiff.coerceAtLeast(-turnSpeed)
            val pAdd = if (pDiff > turnSpeed) turnSpeed else pDiff.coerceAtLeast(-turnSpeed)

            yaw = currentRotation.yaw + yAdd
            pitch = currentRotation.pitch + pAdd

            /* Randomize */
            for (i in 1.0..50.0 + Math.random() * 10.0 step 1.0) {
                if (abs(yAdd) + abs(pAdd) > 0.001) {
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
        return raycastEntity(blockReachDistance, object : IEntityFilter {
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
            mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ
        )
        return mc.theWorld.rayTraceBlocks(eyesPos, vec3) == null
    }
}

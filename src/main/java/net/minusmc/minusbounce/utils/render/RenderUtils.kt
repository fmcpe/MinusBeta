/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils.render

import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityEgg
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import net.minusmc.minusbounce.MinusBounce
import net.minusmc.minusbounce.features.module.modules.render.TargetMark
import net.minusmc.minusbounce.ui.font.Fonts
import net.minusmc.minusbounce.utils.MinecraftInstance
import net.minusmc.minusbounce.utils.block.BlockUtils
import net.minusmc.minusbounce.utils.particles.Particle
import net.minusmc.minusbounce.utils.render.ColorUtils.getColor
import net.minusmc.minusbounce.utils.render.ColorUtils.setColour
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.*


object RenderUtils : MinecraftInstance() {
    private val glCapMap: MutableMap<Int, Boolean> = HashMap()
    var deltaTime = 0
    private val DISPLAY_LISTS_2D = IntArray(4)
    private var startTime: Long = 0
    private const val animationDuration = 500

    init {
        for (i in DISPLAY_LISTS_2D.indices) {
            DISPLAY_LISTS_2D[i] = glGenLists(1)
        }
        glNewList(DISPLAY_LISTS_2D[0], GL_COMPILE)
        quickDrawRect(-7f, 2f, -4f, 3f)
        quickDrawRect(4f, 2f, 7f, 3f)
        quickDrawRect(-7f, 0.5f, -6f, 3f)
        quickDrawRect(6f, 0.5f, 7f, 3f)
        glEndList()
        glNewList(DISPLAY_LISTS_2D[1], GL_COMPILE)
        quickDrawRect(-7f, 3f, -4f, 3.3f)
        quickDrawRect(4f, 3f, 7f, 3.3f)
        quickDrawRect(-7.3f, 0.5f, -7f, 3.3f)
        quickDrawRect(7f, 0.5f, 7.3f, 3.3f)
        glEndList()
        glNewList(DISPLAY_LISTS_2D[2], GL_COMPILE)
        quickDrawRect(4f, -20f, 7f, -19f)
        quickDrawRect(-7f, -20f, -4f, -19f)
        quickDrawRect(6f, -20f, 7f, -17.5f)
        quickDrawRect(-7f, -20f, -6f, -17.5f)
        glEndList()
        glNewList(DISPLAY_LISTS_2D[3], GL_COMPILE)
        quickDrawRect(7f, -20f, 7.3f, -17.5f)
        quickDrawRect(-7.3f, -20f, -7f, -17.5f)
        quickDrawRect(4f, -20.3f, 7.3f, -20f)
        quickDrawRect(-7.3f, -20.3f, -4f, -20f)
        glEndList()
    }

    fun drawText(text: String?, color: Int) {
        val scaledResolution = ScaledResolution(mc)

        mc.fontRendererObj.drawString(
            text,
            (scaledResolution.scaledWidth / 2 - mc.fontRendererObj.getStringWidth(text) / 2).toFloat(),
            (scaledResolution.scaledHeight / 2 + 15).toFloat(),
            color,
            false
        )
    }

    fun renderParticles(particles: List<Particle>) {
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_LINE_SMOOTH)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        var i = 0
        try {
            for (particle in particles) {
                i++
                val v = particle.position
                var draw = true
                val x = v.xCoord - mc.renderManager.renderPosX
                val y = v.yCoord - mc.renderManager.renderPosY
                val z = v.zCoord - mc.renderManager.renderPosZ
                val distanceFromPlayer = mc.thePlayer.getDistance(v.xCoord, v.yCoord - 1, v.zCoord)
                var quality = (distanceFromPlayer * 4 + 10).toInt()
                if (quality > 350) quality = 350
                if (!isInViewFrustrum(EntityEgg(mc.theWorld, v.xCoord, v.yCoord, v.zCoord))) draw = false
                if (i % 10 != 0 && distanceFromPlayer > 25) draw = false
                if (i % 3 == 0 && distanceFromPlayer > 15) draw = false
                if (draw) {
                    glPushMatrix()
                    glTranslated(x, y, z)
                    val scale = 0.04f
                    glScalef(-scale, -scale, -scale)
                    glRotated((-mc.renderManager.playerViewY).toDouble(), 0.0, 1.0, 0.0)
                    glRotated(
                        mc.renderManager.playerViewX.toDouble(),
                        if (mc.gameSettings.thirdPersonView == 2) -1.0 else 1.0,
                        0.0,
                        0.0
                    )
                    val c = Color(getColor(-(1 + 5 * 1.7f), 0.7f, 1f))
                    drawFilledCircleNoGL(0, 0, 0.7, c.hashCode(), quality)
                    if (distanceFromPlayer < 4) drawFilledCircleNoGL(
                        0,
                        0,
                        1.4,
                        Color(c.red, c.green, c.blue, 50).hashCode(),
                        quality
                    )
                    if (distanceFromPlayer < 20) drawFilledCircleNoGL(
                        0,
                        0,
                        2.3,
                        Color(c.red, c.green, c.blue, 30).hashCode(),
                        quality
                    )
                    glScalef(0.8f, 0.8f, 0.8f)
                    glPopMatrix()
                }
            }
        } catch (ignored: ConcurrentModificationException) {
        }
        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glColor3d(255.0, 255.0, 255.0)
    }

    fun drawFilledCircleNoGL(x: Int, y: Int, r: Double, c: Int, quality: Int) {
        val f = (c shr 24 and 0xff) / 255f
        val f1 = (c shr 16 and 0xff) / 255f
        val f2 = (c shr 8 and 0xff) / 255f
        val f3 = (c and 0xff) / 255f
        glColor4f(f1, f2, f3, f)
        glBegin(GL_TRIANGLE_FAN)
        for (i in 0..360 / quality) {
            val x2 = Math.sin(i * quality * Math.PI / 180) * r
            val y2 = Math.cos(i * quality * Math.PI / 180) * r
            glVertex2d(x + x2, y + y2)
        }
        glEnd()
    }

    fun drawBoundingBox(abb: AxisAlignedBB, r: Float, g: Float, b: Float) {
        drawBoundingBox(abb, r, g, b, 0.25f)
    }

    fun drawBoundingBox(abb: AxisAlignedBB, r: Float, g: Float, b: Float, a: Float) {
        val ts = Tessellator.getInstance()
        val vb = ts.worldRenderer
        vb.begin(7, DefaultVertexFormats.POSITION_COLOR)
        vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        ts.draw()
        vb.begin(7, DefaultVertexFormats.POSITION_COLOR)
        vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        ts.draw()
        vb.begin(7, DefaultVertexFormats.POSITION_COLOR)
        vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        ts.draw()
        vb.begin(7, DefaultVertexFormats.POSITION_COLOR)
        vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        ts.draw()
        vb.begin(7, DefaultVertexFormats.POSITION_COLOR)
        vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        ts.draw()
        vb.begin(7, DefaultVertexFormats.POSITION_COLOR)
        vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex()
        vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex()
        ts.draw()
    }

    private fun quickPolygonCircle(x: Float, y: Float, xRadius: Float, yRadius: Float, start: Int, end: Int) {
        var i = end
        while (i >= start) {
            glVertex2d(x + Math.sin(i * Math.PI / 180.0) * xRadius, y + Math.cos(i * Math.PI / 180.0) * yRadius)
            i -= 4
        }
    }

    fun drawRoundedCornerRect(x: Float, y: Float, x1: Float, y1: Float, radius: Float, color: Int) {
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_TEXTURE_2D)
        val hasCull = glIsEnabled(GL_CULL_FACE)
        glDisable(GL_CULL_FACE)
        glColor(color)
        drawRoundedCornerRect(x, y, x1, y1, radius)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        setGlState(GL_CULL_FACE, hasCull)
    }

    fun drawRoundedCornerRect(x: Float, y: Float, x1: Float, y1: Float, radius: Float) {
        glBegin(GL_POLYGON)
        val xRadius = Math.min((x1 - x) * 0.5, radius.toDouble()).toFloat()
        val yRadius = Math.min((y1 - y) * 0.5, radius.toDouble()).toFloat()
        quickPolygonCircle(x + xRadius, y + yRadius, xRadius, yRadius, 180, 270)
        quickPolygonCircle(x1 - xRadius, y + yRadius, xRadius, yRadius, 90, 180)
        quickPolygonCircle(x1 - xRadius, y1 - yRadius, xRadius, yRadius, 0, 90)
        quickPolygonCircle(x + xRadius, y1 - yRadius, xRadius, yRadius, 270, 360)
        glEnd()
    }

    fun color(color: Int) = color(color, ((color shr 24 and 0xFF) / 255).toFloat())

    fun color(color: Int, alpha: Float) {
        val r = (color shr 16 and 0xFF) / 255.0f
        val g = (color shr 8 and 0xFF) / 255.0f
        val b = (color and 0xFF) / 255.0f
        color(r, g, b, alpha)
    }

    private val frustrum = Frustum()
    internal var zLevel = 0f

    fun drawTexturedModalRect(x: Int, y: Int, textureX: Int, textureY: Int, width: Int, height: Int, zLevel: Float) {
        val f = 0.00390625f
        val f1 = 0.00390625f
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
        worldrenderer.pos((x + 0).toDouble(), (y + height).toDouble(), zLevel.toDouble())
            .tex(((textureX + 0).toFloat() * f).toDouble(), ((textureY + height).toFloat() * f1).toDouble()).endVertex()
        worldrenderer.pos((x + width).toDouble(), (y + height).toDouble(), zLevel.toDouble())
            .tex(((textureX + width).toFloat() * f).toDouble(), ((textureY + height).toFloat() * f1).toDouble())
            .endVertex()
        worldrenderer.pos((x + width).toDouble(), (y + 0).toDouble(), zLevel.toDouble())
            .tex(((textureX + width).toFloat() * f).toDouble(), ((textureY + 0).toFloat() * f1).toDouble()).endVertex()
        worldrenderer.pos((x + 0).toDouble(), (y + 0).toDouble(), zLevel.toDouble())
            .tex(((textureX + 0).toFloat() * f).toDouble(), ((textureY + 0).toFloat() * f1).toDouble()).endVertex()
        tessellator.draw()
    }

    fun drawHead(skin: ResourceLocation, x: Int, y: Int, width: Int, height: Int) {
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDepthMask(false)
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(1f, 1f, 1f, 1f)
        mc.textureManager.bindTexture(skin)
        Gui.drawScaledCustomSizeModalRect(x, y, 8F, 8F, 8, 8, width, height, 64F, 64F)
        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    fun isInViewFrustrum(entity: Entity): Boolean {
        return isInViewFrustrum(entity.entityBoundingBox) || entity.ignoreFrustumCheck
    }

    private fun isInViewFrustrum(bb: AxisAlignedBB): Boolean {
        val current = mc.renderViewEntity
        frustrum.setPosition(current.posX, current.posY, current.posZ)
        return frustrum.isBoundingBoxInFrustum(bb)
    }

    fun interpolate(current: Double, old: Double, scale: Double): Double {
        return old + (current - old) * scale
    }

    fun originalRoundedRect(
        paramXStart: Float,
        paramYStart: Float,
        paramXEnd: Float,
        paramYEnd: Float,
        radius: Float,
        color: Int,
    ) {
        var paramXStart = paramXStart
        var paramYStart = paramYStart
        var paramXEnd = paramXEnd
        var paramYEnd = paramYEnd
        val alpha = (color shr 24 and 0xFF) / 255.0f
        val red = (color shr 16 and 0xFF) / 255.0f
        val green = (color shr 8 and 0xFF) / 255.0f
        val blue = (color and 0xFF) / 255.0f
        var z = 0f
        if (paramXStart > paramXEnd) {
            z = paramXStart
            paramXStart = paramXEnd
            paramXEnd = z
        }
        if (paramYStart > paramYEnd) {
            z = paramYStart
            paramYStart = paramYEnd
            paramYEnd = z
        }
        val x1 = (paramXStart + radius).toDouble()
        val y1 = (paramYStart + radius).toDouble()
        val x2 = (paramXEnd - radius).toDouble()
        val y2 = (paramYEnd - radius).toDouble()
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(red, green, blue, alpha)
        worldrenderer.begin(GL_POLYGON, DefaultVertexFormats.POSITION)
        val degree = Math.PI / 180
        run {
            var i = 0.0
            while (i <= 90) {
                worldrenderer.pos(
                    x2 + sin(i * degree) * radius,
                    y2 + cos(i * degree) * radius,
                    0.0
                ).endVertex()
                i += 1.0
            }
        }
        run {
            var i = 90.0
            while (i <= 180) {
                worldrenderer.pos(
                    x2 + sin(i * degree) * radius,
                    y1 + cos(i * degree) * radius,
                    0.0
                ).endVertex()
                i += 1.0
            }
        }
        run {
            var i = 180.0
            while (i <= 270) {
                worldrenderer.pos(
                    x1 + sin(i * degree) * radius,
                    y1 + cos(i * degree) * radius,
                    0.0
                ).endVertex()
                i += 1.0
            }
        }
        var i = 270.0
        while (i <= 360) {
            worldrenderer.pos(x1 + sin(i * degree) * radius, y2 + cos(i * degree) * radius, 0.0).endVertex()
            i += 1.0
        }
        tessellator.draw()
        enableTexture2D()
        disableBlend()
    }

    fun newDrawRect(left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        newDrawRect(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble(), color)
    }

    fun newDrawRect(left: Double, top: Double, right: Double, bottom: Double, color: Int) {
        var left = left
        var top = top
        var right = right
        var bottom = bottom
        if (left < right) {
            val i = left
            left = right
            right = i
        }
        if (top < bottom) {
            val j = top
            top = bottom
            bottom = j
        }
        val f3 = (color shr 24 and 255).toFloat() / 255.0f
        val f = (color shr 16 and 255).toFloat() / 255.0f
        val f1 = (color shr 8 and 255).toFloat() / 255.0f
        val f2 = (color and 255).toFloat() / 255.0f
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(f, f1, f2, f3)
        worldrenderer.begin(7, DefaultVertexFormats.POSITION)
        worldrenderer.pos(left, bottom, 0.0).endVertex()
        worldrenderer.pos(right, bottom, 0.0).endVertex()
        worldrenderer.pos(right, top, 0.0).endVertex()
        worldrenderer.pos(left, top, 0.0).endVertex()
        tessellator.draw()
        enableTexture2D()
        disableBlend()
    }

    @JvmOverloads
    fun drawRoundedRect(
        paramXStart: Float,
        paramYStart: Float,
        paramXEnd: Float,
        paramYEnd: Float,
        radius: Float,
        color: Int,
        popPush: Boolean = true,
    ) {
        var paramXStart = paramXStart
        var paramYStart = paramYStart
        var paramXEnd = paramXEnd
        var paramYEnd = paramYEnd
        val alpha = (color shr 24 and 0xFF) / 255.0f
        val red = (color shr 16 and 0xFF) / 255.0f
        val green = (color shr 8 and 0xFF) / 255.0f
        val blue = (color and 0xFF) / 255.0f
        var z = 0f
        if (paramXStart > paramXEnd) {
            z = paramXStart
            paramXStart = paramXEnd
            paramXEnd = z
        }
        if (paramYStart > paramYEnd) {
            z = paramYStart
            paramYStart = paramYEnd
            paramYEnd = z
        }
        val x1 = (paramXStart + radius).toDouble()
        val y1 = (paramYStart + radius).toDouble()
        val x2 = (paramXEnd - radius).toDouble()
        val y2 = (paramYEnd - radius).toDouble()
        if (popPush) glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(1f)
        glColor4f(red, green, blue, alpha)
        glBegin(GL_POLYGON)
        val degree = Math.PI / 180
        run {
            var i = 0.0
            while (i <= 90) {
                glVertex2d(x2 + sin(i * degree) * radius, y2 + cos(i * degree) * radius)
                i += 1.0
            }
        }
        run {
            var i = 90.0
            while (i <= 180) {
                glVertex2d(x2 + sin(i * degree) * radius, y1 + cos(i * degree) * radius)
                i += 1.0
            }
        }
        run {
            var i = 180.0
            while (i <= 270) {
                glVertex2d(x1 + sin(i * degree) * radius, y1 + cos(i * degree) * radius)
                i += 1.0
            }
        }
        var i = 270.0
        while (i <= 360) {
            glVertex2d(x1 + sin(i * degree) * radius, y2 + cos(i * degree) * radius)
            i += 1.0
        }
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        if (popPush) glPopMatrix()
    }

    // rTL = radius top left, rTR = radius top right, rBR = radius bottom right, rBL = radius bottom left
    fun customRounded(
        paramXStart: Float,
        paramYStart: Float,
        paramXEnd: Float,
        paramYEnd: Float,
        rTL: Float,
        rTR: Float,
        rBR: Float,
        rBL: Float,
        color: Int,
    ) {
        var paramXStart = paramXStart
        var paramYStart = paramYStart
        var paramXEnd = paramXEnd
        var paramYEnd = paramYEnd
        val alpha = (color shr 24 and 0xFF) / 255.0f
        val red = (color shr 16 and 0xFF) / 255.0f
        val green = (color shr 8 and 0xFF) / 255.0f
        val blue = (color and 0xFF) / 255.0f
        var z = 0f
        if (paramXStart > paramXEnd) {
            z = paramXStart
            paramXStart = paramXEnd
            paramXEnd = z
        }
        if (paramYStart > paramYEnd) {
            z = paramYStart
            paramYStart = paramYEnd
            paramYEnd = z
        }
        val xTL = (paramXStart + rTL).toDouble()
        val yTL = (paramYStart + rTL).toDouble()
        val xTR = (paramXEnd - rTR).toDouble()
        val yTR = (paramYStart + rTR).toDouble()
        val xBR = (paramXEnd - rBR).toDouble()
        val yBR = (paramYEnd - rBR).toDouble()
        val xBL = (paramXStart + rBL).toDouble()
        val yBL = (paramYEnd - rBL).toDouble()
        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(1f)
        glColor4f(red, green, blue, alpha)
        glBegin(GL_POLYGON)
        val degree = Math.PI / 180
        if (rBR <= 0) glVertex2d(xBR, yBR) else {
            var i = 0.0
            while (i <= 90) {
                glVertex2d(xBR + sin(i * degree) * rBR, yBR + cos(i * degree) * rBR)
                i += 1.0
            }
        }
        if (rTR <= 0) glVertex2d(xTR, yTR) else {
            var i = 90.0
            while (i <= 180) {
                glVertex2d(xTR + sin(i * degree) * rTR, yTR + cos(i * degree) * rTR)
                i += 1.0
            }
        }
        if (rTL <= 0) glVertex2d(xTL, yTL) else {
            var i = 180.0
            while (i <= 270) {
                glVertex2d(xTL + sin(i * degree) * rTL, yTL + cos(i * degree) * rTL)
                i += 1.0
            }
        }
        if (rBL <= 0) glVertex2d(xBL, yBL) else {
            var i = 270.0
            while (i <= 360) {
                glVertex2d(xBL + sin(i * degree) * rBL, yBL + cos(i * degree) * rBL)
                i += 1.0
            }
        }
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glPopMatrix()
    }

    fun fastRoundedRect(paramXStart: Float, paramYStart: Float, paramXEnd: Float, paramYEnd: Float, radius: Float) {
        var paramXStart = paramXStart
        var paramYStart = paramYStart
        var paramXEnd = paramXEnd
        var paramYEnd = paramYEnd
        var z = 0f
        if (paramXStart > paramXEnd) {
            z = paramXStart
            paramXStart = paramXEnd
            paramXEnd = z
        }
        if (paramYStart > paramYEnd) {
            z = paramYStart
            paramYStart = paramYEnd
            paramYEnd = z
        }
        val x1 = (paramXStart + radius).toDouble()
        val y1 = (paramYStart + radius).toDouble()
        val x2 = (paramXEnd - radius).toDouble()
        val y2 = (paramYEnd - radius).toDouble()
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(1f)
        glBegin(GL_POLYGON)
        val degree = Math.PI / 180
        run {
            var i = 0.0
            while (i <= 90) {
                glVertex2d(x2 + sin(i * degree) * radius, y2 + cos(i * degree) * radius)
                i += 1.0
            }
        }
        run {
            var i = 90.0
            while (i <= 180) {
                glVertex2d(x2 + sin(i * degree) * radius, y1 + cos(i * degree) * radius)
                i += 1.0
            }
        }
        run {
            var i = 180.0
            while (i <= 270) {
                glVertex2d(x1 + sin(i * degree) * radius, y1 + cos(i * degree) * radius)
                i += 1.0
            }
        }
        var i = 270.0
        while (i <= 360) {
            glVertex2d(x1 + sin(i * degree) * radius, y2 + cos(i * degree) * radius)
            i += 1.0
        }
        glEnd()
        glDisable(GL_LINE_SMOOTH)
    }

    fun drawGradientSideways(left: Double, top: Double, right: Double, bottom: Double, col1: Int, col2: Int) {
        val f = (col1 shr 24 and 0xFF) / 255.0f
        val f2 = (col1 shr 16 and 0xFF) / 255.0f
        val f3 = (col1 shr 8 and 0xFF) / 255.0f
        val f4 = (col1 and 0xFF) / 255.0f
        val f5 = (col2 shr 24 and 0xFF) / 255.0f
        val f6 = (col2 shr 16 and 0xFF) / 255.0f
        val f7 = (col2 shr 8 and 0xFF) / 255.0f
        val f8 = (col2 and 0xFF) / 255.0f
        glEnable(3042)
        glDisable(3553)
        glBlendFunc(770, 771)
        glEnable(2848)
        glShadeModel(7425)
        glPushMatrix()
        glBegin(7)
        glColor4f(f2, f3, f4, f)
        glVertex2d(left, top)
        glVertex2d(left, bottom)
        glColor4f(f6, f7, f8, f5)
        glVertex2d(right, bottom)
        glVertex2d(right, top)
        glEnd()
        glPopMatrix()
        glEnable(3553)
        glDisable(3042)
        glDisable(2848)
        glShadeModel(7424)
    }

    fun drawGradientRect(left: Int, top: Int, right: Int, bottom: Int, startColor: Int, endColor: Int) {
        val f = (startColor shr 24 and 255).toFloat() / 255.0f
        val f1 = (startColor shr 16 and 255).toFloat() / 255.0f
        val f2 = (startColor shr 8 and 255).toFloat() / 255.0f
        val f3 = (startColor and 255).toFloat() / 255.0f
        val f4 = (endColor shr 24 and 255).toFloat() / 255.0f
        val f5 = (endColor shr 16 and 255).toFloat() / 255.0f
        val f6 = (endColor shr 8 and 255).toFloat() / 255.0f
        val f7 = (endColor and 255).toFloat() / 255.0f
        GlStateManager.pushMatrix()
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.shadeModel(7425)
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR)
        worldrenderer.pos(right.toDouble(), top.toDouble(), zLevel.toDouble()).color(f1, f2, f3, f).endVertex()
        worldrenderer.pos(left.toDouble(), top.toDouble(), zLevel.toDouble()).color(f1, f2, f3, f).endVertex()
        worldrenderer.pos(left.toDouble(), bottom.toDouble(), zLevel.toDouble()).color(f5, f6, f7, f4).endVertex()
        worldrenderer.pos(right.toDouble(), bottom.toDouble(), zLevel.toDouble()).color(f5, f6, f7, f4).endVertex()
        tessellator.draw()
        GlStateManager.shadeModel(7424)
        disableBlend()
        GlStateManager.enableAlpha()
        enableTexture2D()
        GlStateManager.popMatrix()
    }

    fun drawGradientSideways(left: Float, top: Float, right: Float, bottom: Float, col1: Int, col2: Int) {
        val f = (col1 shr 24 and 0xFF) / 255.0f
        val f2 = (col1 shr 16 and 0xFF) / 255.0f
        val f3 = (col1 shr 8 and 0xFF) / 255.0f
        val f4 = (col1 and 0xFF) / 255.0f
        val f5 = (col2 shr 24 and 0xFF) / 255.0f
        val f6 = (col2 shr 16 and 0xFF) / 255.0f
        val f7 = (col2 shr 8 and 0xFF) / 255.0f
        val f8 = (col2 and 0xFF) / 255.0f
        glEnable(3042)
        glDisable(3553)
        glBlendFunc(770, 771)
        glEnable(2848)
        glShadeModel(7425)
        glPushMatrix()
        glBegin(7)
        glColor4f(f2, f3, f4, f)
        glVertex2f(left, top)
        glVertex2f(left, bottom)
        glColor4f(f6, f7, f8, f5)
        glVertex2f(right, bottom)
        glVertex2f(right, top)
        glEnd()
        glPopMatrix()
        glEnable(3553)
        glDisable(3042)
        glDisable(2848)
        glShadeModel(7424)
    }

    fun drawBlockBox(blockPos: BlockPos, color: Color, outline: Boolean) {
        val renderManager = mc.renderManager
        val timer = mc.timer
        val x = blockPos.x - renderManager.renderPosX
        val y = blockPos.y - renderManager.renderPosY
        val z = blockPos.z - renderManager.renderPosZ
        var axisAlignedBB = AxisAlignedBB(x, y, z, x + 1.0, y + 1, z + 1.0)
        val block = BlockUtils.getBlock(blockPos)
        if (block != null) {
            val player: EntityPlayer = mc.thePlayer
            val posX = player.lastTickPosX + (player.posX - player.lastTickPosX) * timer.renderPartialTicks.toDouble()
            val posY = player.lastTickPosY + (player.posY - player.lastTickPosY) * timer.renderPartialTicks.toDouble()
            val posZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * timer.renderPartialTicks.toDouble()
            axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
                .expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
                .offset(-posX, -posY, -posZ)
        }
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        enableGlCap(GL_BLEND)
        disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST)
        glDepthMask(false)
        glColor(color.red, color.green, color.blue, if (color.alpha != 255) color.alpha else if (outline) 26 else 35)
        drawFilledBox(axisAlignedBB)
        if (outline) {
            glLineWidth(1f)
            enableGlCap(GL_LINE_SMOOTH)
            glColor(color)
            drawSelectionBoundingBox(axisAlignedBB)
        }
        GlStateManager.resetColor()
        glDepthMask(true)
        resetCaps()
    }

    fun drawShadow(x: Float, y: Float, width: Float, height: Float) {
        drawTexturedRect(x - 9, y - 9, 9f, 9f, "paneltopleft")
        drawTexturedRect(x - 9, y + height, 9f, 9f, "panelbottomleft")
        drawTexturedRect(x + width, y + height, 9f, 9f, "panelbottomright")
        drawTexturedRect(x + width, y - 9, 9f, 9f, "paneltopright")
        drawTexturedRect(x - 9, y, 9f, height, "panelleft")
        drawTexturedRect(x + width, y, 9f, height, "panelright")
        drawTexturedRect(x, y - 9, width, 9f, "paneltop")
        drawTexturedRect(x, y + height, width, 9f, "panelbottom")
    }

    fun drawTexturedRect(x: Float, y: Float, width: Float, height: Float, image: String) {
        glPushMatrix()
        val enableBlend = glIsEnabled(GL_BLEND)
        val disableAlpha = !glIsEnabled(GL_ALPHA_TEST)
        if (!enableBlend) glEnable(GL_BLEND)
        if (!disableAlpha) glDisable(GL_ALPHA_TEST)
        mc.textureManager.bindTexture(ResourceLocation("liquidbounce+/ui/$image.png"))
        GlStateManager.color(1f, 1f, 1f, 1f)
        drawModalRectWithCustomSizedTexture(x.toInt(), y.toInt(), 0f, 0f, width.toInt(), height.toInt(), width, height)
        if (!enableBlend) glDisable(GL_BLEND)
        if (!disableAlpha) glEnable(GL_ALPHA_TEST)
        glPopMatrix()
    }

    fun drawSelectionBoundingBox(boundingBox: AxisAlignedBB) {
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)

        // Lower Rectangle
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex()
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex()
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex()
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex()
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex()

        // Upper Rectangle
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex()
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex()
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex()
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex()
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex()

        // Upper Rectangle
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex()
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex()
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex()
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex()
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex()
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex()
        tessellator.draw()
    }

    fun drawEntityBox(entity: Entity, color: Color, outline: Boolean) {
        val renderManager = mc.renderManager
        val timer = mc.timer
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        enableGlCap(GL_BLEND)
        disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST)
        glDepthMask(false)
        val x = (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks
                - renderManager.renderPosX)
        val y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks
                - renderManager.renderPosY)
        val z = (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks
                - renderManager.renderPosZ)
        val entityBox = entity.entityBoundingBox
        val axisAlignedBB = AxisAlignedBB(
            entityBox.minX - entity.posX + x - 0.05,
            entityBox.minY - entity.posY + y,
            entityBox.minZ - entity.posZ + z - 0.05,
            entityBox.maxX - entity.posX + x + 0.05,
            entityBox.maxY - entity.posY + y + 0.15,
            entityBox.maxZ - entity.posZ + z + 0.05
        )
        if (outline) {
            glLineWidth(1f)
            enableGlCap(GL_LINE_SMOOTH)
            glColor(color.red, color.green, color.blue, 95)
            drawSelectionBoundingBox(axisAlignedBB)
        }
        glColor(color.red, color.green, color.blue, if (outline) 26 else 35)
        drawFilledBox(axisAlignedBB)
        resetColor()
        glDepthMask(true)
        resetCaps()
    }

    fun drawAxisAlignedBB(axisAlignedBB: AxisAlignedBB, color: Color) {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glLineWidth(2f)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)
        glColor(color)
        drawFilledBox(axisAlignedBB)
        GlStateManager.resetColor()
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDisable(GL_BLEND)
    }

    fun drawPlatform(y: Double, color: Color, size: Double) {
        val renderManager = mc.renderManager
        val renderY = y - renderManager.renderPosY
        drawAxisAlignedBB(AxisAlignedBB(size, renderY + 0.02, size, -size, renderY, -size), color)
    }

    fun drawPlatform(entity: Entity, color: Color) {
        val renderManager = mc.renderManager
        val timer = mc.timer
        val targetMark = MinusBounce.moduleManager.getModule(TargetMark::class.java) ?: return
        val x = (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks
                - renderManager.renderPosX)
        val y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks
                - renderManager.renderPosY)
        val z = (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks
                - renderManager.renderPosZ)
        val axisAlignedBB = entity.entityBoundingBox
            .offset(-entity.posX, -entity.posY, -entity.posZ)
            .offset(x, y - targetMark.moveMarkValue.get(), z)
        drawAxisAlignedBB(
            AxisAlignedBB(
                axisAlignedBB.minX,
                axisAlignedBB.maxY + 0.2,
                axisAlignedBB.minZ,
                axisAlignedBB.maxX,
                axisAlignedBB.maxY + 0.26,
                axisAlignedBB.maxZ
            ),
            color
        )
    }

    fun drawFilledBox(axisAlignedBB: AxisAlignedBB) {
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(7, DefaultVertexFormats.POSITION)
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        tessellator.draw()
    }

    fun drawEntityOnScreen(posX: Double, posY: Double, scale: Float, entity: EntityLivingBase?) {
        GlStateManager.pushMatrix()
        GlStateManager.enableColorMaterial()
        GlStateManager.translate(posX, posY, 50.0)
        GlStateManager.scale(-scale, scale, scale)
        GlStateManager.rotate(180f, 0f, 0f, 1f)
        GlStateManager.rotate(135f, 0f, 1f, 0f)
        RenderHelper.enableStandardItemLighting()
        GlStateManager.rotate(-135f, 0f, 1f, 0f)
        GlStateManager.translate(0.0, 0.0, 0.0)
        val rendermanager = mc.renderManager
        rendermanager.setPlayerViewY(180f)
        rendermanager.isRenderShadow = false
        rendermanager.renderEntityWithPosYaw(entity, 0.0, 0.0, 0.0, 0f, 1f)
        rendermanager.isRenderShadow = true
        GlStateManager.popMatrix()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit)
        GlStateManager.disableTexture2D()
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit)
    }

    fun drawEntityOnScreen(posX: Int, posY: Int, scale: Int, entity: EntityLivingBase?) {
        drawEntityOnScreen(posX.toDouble(), posY.toDouble(), scale.toFloat(), entity)
    }

    fun quickDrawRect(x: Float, y: Float, x2: Float, y2: Float) {
        glBegin(GL_QUADS)
        glVertex2d(x2.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y2.toDouble())
        glVertex2d(x2.toDouble(), y2.toDouble())
        glEnd()
    }

    fun drawRect(x: Number, y: Number, x2: Number, y2: Number, color: Int) {
        drawRect(x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), color)
    }

    fun drawRect(x: Float, y: Float, x2: Float, y2: Float, color: Int) {
        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glColor(color)
        glBegin(GL_QUADS)
        glVertex2d(x2.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y2.toDouble())
        glVertex2d(x2.toDouble(), y2.toDouble())
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glPopMatrix()
    }

    fun drawRect(left: Double, top: Double, right: Double, bottom: Double, color: Int) {
        var left = left
        var top = top
        var right = right
        var bottom = bottom
        if (left < right) {
            val i = left
            left = right
            right = i
        }
        if (top < bottom) {
            val j = top
            top = bottom
            bottom = j
        }
        val f3 = (color shr 24 and 255).toFloat() / 255.0f
        val f = (color shr 16 and 255).toFloat() / 255.0f
        val f1 = (color shr 8 and 255).toFloat() / 255.0f
        val f2 = (color and 255).toFloat() / 255.0f
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(f, f1, f2, f3)
        worldrenderer.begin(7, DefaultVertexFormats.POSITION)
        worldrenderer.pos(left, bottom, 0.0).endVertex()
        worldrenderer.pos(right, bottom, 0.0).endVertex()
        worldrenderer.pos(right, top, 0.0).endVertex()
        worldrenderer.pos(left, top, 0.0).endVertex()
        tessellator.draw()
        enableTexture2D()
        disableBlend()
    }

    fun drawRect(rect: net.minusmc.minusbounce.utils.geom.Rectangle, color: Int) {
        drawRect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, color)
    }

    fun drawRect(rect: net.minusmc.minusbounce.utils.geom.Rectangle, color: Color) {
        drawRect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, color.rgb)
    }

    /**
     * Like [.drawRect], but without setup
     */
    fun quickDrawRect(x: Float, y: Float, x2: Float, y2: Float, color: Int) {
        glColor(color)
        glBegin(GL_QUADS)
        glVertex2d(x2.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y2.toDouble())
        glVertex2d(x2.toDouble(), y2.toDouble())
        glEnd()
    }

    fun drawRect(x: Float, y: Float, x2: Float, y2: Float, color: Color) {
        drawRect(x, y, x2, y2, color.rgb)
    }

    fun drawBorderedRect(
        x: Float, y: Float, x2: Float, y2: Float, width: Float,
        color1: Int, color2: Int,
    ) {
        drawRect(x, y, x2, y2, color2)
        drawBorder(x, y, x2, y2, width, color1)
    }

    //Insane override func xd
    fun drawBorderedRect(
        x: Number, y: Number, x2: Number, y2: Number, width: Number,
        color1: Int, color2: Int,
    ){
        drawBorderedRect(x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), width.toFloat(), color1, color2)
    }
    fun drawBorder(x: Float, y: Float, x2: Float, y2: Float, width: Float, color1: Int) {
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glColor(color1)
        glLineWidth(width)
        glBegin(GL_LINE_LOOP)
        glVertex2d(x2.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y2.toDouble())
        glVertex2d(x2.toDouble(), y2.toDouble())
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
    }

    fun drawRectBasedBorder(x: Float, y: Float, x2: Float, y2: Float, width: Float, color1: Int) {
        drawRect(x - width / 2f, y - width / 2f, x2 + width / 2f, y + width / 2f, color1)
        drawRect(x - width / 2f, y + width / 2f, x + width / 2f, y2 + width / 2f, color1)
        drawRect(x2 - width / 2f, y + width / 2f, x2 + width / 2f, y2 + width / 2f, color1)
        drawRect(x + width / 2f, y2 - width / 2f, x2 - width / 2f, y2 + width / 2f, color1)
    }

    fun drawRectBasedBorder(x: Double, y: Double, x2: Double, y2: Double, width: Double, color1: Int) {
        newDrawRect(x - width / 2f, y - width / 2f, x2 + width / 2f, y + width / 2f, color1)
        newDrawRect(x - width / 2f, y + width / 2f, x + width / 2f, y2 + width / 2f, color1)
        newDrawRect(x2 - width / 2f, y + width / 2f, x2 + width / 2f, y2 + width / 2f, color1)
        newDrawRect(x + width / 2f, y2 - width / 2f, x2 - width / 2f, y2 + width / 2f, color1)
    }

    fun quickDrawBorderedRect(x: Float, y: Float, x2: Float, y2: Float, width: Float, color1: Int, color2: Int) {
        quickDrawRect(x, y, x2, y2, color2)
        glColor(color1)
        glLineWidth(width)
        glBegin(GL_LINE_LOOP)
        glVertex2d(x2.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y2.toDouble())
        glVertex2d(x2.toDouble(), y2.toDouble())
        glEnd()
    }

    fun drawLoadingCircle(x: Float, y: Float) {
        for (i in 0..3) {
            val rot = (System.nanoTime() / 5000000 * i % 360).toInt()
            drawCircle(x, y, (i * 10).toFloat(), rot - 180, rot)
        }
    }

    fun drawCircle(x: Float, y: Float, radius: Float, lineWidth: Float, start: Int, end: Int, color: Color) {
        glColor(color)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor(color)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(lineWidth)
        glBegin(GL_LINE_STRIP)
        var i = end.toFloat()
        while (i >= start) {
            glVertex2f(
                (x + cos(i * Math.PI / 180) * (radius * 1.001f)).toFloat(),
                (y + sin(i * Math.PI / 180) * (radius * 1.001f)).toFloat()
            )
            i -= 360 / 90.0f
        }
        glEnd()
        glDisable(GL_LINE_SMOOTH)
        enableTexture2D()
        disableBlend()
    }

    fun drawCircle(x: Float, y: Float, radius: Float, lineWidth: Float, start: Int, end: Int) {
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor(Color.WHITE)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(lineWidth)
        glBegin(GL_LINE_STRIP)
        var i = end.toFloat()
        while (i >= start) {
            glVertex2f(
                (x + cos(i * Math.PI / 180) * (radius * 1.001f)).toFloat(),
                (y + sin(i * Math.PI / 180) * (radius * 1.001f)).toFloat()
            )
            i -= 360 / 90.0f
        }
        glEnd()
        glDisable(GL_LINE_SMOOTH)
        enableTexture2D()
        disableBlend()
    }

    fun drawCircle(x: Float, y: Float, radius: Float, start: Int, end: Int) {
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor(Color.WHITE)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(2f)
        glBegin(GL_LINE_STRIP)
        var i = end.toFloat()
        while (i >= start) {
            glVertex2f(
                (x + cos(i * Math.PI / 180) * (radius * 1.001f)).toFloat(),
                (y + sin(i * Math.PI / 180) * (radius * 1.001f)).toFloat()
            )
            i -= 360 / 90.0f
        }
        glEnd()
        glDisable(GL_LINE_SMOOTH)
        enableTexture2D()
        disableBlend()
    }

    fun drawGradientCircle(x: Float, y: Float, radius: Float, start: Int, end: Int, color1: Color, color2: Color) {
        enableBlend()
        disableTexture2D()
        tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(2f)
        glBegin(GL_LINE_STRIP)
        var i = end.toFloat()
        while (i >= start) {
            var c = ColorUtils.getGradientOffset(color1, color2, 1.0, (abs(System.currentTimeMillis() / 360.0 + (i * 34 / 360) * 56 / 100) / 10).toInt()).rgb
            val f2 = (c shr 24 and 255).toFloat() / 255.0f
            val f22 = (c shr 16 and 255).toFloat() / 255.0f
            val f3 = (c shr 8 and 255).toFloat() / 255.0f
            val f4 = (c and 255).toFloat() / 255.0f
            color(f22, f3, f4, f2)
            glVertex2f(
                (x + Math.cos(i * Math.PI / 180) * (radius * 1.001f)).toFloat(),
                (y + Math.sin(i * Math.PI / 180) * (radius * 1.001f)).toFloat()
            )
            i -= 360f / 90.0f
        }
        glEnd()
        glDisable(GL_LINE_SMOOTH)
        enableTexture2D()
        disableBlend()
    }

    fun drawFilledCircle(xx: Int, yy: Int, radius: Float, color: Color) {
        val sections = 50
        val dAngle = 2 * Math.PI / sections
        var x: Float
        var y: Float
        glPushAttrib(GL_ENABLE_BIT)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glBegin(GL_TRIANGLE_FAN)
        for (i in 0 until sections) {
            x = (radius * sin(i * dAngle)).toFloat()
            y = (radius * cos(i * dAngle)).toFloat()
            glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
            glVertex2f(xx + x, yy + y)
        }
        GlStateManager.color(0f, 0f, 0f)
        glEnd()
        glPopAttrib()
    }

    fun drawFilledCircle(xx: Float, yy: Float, radius: Float, color: Color) {
        val sections = 50
        val dAngle = 2 * Math.PI / sections
        var x: Float
        var y: Float
        glPushAttrib(GL_ENABLE_BIT)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glBegin(GL_TRIANGLE_FAN)
        for (i in 0 until sections) {
            x = (radius * sin(i * dAngle)).toFloat()
            y = (radius * cos(i * dAngle)).toFloat()
            glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
            glVertex2f(xx + x, yy + y)
        }
        GlStateManager.color(0f, 0f, 0f)
        glEnd()
        glPopAttrib()
    }

    fun drawImage(image: ResourceLocation?, x: Int, y: Int, width: Int, height: Int) {
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDepthMask(false)
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        mc.textureManager.bindTexture(image)
        drawModalRectWithCustomSizedTexture(x.toInt(),
            y.toInt(), 0f, 0f, width.toInt(), height.toInt(), width.toFloat(), height.toFloat())
        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    fun drawImage(image: ResourceLocation?, x: Int, y: Int, width: Int, height: Int, alpha: Float) {
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDepthMask(false)
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(1.0f, 1.0f, 1.0f, alpha)
        mc.textureManager.bindTexture(image)
        drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, width, height, width.toFloat(), height.toFloat())
        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    fun drawImagee(image: ResourceLocation?, x: Double, y: Double, width: Double, height: Double) {
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDepthMask(false)
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        mc.textureManager.bindTexture(image)
        drawModalRectWithCustomSizedTexture(x.toInt(),
            y.toInt(), 0f, 0f, width.toInt(), height.toInt(), width.toFloat(), height.toFloat())
        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    fun drawImagee(image: ResourceLocation?, x: Int, y: Int, width: Int, height: Int, alpha: Float) {
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDepthMask(false)
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(1.0f, 1.0f, 1.0f, alpha)
        mc.textureManager.bindTexture(image)
        drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, width, height, width.toFloat(), height.toFloat())
        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    fun drawImage2(image: ResourceLocation?, x: Float, y: Float, width: Int, height: Int) {
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDepthMask(false)
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        glTranslatef(x, y, x)
        mc.textureManager.bindTexture(image)
        drawModalRectWithCustomSizedTexture(0, 0, 0f, 0f, width, height, width.toFloat(), height.toFloat())
        glTranslatef(-x, -y, -x)
        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    fun drawImage3(
        image: ResourceLocation?,
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        r: Float,
        g: Float,
        b: Float,
        al: Float,
    ) {
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDepthMask(false)
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(r, g, b, al)
        glTranslatef(x, y, x)
        mc.textureManager.bindTexture(image)
        drawModalRectWithCustomSizedTexture(0, 0, 0f, 0f, width, height, width.toFloat(), height.toFloat())
        glTranslatef(-x, -y, -x)
        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    fun drawExhiEnchants(stack: ItemStack, x: Int, y: Int) {
        drawExhiEnchants(stack, x.toFloat(), y.toFloat())
    }

    fun drawExhiEnchants(stack: ItemStack, x: Float, y: Float) {
        var y = y
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableDepth()
        disableBlend()
        GlStateManager.resetColor()
        val darkBorder = -0x1000000
        if (stack.item is ItemArmor) {
            val prot = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack)
            val unb = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack)
            val thorn = EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack)
            if (prot > 0) {
                drawExhiOutlined(prot.toString(), drawExhiOutlined("P", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(prot), getMainColor(prot), true)
                y += 4f
            }
            if (unb > 0) {
                drawExhiOutlined(unb.toString(), drawExhiOutlined("U", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(unb), getMainColor(unb), true)
                y += 4f
            }
            if (thorn > 0) {
                drawExhiOutlined(thorn.toString(), drawExhiOutlined("T", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(thorn), getMainColor(thorn), true)
                y += 4f
            }
        }
        if (stack.item is ItemBow) {
            val power = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack)
            val punch = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack)
            val flame = EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack)
            val unb = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack)
            if (power > 0) {
                drawExhiOutlined(power.toString(), drawExhiOutlined("Pow", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(power), getMainColor(power), true)
                y += 4f
            }
            if (punch > 0) {
                drawExhiOutlined(punch.toString(), drawExhiOutlined("Pun", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(punch), getMainColor(punch), true)
                y += 4f
            }
            if (flame > 0) {
                drawExhiOutlined(flame.toString(), drawExhiOutlined("F", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(flame), getMainColor(flame), true)
                y += 4f
            }
            if (unb > 0) {
                drawExhiOutlined(unb.toString(), drawExhiOutlined("U", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(unb), getMainColor(unb), true)
                y += 4f
            }
        }
        if (stack.item is ItemSword) {
            val sharp = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack)
            val kb = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack)
            val fire = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack)
            val unb = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack)
            if (sharp > 0) {
                drawExhiOutlined(sharp.toString(), drawExhiOutlined("S", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(sharp), getMainColor(sharp), true)
                y += 4f
            }
            if (kb > 0) {
                drawExhiOutlined(kb.toString(), drawExhiOutlined("K", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(kb), getMainColor(kb), true)
                y += 4f
            }
            if (fire > 0) {
                drawExhiOutlined(fire.toString(), drawExhiOutlined("F", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(fire), getMainColor(fire), true)
                y += 4f
            }
            if (unb > 0) {
                drawExhiOutlined(unb.toString(), drawExhiOutlined("U", x, y, 0.35f, darkBorder, -1, true), y, 0.35f, getBorderColor(unb), getMainColor(unb), true)
                y += 4f
            }
        }
        GlStateManager.enableDepth()
        RenderHelper.enableGUIStandardItemLighting()
    }

    private fun drawExhiOutlined(text: String, x: Float, y: Float, borderWidth: Float, borderColor: Int, mainColor: Int, drawText: Boolean): Float {
        Fonts.fontTahomaSmall.drawString(text, x, y - borderWidth, borderColor)
        Fonts.fontTahomaSmall.drawString(text, x, y + borderWidth, borderColor)
        Fonts.fontTahomaSmall.drawString(text, x - borderWidth, y, borderColor)
        Fonts.fontTahomaSmall.drawString(text, x + borderWidth, y, borderColor)
        if (drawText) Fonts.fontTahomaSmall.drawString(text, x, y, mainColor)
        return x + Fonts.fontTahomaSmall.getWidth(text) - 2f
    }

    private fun getMainColor(level: Int): Int {
        return if (level == 4) -0x560000 else -1
    }

    private fun getBorderColor(level: Int): Int {
        if (level == 2) return 0x7055FF55
        if (level == 3) return 0x7000AAAA
        if (level == 4) return 0x70AA0000
        return if (level >= 5) 0x70FFAA00 else 0x70FFFFFF
    }

    fun glColor(red: Int, green: Int, blue: Int, alpha: Int) {
        GlStateManager.color(red / 255f, green / 255f, blue / 255f, alpha / 255f)
    }

    fun glColor(color: Color) {
        val red = color.red / 255f
        val green = color.green / 255f
        val blue = color.blue / 255f
        val alpha = color.alpha / 255f
        GlStateManager.color(red, green, blue, alpha)
    }

    fun glColor(color: Color, alpha: Float) {
        val red = color.red / 255f
        val green = color.green / 255f
        val blue = color.blue / 255f
        GlStateManager.color(red, green, blue, alpha / 255f)
    }

    fun glColor(color: Int) {
        val alpha = (color shr 24 and 0xFF) / 255f
        val red = (color shr 16 and 0xFF) / 255f
        val green = (color shr 8 and 0xFF) / 255f
        val blue = (color and 0xFF) / 255f
        GlStateManager.color(red, green, blue, alpha)
    }

    fun draw2D(entity: EntityLivingBase, posX: Double, posY: Double, posZ: Double, color: Int, backgroundColor: Int) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(posX, posY, posZ)
        GlStateManager.rotate(-mc.renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.scale(-0.1, -0.1, 0.1)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.depthMask(true)
        glColor(color)
        glCallList(DISPLAY_LISTS_2D[0])
        glColor(backgroundColor)
        glCallList(DISPLAY_LISTS_2D[1])
        GlStateManager.translate(0.0, 21 + -(entity.entityBoundingBox.maxY - entity.entityBoundingBox.minY) * 12, 0.0)
        glColor(color)
        glCallList(DISPLAY_LISTS_2D[2])
        glColor(backgroundColor)
        glCallList(DISPLAY_LISTS_2D[3])

        // Stop render
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        GlStateManager.popMatrix()
    }

    fun draw2D(blockPos: BlockPos, color: Int, backgroundColor: Int) {
        val renderManager = mc.renderManager
        val posX = blockPos.x + 0.5 - renderManager.renderPosX
        val posY = blockPos.y - renderManager.renderPosY
        val posZ = blockPos.z + 0.5 - renderManager.renderPosZ
        GlStateManager.pushMatrix()
        GlStateManager.translate(posX, posY, posZ)
        GlStateManager.rotate(-mc.renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.scale(-0.1, -0.1, 0.1)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.depthMask(true)
        glColor(color)
        glCallList(DISPLAY_LISTS_2D[0])
        glColor(backgroundColor)
        glCallList(DISPLAY_LISTS_2D[1])
        GlStateManager.translate(0f, 9f, 0f)
        glColor(color)
        glCallList(DISPLAY_LISTS_2D[2])
        glColor(backgroundColor)
        glCallList(DISPLAY_LISTS_2D[3])

        // Stop render
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        GlStateManager.popMatrix()
    }

    fun renderNameTag(string: String?, x: Double, y: Double, z: Double) {
        val renderManager = mc.renderManager
        glPushMatrix()
        glTranslated(x - renderManager.renderPosX, y - renderManager.renderPosY, z - renderManager.renderPosZ)
        glNormal3f(0f, 1f, 0f)
        glRotatef(-mc.renderManager.playerViewY, 0f, 1f, 0f)
        glRotatef(mc.renderManager.playerViewX, 1f, 0f, 0f)
        glScalef(-0.05f, -0.05f, 0.05f)
        setGlCap(GL_LIGHTING, false)
        setGlCap(GL_DEPTH_TEST, false)
        setGlCap(GL_BLEND, true)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        val width = Fonts.font35.getStringWidth(string!!) / 2
        Gui.drawRect(-width - 1, -1, width + 1, Fonts.font35.FONT_HEIGHT, Int.MIN_VALUE)
        Fonts.font35.drawString(string, -width.toFloat(), 1.5f, Color.WHITE.rgb, true)
        resetCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }

    fun drawLine(x: Float, y: Float, x1: Float, y1: Float, width: Float) {
        glDisable(GL_TEXTURE_2D)
        glLineWidth(width)
        glBegin(GL_LINES)
        glVertex2f(x, y)
        glVertex2f(x1, y1)
        glEnd()
        glEnable(GL_TEXTURE_2D)
    }

    fun drawLimitedCircle(lx: Float, ly: Float, x2: Float, y2: Float, xx: Int, yy: Int, radius: Float, color: Color) {
        val sections = 50
        val dAngle = 2 * Math.PI / sections
        var x: Float
        var y: Float
        glPushAttrib(GL_ENABLE_BIT)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glBegin(GL_TRIANGLE_FAN)
        for (i in 0 until sections) {
            x = (radius * sin(i * dAngle)).toFloat()
            y = (radius * cos(i * dAngle)).toFloat()
            glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
            glVertex2f(
                min(x2.toDouble(), max((xx + x).toDouble(), lx.toDouble())).toFloat(),
                min(y2.toDouble(), max((yy + y).toDouble(), ly.toDouble())).toFloat()
            )
        }
        GlStateManager.color(0f, 0f, 0f)
        glEnd()
        glPopAttrib()
    }

    fun drawLine(x: Double, y: Double, x1: Double, y1: Double, width: Float) {
        glDisable(GL_TEXTURE_2D)
        glLineWidth(width)
        glBegin(GL_LINES)
        glVertex2d(x, y)
        glVertex2d(x1, y1)
        glEnd()
        glEnable(GL_TEXTURE_2D)
    }

    fun makeScissorBox(x: Float, y: Float, x2: Float, y2: Float) {
        val scaledResolution = ScaledResolution(mc)
        val factor = scaledResolution.scaleFactor
        glScissor(
            (x * factor).toInt(),
            ((scaledResolution.scaledHeight - y2) * factor).toInt(),
            ((x2 - x) * factor).toInt(),
            ((y2 - y) * factor).toInt()
        )
    }
    fun otherDrawOutlinedBoundingBox(yaw: Float, x: Double, y: Double, z: Double, width: Double, height: Double) {
        var width = width * 1.5
        var yaw = (MathHelper.wrapAngleTo180_float(yaw) + 45.0).toFloat()

        var yaw1: Float
        var yaw2: Float
        var yaw3: Float
        var yaw4: Float

        if (yaw < 0.0) {
            yaw1 = 360.0F - abs(yaw)
        } else {
            yaw1 = yaw.toFloat()
        }
        yaw1 *= -1.0F
        yaw1 = (yaw1 * 0.017453292519943295).toFloat()
        yaw += 90.0F

        if (yaw < 0.0) {
            yaw2 = 0.0F
            yaw2 += 360.0F - Math.abs(yaw)
        } else {
            yaw2 = yaw.toFloat()
        }
        yaw2 *= -1.0F
        yaw2 = (yaw2 * 0.017453292519943295).toFloat()

        yaw += 90.0F

        if (yaw < 0.0) {
            yaw3 = 0.0F
            yaw3 += 360.0F - Math.abs(yaw)
        } else {
            yaw3 = yaw.toFloat()
        }

        yaw3 *= -1.0F
        yaw3 = (yaw3 * 0.017453292519943295).toFloat()

        yaw += 90.0F

        if (yaw < 0.0) {
            yaw4 = 0.0F
            yaw4 += 360.0F - Math.abs(yaw)
        } else {
            yaw4 = yaw.toFloat()
        }
        yaw4 *= -1.0F
        yaw4 = (yaw4 * 0.017453292519943295).toFloat()

        val x1 = (sin(yaw1) * width + x).toFloat()
        val z1 = (cos(yaw1) * width + z).toFloat()
        val x2 = (sin(yaw2) * width + x).toFloat()
        val z2 = (cos(yaw2) * width + z).toFloat()
        val x3 = (sin(yaw3) * width + x).toFloat()
        val z3 = (cos(yaw3) * width + z).toFloat()
        val x4 = (sin(yaw4) * width + x).toFloat()
        val z4 = (cos(yaw4) * width + z).toFloat()
        val y2 = (y + height).toFloat()

        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        worldrenderer.pos(x1.toDouble(), y, z1.toDouble()).endVertex()
        worldrenderer.pos(x1.toDouble(), y2.toDouble(), z1.toDouble()).endVertex()
        worldrenderer.pos(x2.toDouble(), y2.toDouble(), z2.toDouble()).endVertex()
        worldrenderer.pos(x2.toDouble(), y, z2.toDouble()).endVertex()
        worldrenderer.pos(x1.toDouble(), y, z1.toDouble()).endVertex()
        worldrenderer.pos(x4.toDouble(), y, z4.toDouble()).endVertex()
        worldrenderer.pos(x3.toDouble(), y, z3.toDouble()).endVertex()
        worldrenderer.pos(x3.toDouble(), y2.toDouble(), z3.toDouble()).endVertex()
        worldrenderer.pos(x4.toDouble(), y2.toDouble(), z4.toDouble()).endVertex()
        worldrenderer.pos(x4.toDouble(), y, z4.toDouble()).endVertex()
        worldrenderer.pos(x4.toDouble(), y2.toDouble(), z4.toDouble()).endVertex()
        worldrenderer.pos(x3.toDouble(), y2.toDouble(), z3.toDouble()).endVertex()
        worldrenderer.pos(x2.toDouble(), y2.toDouble(), z2.toDouble()).endVertex()
        worldrenderer.pos(x2.toDouble(), y, z2.toDouble()).endVertex()
        worldrenderer.pos(x3.toDouble(), y, z3.toDouble()).endVertex()
        worldrenderer.pos(x4.toDouble(), y, z4.toDouble()).endVertex()
        worldrenderer.pos(x4.toDouble(), y2.toDouble(), z4.toDouble()).endVertex()
        worldrenderer.pos(x1.toDouble(), y2.toDouble(), z1.toDouble()).endVertex()
        worldrenderer.pos(x1.toDouble(), y, z1.toDouble()).endVertex()
        tessellator.draw()
    }
    fun otherDrawBoundingBox(yaw: Float, x: Double, y: Double, z: Double, width: Double, height: Double) {
        var width = width * 1.5
        var yaw = MathHelper.wrapAngleTo180_float(yaw) + 45.0f
        var yaw1: Float
        var yaw2: Float
        var yaw3: Float
        var yaw4: Float

        if (yaw < 0.0f) {
            yaw1 = 0.0f
            yaw1 += 360.0f - abs(yaw)
        } else {
            yaw1 = yaw
        }

        yaw1 *= -1.0f
        yaw1 = (yaw1 * (PI / 180.0)).toFloat()

        yaw += 90.0f

        if (yaw < 0.0f) {
            yaw2 = 0.0f
            yaw2 += 360.0f - abs(yaw)
        } else {
            yaw2 = yaw
        }

        yaw2 *= -1.0f
        yaw2 = (yaw2 * (PI / 180.0)).toFloat()

        yaw += 90.0f

        if (yaw < 0.0f) {
            yaw3 = 0.0f
            yaw3 += 360.0f - abs(yaw)
        } else {
            yaw3 = yaw
        }

        yaw3 *= -1.0f
        yaw3 = (yaw3 * (PI / 180.0)).toFloat()

        yaw += 90.0f

        if (yaw < 0.0f) {
            yaw4 = 0.0f
            yaw4 += 360.0f - abs(yaw)
        } else {
            yaw4 = yaw
        }

        yaw4 *= -1.0f
        yaw4 = (yaw4 * (PI / 180.0)).toFloat()

        val x1 = (sin(yaw1) * width + x).toFloat()
        val z1 = (cos(yaw1) * width + z).toFloat()
        val x2 = (sin(yaw2) * width + x).toFloat()
        val z2 = (cos(yaw2) * width + z).toFloat()
        val x3 = (sin(yaw3) * width + x).toFloat()
        val z3 = (cos(yaw3) * width + z).toFloat()
        val x4 = (sin(yaw4) * width + x).toFloat()
        val z4 = (cos(yaw4) * width + z).toFloat()

        val y1 = y.toFloat()
        val y2 = (y + height).toFloat()

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(x1.toDouble(), y1.toDouble(), z1.toDouble()).endVertex()
        worldRenderer.pos(x1.toDouble(), y2.toDouble(), z1.toDouble()).endVertex()
        worldRenderer.pos(x2.toDouble(), y2.toDouble(), z2.toDouble()).endVertex()
        worldRenderer.pos(x2.toDouble(), y1.toDouble(), z2.toDouble()).endVertex()
        worldRenderer.pos(x2.toDouble(), y1.toDouble(), z2.toDouble()).endVertex()
        worldRenderer.pos(x2.toDouble(), y2.toDouble(), z2.toDouble()).endVertex()
        worldRenderer.pos(x3.toDouble(), y2.toDouble(), z3.toDouble()).endVertex()
        worldRenderer.pos(x3.toDouble(), y1.toDouble(), z3.toDouble()).endVertex()
        worldRenderer.pos(x3.toDouble(), y1.toDouble(), z3.toDouble()).endVertex()
        worldRenderer.pos(x3.toDouble(), y2.toDouble(), z3.toDouble()).endVertex()
        worldRenderer.pos(x4.toDouble(), y2.toDouble(), z4.toDouble()).endVertex()
        worldRenderer.pos(x4.toDouble(), y1.toDouble(), z4.toDouble()).endVertex()
        worldRenderer.pos(x4.toDouble(), y1.toDouble(), z4.toDouble()).endVertex()
        worldRenderer.pos(x4.toDouble(), y2.toDouble(), z4.toDouble()).endVertex()
        worldRenderer.pos(x1.toDouble(), y2.toDouble(), z1.toDouble()).endVertex()
        worldRenderer.pos(x1.toDouble(), y1.toDouble(), z1.toDouble()).endVertex()
        worldRenderer.pos(x1.toDouble(), y1.toDouble(), z1.toDouble()).endVertex()
        worldRenderer.pos(x2.toDouble(), y1.toDouble(), z2.toDouble()).endVertex()
        worldRenderer.pos(x3.toDouble(), y1.toDouble(), z3.toDouble()).endVertex()
        worldRenderer.pos(x4.toDouble(), y1.toDouble(), z4.toDouble()).endVertex()
        worldRenderer.pos(x1.toDouble(), y2.toDouble(), z1.toDouble()).endVertex()
        worldRenderer.pos(x2.toDouble(), y2.toDouble(), z2.toDouble()).endVertex()
        worldRenderer.pos(x3.toDouble(), y2.toDouble(), z3.toDouble()).endVertex()
        worldRenderer.pos(x4.toDouble(), y2.toDouble(), z4.toDouble()).endVertex()
        tessellator.draw()
    }

    fun drawRoundedGradientOutlineCorner(
        x: Float,
        y: Float,
        x1: Float,
        y1: Float,
        width: Float,
        radius: Float,
        color: Int,
        color2: Int,
        color3: Int,
        color4: Int,
    ) {
        var x = x
        var y = y
        var x1 = x1
        var y1 = y1
        setColour(-1)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)
        glPushAttrib(0)
        glScaled(0.5, 0.5, 0.5)
        x *= 2.0.toFloat()
        y *= 2.0.toFloat()
        x1 *= 2.0.toFloat()
        y1 *= 2.0.toFloat()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        setColour(color)
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)
        glLineWidth(width)
        glBegin(GL_LINE_LOOP)
        var i: Int
        i = 0
        while (i <= 90) {
            glVertex2d(
                x + radius + sin(i * Math.PI / 180.0) * radius * -1.0,
                y + radius + cos(i * Math.PI / 180.0) * radius * -1.0
            )
            i += 3
        }
        setColour(color2)
        i = 90
        while (i <= 180) {
            glVertex2d(
                x + radius + sin(i * Math.PI / 180.0) * radius * -1.0,
                y1 - radius + cos(i * Math.PI / 180.0) * radius * -1.0
            )
            i += 3
        }
        setColour(color3)
        i = 0
        while (i <= 90) {
            glVertex2d(x1 - radius + sin(i * Math.PI / 180.0) * radius, y1 - radius + cos(i * Math.PI / 180.0) * radius)
            i += 3
        }
        setColour(color4)
        i = 90
        while (i <= 180) {
            glVertex2d(
                x1 - radius + sin(i * Math.PI / 180.0) * radius,
                y + radius + cos(i * Math.PI / 180.0) * radius
            )
            i += 3
        }
        glEnd()
        glLineWidth(1f)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glScaled(2.0, 2.0, 2.0)
        glPopAttrib()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glShadeModel(GL_FLAT)
        setColour(-1)
    }

    fun drawRoundedGradientOutlineCorner(
        x: Float,
        y: Float,
        x1: Float,
        y1: Float,
        width: Float,
        radius: Float,
        color: Int,
        color2: Int,
    ) {
        var x = x
        var y = y
        var x1 = x1
        var y1 = y1
        setColour(-1)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)
        glPushAttrib(0)
        glScaled(0.5, 0.5, 0.5)
        x *= 2.0f
        y *= 2.0f
        x1 *= 2.0f
        y1 *= 2.0f
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        setColour(color)
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)
        glLineWidth(width)
        glBegin(GL_LINE_LOOP)
        var i: Int
        i = 0
        while (i <= 90) {
            glVertex2d(
                x + radius + sin(i * Math.PI / 180.0) * radius * -1.0,
                y + radius + cos(i * Math.PI / 180.0) * radius * -1.0
            )
            i += 3
        }
        setColour(color)
        i = 90
        while (i <= 180) {
            glVertex2d(
                x + radius + sin(i * Math.PI / 180.0) * radius * -1.0,
                y1 - radius + cos(i * Math.PI / 180.0) * radius * -1.0
            )
            i += 3
        }
        setColour(color2)
        i = 0
        while (i <= 90) {
            glVertex2d(x1 - radius + sin(i * Math.PI / 180.0) * radius, y1 - radius + cos(i * Math.PI / 180.0) * radius)
            i += 3
        }
        setColour(color2)
        i = 90
        while (i <= 180) {
            glVertex2d(
                x1 - radius + sin(i * Math.PI / 180.0) * radius,
                y + radius + cos(i * Math.PI / 180.0) * radius
            )
            i += 3
        }
        glEnd()
        glLineWidth(1f)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glScaled(2.0, 2.0, 2.0)
        glPopAttrib()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glShadeModel(GL_FLAT)
        setColour(-1)
    }

    fun drawRoundedGradientRectCorner(
        x: Float,
        y: Float,
        x1: Float,
        y1: Float,
        radius: Float,
        color: Int,
        color2: Int,
        color3: Int,
        color4: Int,
    ) {
        var x = x
        var y = y
        var x1 = x1
        var y1 = y1
        setColour(-1)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)
        glPushAttrib(0)
        glScaled(0.5, 0.5, 0.5)
        x *= 2.0.toFloat()
        y *= 2.0.toFloat()
        x1 *= 2.0.toFloat()
        y1 *= 2.0.toFloat()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        setColour(color)
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)
        glBegin(6)
        var i: Int
        i = 0
        while (i <= 90) {
            glVertex2d(
                x + radius + sin(i * Math.PI / 180.0) * radius * -1.0,
                y + radius + cos(i * Math.PI / 180.0) * radius * -1.0
            )
            i += 3
        }
        setColour(color2)
        i = 90
        while (i <= 180) {
            glVertex2d(
                x + radius + sin(i * Math.PI / 180.0) * radius * -1.0,
                y1 - radius + cos(i * Math.PI / 180.0) * radius * -1.0
            )
            i += 3
        }
        setColour(color3)
        i = 0
        while (i <= 90) {
            glVertex2d(x1 - radius + sin(i * Math.PI / 180.0) * radius, y1 - radius + cos(i * Math.PI / 180.0) * radius)
            i += 3
        }
        setColour(color4)
        i = 90
        while (i <= 180) {
            glVertex2d(
                x1 - radius + sin(i * Math.PI / 180.0) * radius,
                y + radius + cos(i * Math.PI / 180.0) * radius
            )
            i += 3
        }
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glScaled(2.0, 2.0, 2.0)
        glPopAttrib()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glShadeModel(GL_FLAT)
        setColour(-1)
    }

    fun drawAnimatedGradient(left: Double, top: Double, right: Double, bottom: Double, col1: Int, col2: Int) {
        val currentTime = System.currentTimeMillis()
        if (startTime.toInt() === 0) {
            startTime = currentTime
        }
        val elapsedTime: Long = currentTime - startTime
        val progress: Float = (elapsedTime % animationDuration) as Float / animationDuration
        val color1: Int
        val color2: Int
        if ((elapsedTime / animationDuration % 2).toInt() === 0) {
            color1 = interpolateColors(col1, col2, progress)
            color2 = interpolateColors(col2, col1, progress)
        } else {
            color1 = interpolateColors(col2, col1, progress)
            color2 = interpolateColors(col1, col2, progress)
        }
        drawGradientSideways(left, top, right, bottom, color1, color2)
        if (elapsedTime >= 2 * animationDuration) {
            startTime = currentTime
        }
    }

    fun interpolateColors(color1: Int, color2: Int, progress: Float): Int {
        val alpha = ((1.0 - progress) * (color1 ushr 24) + progress * (color2 ushr 24)).toInt()
        val red = ((1.0 - progress) * (color1 shr 16 and 0xFF) + progress * (color2 shr 16 and 0xFF)).toInt()
        val green = ((1.0 - progress) * (color1 shr 8 and 0xFF) + progress * (color2 shr 8 and 0xFF)).toInt()
        val blue = ((1.0 - progress) * (color1 and 0xFF) + progress * (color2 and 0xFF)).toInt()
        return alpha shl 24 or (red shl 16) or (green shl 8) or blue
    }

    fun drawRoundedGradientRectCorner(
        x: Float,
        y: Float,
        x1: Float,
        y1: Float,
        radius: Float,
        color: Int,
        color2: Int,
    ) {
        var x = x
        var y = y
        var x1 = x1
        var y1 = y1
        setColour(-1)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)
        glPushAttrib(0)
        glScaled(0.5, 0.5, 0.5)
        x *= 2.0.toFloat()
        y *= 2.0.toFloat()
        x1 *= 2.0.toFloat()
        y1 *= 2.0.toFloat()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        setColour(color)
        glEnable(GL_LINE_SMOOTH)
        glShadeModel(GL_SMOOTH)
        glBegin(6)
        var i: Int
        i = 0
        while (i <= 90) {
            glVertex2d(
                x + radius + sin(i * Math.PI / 180.0) * radius * -1.0,
                y + radius + cos(i * Math.PI / 180.0) * radius * -1.0
            )
            i += 3
        }
        setColour(color)
        i = 90
        while (i <= 180) {
            glVertex2d(
                x + radius + sin(i * Math.PI / 180.0) * radius * -1.0,
                y1 - radius + cos(i * Math.PI / 180.0) * radius * -1.0
            )
            i += 3
        }
        setColour(color2)
        i = 0
        while (i <= 90) {
            glVertex2d(x1 - radius + sin(i * Math.PI / 180.0) * radius, y1 - radius + cos(i * Math.PI / 180.0) * radius)
            i += 3
        }
        setColour(color2)
        i = 90
        while (i <= 180) {
            glVertex2d(
                x1 - radius + sin(i * Math.PI / 180.0) * radius,
                y + radius + cos(i * Math.PI / 180.0) * radius
            )
            i += 3
        }
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glScaled(2.0, 2.0, 2.0)
        glPopAttrib()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glShadeModel(GL_FLAT)
        setColour(-1)
    }
    /**
     * @param x : X pos
     * @param y : Y pos
     * @param x1 : X2 pos
     * @param y1 : Y2 pos
     * @param width : width of line;
     * @param radius : round of edges;
     * @param color : color;
     * @param color2 : color2;
     * @param color3 : color3;
     * @param color4 : color4;
     */

    /**
     * GL CAP MANAGER
     *
     * TODO: Remove gl cap manager and replace by something better
     */
    fun resetCaps() {
        glCapMap.forEach(this::setGlState)
    }

    fun enableGlCap(cap: Int) {
        setGlCap(cap, true)
    }

    fun enableGlCap(vararg caps: Int) {
        for (cap in caps) setGlCap(cap, true)
    }

    fun disableGlCap(cap: Int) {
        setGlCap(cap, true)
    }

    fun disableGlCap(vararg caps: Int) {
        for (cap in caps) setGlCap(cap, false)
    }

    fun setGlCap(cap: Int, state: Boolean) {
        glCapMap[cap] = glGetBoolean(cap)
        setGlState(cap, state)
    }

    fun setGlState(cap: Int, state: Boolean) {
        if (state) glEnable(cap) else glDisable(cap)
    }

    fun stop3D() {
        enableCull()
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDisable(GL_BLEND)
    }

    fun start3D() {
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDepthMask(false)
        disableCull()
    }

    fun renderHitbox(bb: AxisAlignedBB, type: Int) {
        glBegin(type)

        glVertex3d(bb.minX, bb.minY, bb.maxZ)
        glVertex3d(bb.maxX, bb.minY, bb.maxZ)
        glVertex3d(bb.maxX, bb.minY, bb.minZ)
        glVertex3d(bb.minX, bb.minY, bb.minZ)

        glEnd()

        glBegin(type)

        glVertex3d(bb.minX, bb.maxY, bb.maxZ)
        glVertex3d(bb.maxX, bb.maxY, bb.maxZ)
        glVertex3d(bb.maxX, bb.maxY, bb.minZ)
        glVertex3d(bb.minX, bb.maxY, bb.minZ)

        glEnd()

        glBegin(type)

        glVertex3d(bb.minX, bb.minY, bb.minZ)
        glVertex3d(bb.minX, bb.minY, bb.maxZ)
        glVertex3d(bb.minX, bb.maxY, bb.maxZ)
        glVertex3d(bb.minX, bb.maxY, bb.minZ)

        glEnd()
        glBegin(type)

        glVertex3d(bb.maxX, bb.minY, bb.minZ)
        glVertex3d(bb.maxX, bb.minY, bb.maxZ)
        glVertex3d(bb.maxX, bb.maxY, bb.maxZ)
        glVertex3d(bb.maxX, bb.maxY, bb.minZ)

        glEnd()
        glBegin(type)
        glVertex3d(bb.minX, bb.minY, bb.minZ)
        glVertex3d(bb.maxX, bb.minY, bb.minZ)
        glVertex3d(bb.maxX, bb.maxY, bb.minZ)
        glVertex3d(bb.minX, bb.maxY, bb.minZ)

        glEnd()
        glBegin(type)
        glVertex3d(bb.minX, bb.minY, bb.maxZ)
        glVertex3d(bb.maxX, bb.minY, bb.maxZ)
        glVertex3d(bb.maxX, bb.maxY, bb.maxZ)
        glVertex3d(bb.minX, bb.maxY, bb.maxZ)

        glEnd()
    }

}
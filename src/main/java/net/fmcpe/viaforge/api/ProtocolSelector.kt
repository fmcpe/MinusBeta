package net.fmcpe.viaforge.api

import com.mojang.realmsclient.gui.ChatFormatting
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import net.fmcpe.viaforge.ProtocolBase
import net.fmcpe.viaforge.api.ProtocolSelector.FinishedCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSlot
import org.lwjgl.input.Keyboard
import java.io.IOException

class ProtocolSelector @JvmOverloads constructor(
    private val parent: GuiScreen,
    private val simple: Boolean = false,
    private val finishedCallback: FinishedCallback = FinishedCallback { version: ProtocolVersion?, unused: GuiScreen? ->
        if (version != null) {
            ProtocolBase.manager!!.targetVersion = version
        }
    }
) : GuiScreen() {
    private var list: SlotList? = null

    override fun initGui() {
        super.initGui()
        buttonList.add(GuiButton(1, 5, height - 25, 60, 20, "Done"))

        list = SlotList(mc, width, height, -26 + (fontRendererObj.FONT_HEIGHT) * 3, height, fontRendererObj.FONT_HEIGHT)
    }

    override fun actionPerformed(button: GuiButton) {
        list!!.actionPerformed(button)

        if (button.id == 1) {
            mc.displayGuiScreen(parent)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent)
        }
    }

    @Throws(IOException::class)
    override fun handleMouseInput() {
        list!!.handleMouseInput()
        super.handleMouseInput()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        list!!.drawScreen(mouseX, mouseY, partialTicks)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    internal inner class SlotList(client: Minecraft?, width: Int, height: Int, top: Int, bottom: Int, slotHeight: Int) :
        GuiSlot(client, width, height, top, bottom, slotHeight) {
        override fun getSize(): Int {
            return ProtocolBase.versions.size
        }

        override fun elementClicked(index: Int, b: Boolean, i1: Int, i2: Int) {
            finishedCallback.finished(ProtocolBase.versions[index], parent)
        }

        override fun isSelected(index: Int): Boolean {
            return false
        }

        override fun drawBackground() {
            drawDefaultBackground()
        }

        override fun drawSlot(index: Int, x: Int, y: Int, slotHeight: Int, mouseX: Int, mouseY: Int) {
            val targetVersion = ProtocolBase.manager!!.targetVersion
            val version = ProtocolBase.versions[index]
            val color = if (targetVersion == version) {
                if (this@ProtocolSelector.simple) ChatFormatting.GOLD.toString() else ChatFormatting.GREEN.toString()
            } else {
                if (this@ProtocolSelector.simple) ChatFormatting.WHITE.toString() else ChatFormatting.DARK_RED.toString()
            }

            drawCenteredString(mc.fontRendererObj, (color) + version.name, width / 2, y - 1, -1)
        }
    }

    fun interface FinishedCallback {
        fun finished(version: ProtocolVersion?, parent: GuiScreen?)
    }
}
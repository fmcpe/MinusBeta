/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce

import com.viaversion.viaversion.api.connection.UserConnection
import net.fmcpe.viaforge.ProtocolBase
import net.fmcpe.viaforge.ProtocolMod
import net.fmcpe.viaforge.api.McUpdatesHandler
import net.fmcpe.viaforge.api.PacketManager
import net.fmcpe.viaforge.inventorytabs.EnchantItems
import net.fmcpe.viaforge.inventorytabs.ModItems
import net.fmcpe.viaforge.inventorytabs.StackItems
import net.fmcpe.viaforge.packets.C1APacketSwapHand
import net.fmcpe.viaforge.packets.C1BPacketTeleportConfirm
import net.fmcpe.viaforge.packets.PacketHandler
import net.minecraft.client.gui.GuiScreen
import net.minecraft.network.Packet
import net.minecraft.util.ResourceLocation
import net.minusmc.minusbounce.event.ClientShutdownEvent
import net.minusmc.minusbounce.event.EventManager
import net.minusmc.minusbounce.features.command.CommandManager
import net.minusmc.minusbounce.features.module.ModuleManager
import net.minusmc.minusbounce.features.special.*
import net.minusmc.minusbounce.file.FileManager
import net.minusmc.minusbounce.plugin.PluginAPIVersion
import net.minusmc.minusbounce.plugin.PluginManager
import net.minusmc.minusbounce.ui.client.altmanager.GuiAltManager
import net.minusmc.minusbounce.ui.client.clickgui.dropdown.DropDownClickGui
import net.minusmc.minusbounce.ui.client.hud.HUD
import net.minusmc.minusbounce.ui.client.hud.HUD.Companion.createDefault
import net.minusmc.minusbounce.ui.font.Fonts
import net.minusmc.minusbounce.utils.*
import net.minusmc.minusbounce.utils.click.ClickHandle
import net.minusmc.minusbounce.utils.misc.sound.TipSoundManager
import net.minusmc.minusbounce.utils.timing.WaitTickUtils

object MinusBounce {

    // Client information
    const val CLIENT_NAME = "MinusBounce"
    const val CLIENT_FOLDER = "MinusBounce"
    const val CLIENT_VERSION = 20231120
    const val CLIENT_CREATOR = "CCBlueX, MinusMC Team"
    val API_VERSION = PluginAPIVersion.VER_01
    const val CLIENT_CLOUD = "https://minusmc.github.io/MinusCloud/LiquidBounce"
    
    var isStarting = false

    // Managers
    lateinit var moduleManager: ModuleManager
    lateinit var commandManager: CommandManager
    lateinit var eventManager: EventManager
    lateinit var combatManager: CombatManager
    lateinit var fileManager: FileManager
    lateinit var tipSoundManager: TipSoundManager
    lateinit var pluginManager: PluginManager
    lateinit var clickGui: DropDownClickGui
    lateinit var sessionManager: SessionManager
    lateinit var viaUser: UserConnection

    // HUD & ClickGUI
    lateinit var hud: HUD

    // Menu Background
    var background: ResourceLocation? = null

    private var lastTick : Long = 0L

    val mainMenuButton = hashMapOf<String, Class<out GuiScreen>>()

    fun addMenuButton(name: String, gui: Class<out GuiScreen>) {
        mainMenuButton[name] = gui
    }

    /**
     * Execute if client will be started
     */
    fun startClient() {
        isStarting = true

        ClientUtils.logger.info("Starting $CLIENT_NAME")
        ClassUtils.initCacheClass()
        ProtocolBase.init(ProtocolMod.PLATFORM)
        lastTick = System.currentTimeMillis()

        fileManager = FileManager()
        eventManager = EventManager()
        combatManager = CombatManager()
        sessionManager = SessionManager()
        eventManager.registerListener(RotationUtils)
        eventManager.registerListener(AntiForge())
        eventManager.registerListener(BungeeCordSpoof())
        eventManager.registerListener(InventoryUtils)
        eventManager.registerListener(InventoryHelper)
        eventManager.registerListener(PacketUtils)
        eventManager.registerListener(WaitTickUtils)
        eventManager.registerListener(SessionUtils())
        eventManager.registerListener(MacroManager)
        eventManager.registerListener(BadPacketUtils)
        eventManager.registerListener(MovementUtils)
        eventManager.registerListener(SelectorDetectionComponent)
        eventManager.registerListener(combatManager)
        eventManager.registerListener(sessionManager)
        eventManager.registerListener(ClickHandle)
        eventManager.registerListener(PacketManager())
        eventManager.registerListener(McUpdatesHandler())

        ModItems()
        StackItems()
        EnchantItems()

        commandManager = CommandManager()
        Fonts.loadFonts()

        tipSoundManager = TipSoundManager()

        moduleManager = ModuleManager()
        moduleManager.registerModules()
        // plugin load modules   

        pluginManager = PluginManager()
        pluginManager.registerPlugins()
        pluginManager.initPlugins()

        pluginManager.registerModules()

        commandManager.registerCommands()
        pluginManager.registerCommands()
        // plugin load command

        fileManager.loadConfigs(fileManager.modulesConfig, fileManager.valuesConfig, fileManager.accountsConfig, fileManager.friendsConfig)

        clickGui = DropDownClickGui()
        fileManager.loadConfig(fileManager.clickGuiConfig)

        // Set HUD
        hud = createDefault()
        fileManager.loadConfig(fileManager.hudConfig)

        moduleManager.initModeListValues()

        // Load generators
        GuiAltManager.loadActiveGenerators()

        ClientUtils.logger.info("Finished loading $CLIENT_NAME in ${System.currentTimeMillis() - lastTick}ms.")

        // Set is starting status
        isStarting = false
    }

    @JvmStatic
    fun handlePacket(packet: Packet<*>): Boolean {
        when (packet) {
            is C1APacketSwapHand -> PacketHandler.handlePacketSwapHand(packet)
            is C1BPacketTeleportConfirm -> PacketHandler.handlePacketTeleportConfirm(packet)
            else -> return false
        }

        return true
    }

    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        // Call client shutdown
        eventManager.callEvent(ClientShutdownEvent())

        // Save all available configs
        fileManager.saveAllConfigs()
    }

}
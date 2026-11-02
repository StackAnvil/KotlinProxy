/*
 * This file is part of KotlinProxy - https://github.com/StackAnvil/KotlinProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viaproxy

import io.netty.channel.Channel
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.ResourceLeakDetector
import io.netty.util.concurrent.GlobalEventExecutor
import io.netty.util.internal.PlatformDependent
import net.lenni0451.lambdaevents.LambdaManager
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator
import net.lenni0451.reflect.JavaBypass
import net.raphimc.netminecraft.constants.MCPipeline
import net.raphimc.netminecraft.netty.connection.NetServer
import net.raphimc.viaproxy.cli.ConsoleHandler
import net.raphimc.viaproxy.injection.MixinManager
import net.raphimc.viaproxy.plugins.PluginManager
import net.raphimc.viaproxy.plugins.events.Client2ProxyHandlerCreationEvent
import net.raphimc.viaproxy.plugins.events.ProxyStartEvent
import net.raphimc.viaproxy.plugins.events.ProxyStopEvent
import net.raphimc.viaproxy.plugins.events.ViaProxyLoadedEvent
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyChannelInitializer
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyHandler
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import net.raphimc.viaproxy.saves.SaveManager
import net.raphimc.viaproxy.tasks.SystemRequirementsCheck
import net.raphimc.viaproxy.tasks.UpdateCheckTask
import net.raphimc.viaproxy.ui.SplashScreen
import net.raphimc.viaproxy.ui.ViaProxyWindow
import net.raphimc.viaproxy.util.AddressUtil
import net.raphimc.viaproxy.util.ClassLoaderPriorityUtil
import net.raphimc.viaproxy.util.JarUtil
import net.raphimc.viaproxy.util.logging.Logger
import java.awt.GraphicsEnvironment
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

object ViaProxy {

    @JvmField
    val VERSION: String = "\${version}"

    @JvmField
    val IMPL_VERSION: String = "git-KotlinProxy-\${version}:\${commit_hash}"

    @JvmField
    val EVENT_MANAGER: LambdaManager =
        LambdaManager.threadSafe(LambdaMetaFactoryGenerator(JavaBypass.TRUSTED_LOOKUP))

    private var cwd: File? = null
    private var pluginManager: PluginManager? = null
    private var saveManager: SaveManager? = null
    private var config: ViaProxyConfig? = null
    private lateinit var clientChannels: ChannelGroup

    private var currentProxyServer: NetServer? = null
    private var viaProxyWindow: ViaProxyWindow? = null
    private var foregroundWindow: JFrame? = null

    @JvmStatic
    @Throws(Throwable::class)
    fun main(args: Array<String>) {
        MixinManager.initialize()
        injectedMain("ClassLoader", args)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun injectedMain(injectionMethod: String, args: Array<String>) {
        val useUI = args.isEmpty() && !GraphicsEnvironment.isHeadless()
        val useConfig = args.size == 2 && args[0] == "config"
        val useCLI = args.isNotEmpty() && args[0] == "cli"

        val potentialCwds = ArrayList<File>()
        System.getenv("VP_RUN_DIR")?.let { potentialCwds.add(File(it)) }
        potentialCwds.add(File(System.getProperty("user.dir")))
        potentialCwds.add(File("."))
        JarUtil.getJarFile().map(File::getParentFile).ifPresent { potentialCwds.add(it) }

        val failedCwds = ArrayList<File>()
        for (potentialCwd in potentialCwds) {
            if (potentialCwd.isDirectory && Files.isWritable(potentialCwd.toPath())) {
                cwd = potentialCwd
                break
            }
            failedCwds.add(potentialCwd)
        }
        if (cwd == null) {
            for (potentialCwd in potentialCwds) {
                if (potentialCwd.isDirectory) {
                    try {
                        val testFile = File(potentialCwd, "viaproxy_writable_test.txt").toPath()
                        Files.deleteIfExists(testFile)
                        Files.writeString(testFile, "This is just a test. This file can be deleted.")
                        Files.deleteIfExists(testFile)
                        cwd = potentialCwd
                        break
                    } catch (ignored: Exception) {
                    }
                }
            }
        }

        val workingDirectory = cwd
        if (workingDirectory != null) {
            System.setProperty("user.dir", workingDirectory.absolutePath)
        } else if (useUI) {
            JOptionPane.showMessageDialog(
                null,
                "Could not find a suitable directory to use as working directory. Make sure that the current folder is writeable.",
                "KotlinProxy",
                JOptionPane.ERROR_MESSAGE
            )
            System.exit(1)
        } else {
            System.err.println("Could not find a suitable directory to use as working directory. Make sure that the current folder is writeable.")
            System.err.println("Attempted to use the following directories:")
            for (failedCwd in failedCwds) {
                System.err.println("\t- " + failedCwd.absolutePath)
            }
            System.exit(1)
        }

        Logger.setup()
        if (!useUI && !useConfig && !useCLI) {
            val fileName = JarUtil.getJarFile().map(File::getName).orElse("KotlinProxy.jar")
            Logger.LOGGER.info("Usage: java -jar $fileName | Starts KotlinProxy in graphical mode if available")
            Logger.LOGGER.info("Usage: java -jar $fileName config <config file> | Starts KotlinProxy with the specified config file")
            Logger.LOGGER.info("Usage: java -jar $fileName cli --help | Starts KotlinProxy in CLI mode")
            System.exit(1)
        }

        Logger.LOGGER.info(
            "Initializing KotlinProxy {} v{} ({}) (Injected using {})...",
            if (useUI) "GUI" else "CLI",
            VERSION,
            IMPL_VERSION,
            injectionMethod
        )
        Logger.LOGGER.info(
            "Using java version: " + System.getProperty("java.vm.name") + " " +
                System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ") on " +
                System.getProperty("os.name")
        )
        Logger.LOGGER.info("Available memory (bytes): " + Runtime.getRuntime().maxMemory())
        Logger.LOGGER.info("Working directory: " + workingDirectory!!.absolutePath)
        if (failedCwds.isNotEmpty()) {
            Logger.LOGGER.warn("Failed to use the following directories as working directory:")
            for (failedCwd in failedCwds) {
                Logger.LOGGER.warn("\t- " + failedCwd.absolutePath)
            }
        }
        if (System.getProperty("ignoreSystemRequirements") == null) {
            SystemRequirementsCheck.run(useUI)
        }

        var splashScreen: SplashScreen? = null
        val progressConsumer: (String) -> Unit
        if (useUI) {
            val progressStep = 1F / 7F
            splashScreen = SplashScreen()
            foregroundWindow = splashScreen
            progressConsumer = { text ->
                val screen = foregroundWindow as SplashScreen
                screen.progress = screen.progress + progressStep
                screen.setText(text)
            }
            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                ViaProxyWindow.showException(throwable)
                System.exit(1)
            }
        } else {
            splashScreen = null
            progressConsumer = { }
        }
        progressConsumer("Initializing KotlinProxy")

        ConsoleHandler.hookConsole()
        loadNetty()
        ClassLoaderPriorityUtil.loadOverridingJars()

        progressConsumer("Loading Plugins")
        pluginManager = PluginManager()
        progressConsumer("Loading Protocol Translators")
        ProtocolTranslator.init()
        progressConsumer("Loading Saves")
        saveManager = SaveManager()
        progressConsumer("Loading Config")

        val viaProxyConfigFile = if (useConfig) {
            val absoluteConfigFile = File(args[1])
            if (absoluteConfigFile.isAbsolute) absoluteConfigFile else File(cwd, args[1])
        } else {
            File(cwd, "viaproxy.yml")
        }
        val firstStart = !viaProxyConfigFile.exists()
        config = ViaProxyConfig.create(viaProxyConfigFile)

        if (useUI) {
            progressConsumer("Loading GUI")
            SwingUtilities.invokeAndWait {
                try {
                    foregroundWindow = ViaProxyWindow().also { viaProxyWindow = it }
                    progressConsumer("Done")
                    splashScreen?.dispose()
                } catch (throwable: Throwable) {
                    Logger.LOGGER.fatal("Failed to initialize UI", throwable)
                    System.exit(1)
                }
            }
            if (System.getProperty("skipUpdateCheck") == null) {
                CompletableFuture.runAsync(UpdateCheckTask(true))
            }
            EVENT_MANAGER.call(ViaProxyLoadedEvent())
            Logger.LOGGER.info("KotlinProxy started successfully!")
        } else {
            if (useCLI) {
                val cliArgs = args.copyOfRange(1, args.size)
                try {
                    config!!.loadFromArguments(cliArgs)
                } catch (throwable: Throwable) {
                    throw RuntimeException("Failed to load CLI arguments", throwable)
                }
            } else if (firstStart) {
                Logger.LOGGER.info("This is the first start of KotlinProxy. Please configure the settings in the " + viaProxyConfigFile.name + " file and restart KotlinProxy.")
                System.exit(0)
            }

            if (System.getProperty("skipUpdateCheck") == null) {
                CompletableFuture.runAsync(UpdateCheckTask(false))
            }
            EVENT_MANAGER.call(ViaProxyLoadedEvent())
            Logger.LOGGER.info("KotlinProxy started successfully!")
            startProxy()

            Thread.sleep(Long.MAX_VALUE)
        }
    }

    @JvmStatic
    fun startProxy() {
        check(this.currentProxyServer == null) { "Proxy is already running" }
        try {
            Logger.LOGGER.info("Starting proxy server")
            this.currentProxyServer = NetServer(
                Client2ProxyChannelInitializer {
                    EVENT_MANAGER.call(Client2ProxyHandlerCreationEvent(Client2ProxyHandler(), false)).handler
                }
            )
            EVENT_MANAGER.call(ProxyStartEvent())
            Logger.LOGGER.info("Binding proxy server to " + AddressUtil.toString(config!!.getBindAddress()))
            this.currentProxyServer!!.bind(config!!.getBindAddress(), false)
        } catch (throwable: Throwable) {
            this.currentProxyServer = null
            throw throwable
        }
    }

    @JvmStatic
    fun stopProxy() {
        val server = this.currentProxyServer ?: return
        Logger.LOGGER.info("Stopping proxy server")
        EVENT_MANAGER.call(ProxyStopEvent())

        server.channel.close()
        this.currentProxyServer = null

        for (channel in clientChannels) {
            try {
                ProxyConnection.fromChannel(channel)!!.kickClient("§cKotlinProxy has been stopped")
            } catch (ignored: Throwable) {
            }
        }
    }

    private fun loadNetty() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED)
        if (System.getProperty("io.netty.allocator.type") == null) {
            System.setProperty("io.netty.allocator.type", if (PlatformDependent.isAndroid()) "unpooled" else "pooled")
        }
        MCPipeline.useOptimizedPipeline()
        clientChannels = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
    }

    @JvmStatic
    fun getCwd(): File = cwd!!

    @JvmStatic
    fun getPluginManager(): PluginManager = pluginManager!!

    @JvmStatic
    fun getSaveManager(): SaveManager = saveManager!!

    @JvmStatic
    fun getConfig(): ViaProxyConfig = config!!

    @JvmStatic
    fun getConnectedClients(): ChannelGroup = clientChannels

    @JvmStatic
    fun getCurrentProxyServer(): NetServer? = currentProxyServer

    @JvmStatic
    fun getViaProxyWindow(): ViaProxyWindow? = viaProxyWindow

    @JvmStatic
    fun getForegroundWindow(): JFrame? = foregroundWindow

}

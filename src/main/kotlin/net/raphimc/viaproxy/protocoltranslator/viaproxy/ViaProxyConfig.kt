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
package net.raphimc.viaproxy.protocoltranslator.viaproxy

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import joptsimple.OptionException
import joptsimple.OptionParser
import joptsimple.OptionSpec
import net.lenni0451.optconfig.ConfigContext
import net.lenni0451.optconfig.ConfigLoader
import net.lenni0451.optconfig.annotations.Description
import net.lenni0451.optconfig.annotations.NotReloadable
import net.lenni0451.optconfig.annotations.OptConfig
import net.lenni0451.optconfig.annotations.Option
import net.lenni0451.optconfig.annotations.TypeSerializer
import net.lenni0451.optconfig.annotations.Validator
import net.lenni0451.optconfig.index.ClassIndexer
import net.lenni0451.optconfig.index.ConfigType
import net.lenni0451.optconfig.index.types.ConfigOption
import net.lenni0451.optconfig.index.types.SectionIndex
import net.lenni0451.optconfig.provider.ConfigProvider
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.cli.BetterHelpFormatter
import net.raphimc.viaproxy.cli.HelpRequestedException
import net.raphimc.viaproxy.plugins.events.PostOptionsParseEvent
import net.raphimc.viaproxy.plugins.events.PreOptionsParseEvent
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator
import net.raphimc.viaproxy.saves.impl.accounts.Account
import net.raphimc.viaproxy.util.AddressUtil
import net.raphimc.viaproxy.util.Proxy
import net.raphimc.viaproxy.util.config.AccountTypeSerializer
import net.raphimc.viaproxy.util.config.ProtocolVersionTypeSerializer
import net.raphimc.viaproxy.util.config.ProxyTypeSerializer
import net.raphimc.viaproxy.util.config.SocketAddressTypeSerializer
import net.raphimc.viaproxy.util.config.TargetAddressTypeSerializer
import net.raphimc.viaproxy.util.logging.Logger
import java.io.File
import java.net.SocketAddress
import java.util.HashMap
import java.util.Stack

@OptConfig(header = ["ViaProxy configuration file"], version = 1)
class ViaProxyConfig {

    private var configContext: ConfigContext<ViaProxyConfig>? = null

    @field:NotReloadable
    @field:Option("bind-address")
    @field:Description("The address ViaProxy should listen for connections.")
    @field:TypeSerializer(SocketAddressTypeSerializer::class)
    private var bindAddress: SocketAddress = AddressUtil.parse("0.0.0.0:25568", null)

    @field:Option(value = "target-address", dependencies = ["target-version"])
    @field:Description("The address of the server ViaProxy should connect to.")
    @field:TypeSerializer(TargetAddressTypeSerializer::class)
    private var targetAddress: SocketAddress = AddressUtil.parse("127.0.0.1:25565", null)

    @field:Option("target-version")
    @field:Description("The version ViaProxy should translate to. (See ViaProxy GUI for a list of versions)")
    @field:TypeSerializer(ProtocolVersionTypeSerializer::class)
    private var targetVersion: ProtocolVersion = ProtocolTranslator.AUTO_DETECT_PROTOCOL

    @field:Option("connect-timeout")
    @field:Description("The connect timeout for backend server connections in milliseconds.")
    private var connectTimeout: Int = 8000

    @field:Option("proxy-online-mode")
    @field:Description(
        "Proxy Online Mode allows you to see skins on online mode servers and use the signed chat features.",
        "Enabling Proxy Online Mode requires your client to have a valid minecraft account."
    )
    private var proxyOnlineMode: Boolean = false

    @field:Option("auth-method")
    @field:Description(
        "The authentication method to use for joining the target server.",
        "none: No authentication (Offline mode)",
        "account: Use an account for joining the target server. (Has to be configured in ViaProxy GUI)"
    )
    private var authMethod: AuthMethod = AuthMethod.NONE

    @field:Option(value = "minecraft-account-index", dependencies = ["auth-method"])
    @field:Description("The GUI account list index (0 indexed) of the account if the auth method is set to account.")
    @field:TypeSerializer(AccountTypeSerializer::class)
    private var account: Account? = null

    @field:Option("betacraft-auth")
    @field:Description(
        "Use BetaCraft authentication for classic servers.",
        "Enabling BetaCraft Auth allows you to join classic servers which have online mode enabled."
    )
    private var betacraftAuth: Boolean = false

    @field:Option("backend-proxy-url")
    @field:Description(
        "URL of a SOCKS(4/5)/HTTP(S) proxy which will be used for backend server connections. Leave empty to connect directly.",
        "Supported formats:",
        "- type://address:port",
        "- type://username:password@address:port"
    )
    @field:TypeSerializer(ProxyTypeSerializer::class)
    private var backendProxy: Proxy? = null

    @field:Option("backend-haproxy")
    @field:Description("Send HAProxy protocol messages to the target server.")
    private var backendHaProxy: Boolean = false

    @field:Option("frontend-haproxy")
    @field:Description("Read HAProxy protocol messages from client connections.")
    private var frontendHaProxy: Boolean = false

    @field:Option("chat-signing")
    @field:Description("Enables sending signed chat messages on >= 1.19 servers.")
    private var chatSigning: Boolean = true

    @field:Option("compression-threshold")
    @field:Description("The threshold for packet compression. Packets larger than this size will be compressed. (-1 to disable)")
    private var compressionThreshold: Int = 256

    @field:Option("allow-beta-pinging")
    @field:Description("Enabling this will allow you to ping <= b1.7.3 servers. This may cause issues with servers that block too frequent connections.")
    private var allowBetaPinging: Boolean = false

    @field:Option("ignore-protocol-translation-errors")
    @field:Description(
        "Enabling this will prevent getting disconnected from the server when a packet translation error occurs and instead only print the error in the console.",
        "This may cause issues depending on the type of packet which failed to translate."
    )
    private var ignoreProtocolTranslationErrors: Boolean = false

    @field:Option("suppress-client-protocol-errors")
    @field:Description(
        "Enabling this will suppress client protocol errors to prevent lag when ViaProxy is getting spammed with invalid packets.",
        "This may cause issues with debugging client connection issues because no error messages will be printed."
    )
    private var suppressClientProtocolErrors: Boolean = false

    @field:Option("allow-legacy-client-passthrough")
    @field:Description("Allow <= 1.6.4 clients to connect through ViaProxy to the target server. (No protocol translation or packet handling)")
    private var allowLegacyClientPassthrough: Boolean = false

    @field:Option("bungeecord-player-info-passthrough")
    @field:Description(
        "Allow additional information like player ip, player uuid to be passed through to the backend server.",
        "This is typically used by proxies like BungeeCord and requires support from the backend server as well."
    )
    private var bungeecordPlayerInfoPassthrough: Boolean = false

    @field:Option("rewrite-handshake-packet")
    @field:Description(
        "Enabling this will rewrite the address in the handshake packet to a value the vanilla client would have sent when connecting directly to the target server.",
        "This should be left enabled unless you are a server owner and you need the original address on the backend server."
    )
    private var rewriteHandshakePacket: Boolean = true

    @field:Option("rewrite-transfer-packets")
    @field:Description(
        "Enabling this will rewrite transfer packets to point back to ViaProxy. This allows ViaProxy to perform protocol translation when forwarding the player to the actual server from the transfer packet.",
        "This should be left enabled unless you are a server owner and the servers you are transferring to perform their own protocol translation."
    )
    private var rewriteTransferPackets: Boolean = true

    @field:Option("custom-motd")
    @field:Description("Custom MOTD to send when clients ping the proxy. Leave empty to use the target server's MOTD.")
    private var customMotd: String = ""

    @field:Option("custom-favicon-path")
    @field:Description("Relative file path to a custom favicon to send when clients ping the proxy. Leave empty to use the target server's favicon.")
    private var customFaviconPath: String = ""

    @field:Option("resource-pack-url")
    @field:Description(
        "URL of a resource pack which clients can optionally download when connecting to the server. Leave empty to disable.",
        "Example: http://example.com/resourcepack.zip"
    )
    private var resourcePackUrl: String = ""

    @field:Option("wildcard-domain-handling")
    @field:Description(
        "Allows clients to specify a target server and version using wildcard domains.",
        "none: No wildcard domain handling.",
        "public: Public wildcard domain handling. Intended for usage by external clients. (Example: address_port_version.viaproxy.127.0.0.1.nip.io)",
        "internal: Internal wildcard domain handling. Intended for local usage by custom clients. (Example: original-handshake-address\\7address:port\\7version\\7classic-mppass)"
    )
    private var wildcardDomainHandling: WildcardDomainHandling = WildcardDomainHandling.NONE

    @field:Option("simple-voice-chat-support")
    @field:Description("Enables handling and rewriting of Simple Voice Chat mod packets.")
    private var simpleVoiceChatSupport: Boolean = false

    @field:Option("fix-fabric-particle-api")
    @field:Description(
        "Fixes an issue where the Fabric Particle API causes disconnects when both the client and server have the mod installed and both are 1.21.5+.",
        "See https://github.com/ViaVersion/ViaFabric/issues/428"
    )
    private var fixFabricParticleApi: Boolean = true

    @field:Option("fake-accept-resource-packs")
    @field:Description(
        "Accepts resource packs from the server without showing a prompt to the client.",
        "This is required for servers that require a resource pack, but the client can't load it due to version differences."
    )
    private var fakeAcceptResourcePacks: Boolean = false

    @field:Option("skip-config-state-packet-queue")
    @field:Description(
        "Fixes potential join issues on <= 1.20.1 quilt/fabric servers.",
        "It's recommended to only enable this if you are experiencing issues with the config state packet queue (See above issue)."
    )
    private var skipConfigStatePacketQueue: Boolean = false

    @field:Option("log-ips")
    @field:Description("Disable this if you want to hide IP addresses in the console and log files.")
    private var logIps: Boolean = true

    @field:Option("log-client-status-requests")
    @field:Description("Enable this if you want to see client status requests in the console and log files.")
    private var logClientStatusRequests: Boolean = false

    @Throws(Exception::class)
    fun loadFromArguments(args: Array<String>) {
        val optionParser = OptionParser()
        val optionHelp = optionParser.accepts("help").forHelp()
        val optionListVersions = optionParser.accepts("list-versions", "Lists all supported server/target versions").forHelp()

        val optionMap = HashMap<OptionSpec<Any>, ConfigOption>()
        val stack = Stack<SectionIndex>()
        val configLoader = ConfigLoader(ViaProxyConfig::class.java)
        stack.push(ClassIndexer.indexClass(ConfigType.INSTANCED, ViaProxyConfig::class.java, configLoader.configOptions.classAccessFactory))
        while (!stack.isEmpty()) {
            val index = stack.pop()
            stack.addAll(index.subSections.values)

            for (option in index.options) {
                if ((index.subSections as Map<*, *>).containsKey(option)) continue

                var defaultValue: Any? = option.fieldAccess.getValue(this)
                if (option.typeSerializer != null) {
                    defaultValue = option.createTypeSerializer<ViaProxyConfig, Any?>(configLoader, ViaProxyConfig::class.java, this).serialize(defaultValue)
                }
                val nonNullDefaultValue = defaultValue!!
                val cliOption: OptionSpec<Any> = optionParser.accepts(option.name)
                    .withRequiredArg()
                    .ofType(nonNullDefaultValue.javaClass)
                    .defaultsTo(nonNullDefaultValue)
                optionMap[cliOption] = option
            }
        }

        try {
            ViaProxy.EVENT_MANAGER.call(PreOptionsParseEvent(optionParser))
            val options = optionParser.parse(*args)
            if (options.has(optionHelp)) {
                throw HelpRequestedException()
            } else if (options.has(optionListVersions)) {
                Logger.LOGGER.info("=== Supported Server Versions ===")
                for (version in ProtocolVersion.getProtocols()) {
                    Logger.LOGGER.info(version.name)
                }
                Logger.LOGGER.info("===================================")
                System.exit(0)
            }

            if (options.has("minecraft-account-index")) {
                this.authMethod = AuthMethod.ACCOUNT
            }
            for (entry in optionMap.entries) {
                val option = entry.value
                if (options.has(entry.key)) {
                    var value: Any? = options.valueOf(entry.key)
                    if (option.typeSerializer != null) {
                        value = option.createTypeSerializer<ViaProxyConfig, Any>(configLoader, ViaProxyConfig::class.java, this).deserialize(option.fieldAccess.type as Class<Any>, value)
                    }
                    if (option.validator != null) {
                        value = option.validator!!.invoke(this, value)
                    }
                    option.fieldAccess.setValue(this, value)
                }
            }

            ViaProxy.EVENT_MANAGER.call(PostOptionsParseEvent(options))
            return
        } catch (e: OptionException) {
            Logger.LOGGER.fatal("Error parsing CLI options: " + e.message)
        } catch (ignored: HelpRequestedException) {
        }

        optionParser.formatHelpWith(BetterHelpFormatter())
        optionParser.printHelpOn(Logger.SYSOUT)
        Logger.LOGGER.info("For a more detailed description of the options, please refer to the viaproxy.yml file.")
        System.exit(1)
    }

    fun save() {
        try {
            this.configContext!!.save()
        } catch (e: Throwable) {
            throw RuntimeException("Failed to save config", e)
        }
    }

    fun getBindAddress(): SocketAddress {
        return this.bindAddress
    }

    fun setBindAddress(bindAddress: SocketAddress) {
        this.bindAddress = bindAddress
        this.save()
    }

    fun getTargetAddress(): SocketAddress {
        return this.targetAddress
    }

    fun setTargetAddress(targetAddress: SocketAddress) {
        this.targetAddress = targetAddress
        this.save()
    }

    fun getTargetVersion(): ProtocolVersion {
        return this.targetVersion
    }

    fun setTargetVersion(targetVersion: ProtocolVersion) {
        this.targetVersion = targetVersion
        this.save()
    }

    fun getConnectTimeout(): Int {
        return this.connectTimeout
    }

    fun setConnectTimeout(connectTimeout: Int) {
        this.connectTimeout = connectTimeout
        this.save()
    }

    fun isProxyOnlineMode(): Boolean {
        return this.proxyOnlineMode
    }

    fun setProxyOnlineMode(proxyOnlineMode: Boolean) {
        this.proxyOnlineMode = proxyOnlineMode
        this.save()
    }

    fun getAuthMethod(): AuthMethod {
        return this.authMethod
    }

    fun setAuthMethod(authMethod: AuthMethod) {
        this.authMethod = authMethod
        this.save()
    }

    fun getAccount(): Account? {
        return this.account
    }

    fun setAccount(account: Account?) {
        this.account = account
        this.save()
    }

    fun useBetacraftAuth(): Boolean {
        return this.betacraftAuth
    }

    fun setBetacraftAuth(betacraftAuth: Boolean) {
        this.betacraftAuth = betacraftAuth
        this.save()
    }

    fun getBackendProxy(): Proxy? {
        return this.backendProxy
    }

    fun setBackendProxy(backendProxy: Proxy?) {
        this.backendProxy = backendProxy
        this.save()
    }

    fun useBackendHaProxy(): Boolean {
        return this.backendHaProxy
    }

    fun setBackendHaProxy(backendHaProxy: Boolean) {
        this.backendHaProxy = backendHaProxy
        this.save()
    }

    fun useFrontendHaProxy(): Boolean {
        return this.frontendHaProxy
    }

    fun setFrontendHaProxy(frontendHaProxy: Boolean) {
        this.frontendHaProxy = frontendHaProxy
        this.save()
    }

    fun shouldSignChat(): Boolean {
        return this.chatSigning
    }

    fun setChatSigning(chatSigning: Boolean) {
        this.chatSigning = chatSigning
        this.save()
    }

    fun getCompressionThreshold(): Int {
        return this.compressionThreshold
    }

    fun setCompressionThreshold(compressionThreshold: Int) {
        this.compressionThreshold = compressionThreshold
        this.save()
    }

    fun shouldAllowBetaPinging(): Boolean {
        return this.allowBetaPinging
    }

    fun setAllowBetaPinging(allowBetaPinging: Boolean) {
        this.allowBetaPinging = allowBetaPinging
        this.save()
    }

    fun shouldIgnoreProtocolTranslationErrors(): Boolean {
        return this.ignoreProtocolTranslationErrors
    }

    fun setIgnoreProtocolTranslationErrors(ignoreProtocolTranslationErrors: Boolean) {
        this.ignoreProtocolTranslationErrors = ignoreProtocolTranslationErrors
        this.save()
    }

    fun shouldSuppressClientProtocolErrors(): Boolean {
        return this.suppressClientProtocolErrors
    }

    fun setSuppressClientProtocolErrors(suppressClientProtocolErrors: Boolean) {
        this.suppressClientProtocolErrors = suppressClientProtocolErrors
        this.save()
    }

    fun shouldAllowLegacyClientPassthrough(): Boolean {
        return this.allowLegacyClientPassthrough
    }

    fun setAllowLegacyClientPassthrough(allowLegacyClientPassthrough: Boolean) {
        this.allowLegacyClientPassthrough = allowLegacyClientPassthrough
        this.save()
    }

    fun shouldPassthroughBungeecordPlayerInfo(): Boolean {
        return this.bungeecordPlayerInfoPassthrough
    }

    fun setPassthroughBungeecordPlayerInfo(bungeecordPlayerInfoPassthrough: Boolean) {
        this.bungeecordPlayerInfoPassthrough = bungeecordPlayerInfoPassthrough
        this.save()
    }

    fun shouldRewriteHandshakePacket(): Boolean {
        return this.rewriteHandshakePacket
    }

    fun setRewriteHandshakePacket(rewriteHandshakePacket: Boolean) {
        this.rewriteHandshakePacket = rewriteHandshakePacket
        this.save()
    }

    fun shouldRewriteTransferPackets(): Boolean {
        return this.rewriteTransferPackets
    }

    fun setRewriteTransferPackets(rewriteTransferPackets: Boolean) {
        this.rewriteTransferPackets = rewriteTransferPackets
        this.save()
    }

    fun getCustomMotd(): String {
        return this.customMotd
    }

    fun setCustomMotd(customMotd: String) {
        this.customMotd = customMotd
        this.save()
    }

    fun getCustomFaviconPath(): String {
        return this.customFaviconPath
    }

    fun setCustomFaviconPath(customFaviconPath: String) {
        this.customFaviconPath = customFaviconPath
        this.save()
    }

    fun getResourcePackUrl(): String {
        return this.resourcePackUrl
    }

    fun setResourcePackUrl(resourcePackUrl: String) {
        this.resourcePackUrl = resourcePackUrl
        this.save()
    }

    fun getWildcardDomainHandling(): WildcardDomainHandling {
        return this.wildcardDomainHandling
    }

    fun setWildcardDomainHandling(wildcardDomainHandling: WildcardDomainHandling) {
        this.wildcardDomainHandling = wildcardDomainHandling
        this.save()
    }

    fun shouldSupportSimpleVoiceChat(): Boolean {
        return this.simpleVoiceChatSupport
    }

    fun setSimpleVoiceChatSupport(simpleVoiceChatSupport: Boolean) {
        this.simpleVoiceChatSupport = simpleVoiceChatSupport
        this.save()
    }

    fun shouldFixFabricParticleApi(): Boolean {
        return this.fixFabricParticleApi
    }

    fun setFixFabricParticleApi(fixFabricParticleApi: Boolean) {
        this.fixFabricParticleApi = fixFabricParticleApi
        this.save()
    }

    fun shouldFakeAcceptResourcePacks(): Boolean {
        return this.fakeAcceptResourcePacks
    }

    fun setFakeAcceptResourcePacks(fakeAcceptResourcePacks: Boolean) {
        this.fakeAcceptResourcePacks = fakeAcceptResourcePacks
        this.save()
    }

    fun shouldSkipConfigStatePacketQueue(): Boolean {
        return this.skipConfigStatePacketQueue
    }

    fun setSkipConfigStatePacketQueue(skipConfigStatePacketQueue: Boolean) {
        this.skipConfigStatePacketQueue = skipConfigStatePacketQueue
        this.save()
    }

    fun shouldLogIps(): Boolean {
        return this.logIps
    }

    fun setLogIps(logIps: Boolean) {
        this.logIps = logIps
        this.save()
    }

    fun shouldLogClientStatusRequests(): Boolean {
        return this.logClientStatusRequests
    }

    fun setLogClientStatusRequests(logClientStatusRequests: Boolean) {
        this.logClientStatusRequests = logClientStatusRequests
        this.save()
    }

    @Validator("target-version")
    private fun validateTargetVersion(targetVersion: ProtocolVersion?): ProtocolVersion {
        if (targetVersion == null) {
            Logger.LOGGER.warn("Invalid target server version. Defaulting to auto detect.")
            Logger.LOGGER.warn("=== Supported Protocol Versions ===")
            for (version in ProtocolVersion.getProtocols()) {
                Logger.LOGGER.warn(version.name)
            }
            Logger.LOGGER.warn("===================================")
            return ProtocolTranslator.AUTO_DETECT_PROTOCOL
        }
        return targetVersion
    }

    enum class AuthMethod(private val guiTranslationKey: String) {

        /**
         * Use an account for joining the target server (Has to be configured in ViaProxy GUI)
         */
        ACCOUNT("tab.general.minecraft_account.option_select_account"),

        /**
         * No authentication (Offline mode)
         */
        NONE("tab.general.minecraft_account.option_no_account");

        fun getGuiTranslationKey(): String {
            return this.guiTranslationKey
        }

    }

    enum class WildcardDomainHandling {

        /**
         * No wildcard domain handling
         */
        NONE,

        /**
         * Public wildcard domain handling
         */
        PUBLIC,

        /**
         * Iternal wildcard domain handling
         */
        INTERNAL,

    }

    companion object {

        @JvmStatic
        fun create(configFile: File): ViaProxyConfig {
            val configLoader = ConfigLoader(ViaProxyConfig::class.java)
            configLoader.configOptions.setResetInvalidOptions(true).setRewriteConfig(true).setCommentSpacing(1)
            try {
                return configLoader.load(ConfigProvider.file(configFile)).configInstance
            } catch (e: Throwable) {
                throw RuntimeException("Failed to load config", e)
            }
        }

    }

}

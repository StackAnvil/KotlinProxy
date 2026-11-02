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
package net.raphimc.viaproxy.protocoltranslator.providers

import com.viaversion.viaversion.api.connection.UserConnection
import io.netty.channel.Channel
import net.raphimc.netminecraft.constants.MCPipeline
import net.raphimc.viabedrock.netty.CompressionCodec
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PacketCompressionAlgorithm
import net.raphimc.viabedrock.protocol.provider.NettyPipelineProvider
import net.raphimc.viaproxy.proxy.session.ProxyConnection
import javax.crypto.SecretKey

class ViaProxyNettyPipelineProvider : NettyPipelineProvider() {

    override fun enableCompression(user: UserConnection, preferredCompressionAlgorithm: PacketCompressionAlgorithm, threshold: Int) {
        val proxyConnection = ProxyConnection.fromUserConnection(user)!!
        val channel: Channel = proxyConnection.channel
        if (!channel.pipeline().names().contains(MCPipeline.COMPRESSION_HANDLER_NAME)) {
            channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, MCPipeline.COMPRESSION_HANDLER_NAME, CompressionCodec(preferredCompressionAlgorithm, threshold))
        } else {
            channel.pipeline().replace(MCPipeline.COMPRESSION_HANDLER_NAME, MCPipeline.COMPRESSION_HANDLER_NAME, CompressionCodec(preferredCompressionAlgorithm, threshold))
        }
    }

    override fun enableEncryption(user: UserConnection, key: SecretKey) {
        // Not needed for NetherNet
    }

}

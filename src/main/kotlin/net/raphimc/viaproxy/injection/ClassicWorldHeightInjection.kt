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
package net.raphimc.viaproxy.injection

import com.viaversion.nbt.tag.CompoundTag
import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.minecraft.chunks.Chunk
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers
import com.viaversion.viaversion.api.type.Types
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_17
import net.raphimc.vialegacy.api.LegacyProtocolVersion
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.model.ClassicLevel
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicWorldHeightProvider
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.storage.ClassicLevelStorage
import java.util.ArrayList
import java.util.BitSet

class ClassicWorldHeightInjection {

    companion object {
        @JvmStatic
        fun handleJoinGame(parentHandler: PacketHandler): PacketHandler {
            return PacketHandler { wrapper ->
                parentHandler.handle(wrapper)
                if (!wrapper.isCancelled) {
                    if (wrapper.user().getProtocolInfo().serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
                        for (dimension in wrapper.get(Types.NAMED_COMPOUND_TAG, 0).getCompoundTag("minecraft:dimension_type")!!.getListTag("value", CompoundTag::class.java)!!) {
                            changeDimensionTagHeight(wrapper.user(), dimension.getCompoundTag("element")!!)
                        }
                        changeDimensionTagHeight(wrapper.user(), wrapper.get(Types.NAMED_COMPOUND_TAG, 1))
                    }
                }
            }
        }

        @JvmStatic
        fun handleRespawn(parentHandler: PacketHandler): PacketHandler {
            return PacketHandler { wrapper ->
                parentHandler.handle(wrapper)
                if (!wrapper.isCancelled) {
                    if (wrapper.user().getProtocolInfo().serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
                        changeDimensionTagHeight(wrapper.user(), wrapper.get(Types.NAMED_COMPOUND_TAG, 0))
                    }
                }
            }
        }

        @JvmStatic
        fun handleChunkData(parentHandler: PacketHandler): PacketHandler {
            return PacketHandler { wrapper ->
                parentHandler.handle(wrapper)
                if (!wrapper.isCancelled) {
                    if (wrapper.user().getProtocolInfo().serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
                        wrapper.resetReader()
                        val chunk: Chunk = wrapper.read(ChunkType1_17(16))
                        wrapper.write(ChunkType1_17(chunk.sections.size), chunk)

                        val heightProvider = Via.getManager().providers.get(ClassicWorldHeightProvider::class.java)!!
                        if (chunk.sections.size < heightProvider.getMaxChunkSectionCount(wrapper.user()).toInt()) { // Increase available sections to match new world height
                            val newArray = arrayOfNulls<ChunkSection>(heightProvider.getMaxChunkSectionCount(wrapper.user()).toInt())
                            System.arraycopy(chunk.sections, 0, newArray, 0, chunk.sections.size)
                            chunk.sections = newArray
                        }

                        val chunkMask = BitSet()
                        for (i in chunk.sections.indices) {
                            if (chunk.sections[i] != null) chunkMask.set(i)
                        }
                        chunk.chunkMask = chunkMask

                        val newBiomeData = IntArray(chunk.sections.size * 4 * 4 * 4)
                        val biomeData = chunk.biomeData!!
                        System.arraycopy(biomeData, 0, newBiomeData, 0, biomeData.size)
                        var i = 64
                        while (i < chunk.sections.size * 4) { // copy top layer of old biome data all the way to max world height
                            System.arraycopy(biomeData, biomeData.size - 16, newBiomeData, i * 16, 16)
                            i++
                        }
                        chunk.biomeData = newBiomeData

                        chunk.heightMap = CompoundTag() // rip heightmap :(
                    }
                }
            }
        }

        @JvmStatic
        fun handleUpdateLight(parentHandler: PacketHandler): PacketHandler {
            val classicLightHandler: PacketHandler = object : PacketHandlers() {
                override fun register() {
                    map(Types.VAR_INT) // x
                    map(Types.VAR_INT) // y
                    map(Types.BOOLEAN) // trust edges
                    handler { wrapper ->
                        wrapper.read(Types.VAR_INT) // sky light mask
                        wrapper.read(Types.VAR_INT) // block light mask
                        val emptySkyLightMask = wrapper.read(Types.VAR_INT) // empty sky light mask
                        val emptyBlockLightMask = wrapper.read(Types.VAR_INT) // empty block light mask

                        val level = wrapper.user().get(ClassicLevelStorage::class.java)!!.classicLevel
                        val heightProvider = Via.getManager().providers.get(ClassicWorldHeightProvider::class.java)!!

                        var sectionYCount = level.sizeY shr 4
                        if (level.sizeY % 16 != 0) sectionYCount++
                        if (sectionYCount > heightProvider.getMaxChunkSectionCount(wrapper.user()).toInt()) {
                            sectionYCount = heightProvider.getMaxChunkSectionCount(wrapper.user()).toInt()
                        }

                        val lightArrays = ArrayList<ByteArray>()
                        while (wrapper.isReadable(Types.BYTE_ARRAY_PRIMITIVE, 0)) {
                            lightArrays.add(wrapper.read(Types.BYTE_ARRAY_PRIMITIVE))
                        }

                        var skyLightCount = 16
                        var blockLightCount = sectionYCount
                        if (lightArrays.size == 16 + 0 + 2) {
                            blockLightCount = 0
                        } else if (lightArrays.size == 16 + sectionYCount + 2) {
                        } else if (lightArrays.size == sectionYCount + sectionYCount + 2) {
                            skyLightCount = sectionYCount
                        }
                        skyLightCount += 2 // Chunk below 0 and above 255

                        val skyLightMask = BitSet()
                        val blockLightMask = BitSet()
                        skyLightMask.set(0, skyLightCount)
                        blockLightMask.set(0, blockLightCount)

                        wrapper.write(Types.LONG_ARRAY_PRIMITIVE, skyLightMask.toLongArray())
                        wrapper.write(Types.LONG_ARRAY_PRIMITIVE, blockLightMask.toLongArray())
                        wrapper.write(Types.LONG_ARRAY_PRIMITIVE, LongArray(emptySkyLightMask))
                        wrapper.write(Types.LONG_ARRAY_PRIMITIVE, LongArray(emptyBlockLightMask))

                        wrapper.write(Types.VAR_INT, skyLightCount)
                        for (i in 0 until skyLightCount) {
                            wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, lightArrays.removeAt(0))
                        }
                        wrapper.write(Types.VAR_INT, blockLightCount)
                        for (i in 0 until blockLightCount) {
                            wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, lightArrays.removeAt(0))
                        }
                    }
                }
            }

            return PacketHandler { wrapper ->
                if (wrapper.user().getProtocolInfo().serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
                    classicLightHandler.handle(wrapper)
                } else {
                    parentHandler.handle(wrapper)
                }
            }
        }

        private fun changeDimensionTagHeight(user: UserConnection, tag: CompoundTag) {
            tag.putInt("height", Via.getManager().providers.get(ClassicWorldHeightProvider::class.java)!!.getMaxChunkSectionCount(user).toInt() shl 4)
        }
    }

}

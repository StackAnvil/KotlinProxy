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
package net.raphimc.viaproxy.protocoltranslator.impl

import com.viaversion.viaversion.api.data.MappingDataLoader
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import java.util.HashMap

class ViaProxyMappingDataLoader private constructor() :
    MappingDataLoader(ViaProxyMappingDataLoader::class.java, "assets/viaproxy/data/") {

    init {
        val materialsData = this.loadData("materials-1.19.4.json")!!
        for (entry in materialsData.getAsJsonObject("materials")!!.entrySet()) {
            val materialData = entry.value.getAsJsonObject()
            MATERIALS.put(entry.key, Material(
                materialData.get("blocksMovement").asBoolean,
                materialData.get("burnable").asBoolean,
                materialData.get("liquid").asBoolean,
                materialData.get("blocksLight").asBoolean,
                materialData.get("replaceable").asBoolean,
                materialData.get("solid").asBoolean
            ))
        }
        for (blockEntry in materialsData.getAsJsonObject("blocks")!!.entrySet()) {
            val blockMaterials = HashMap<ProtocolVersion, String>()
            for (entry in blockEntry.value.getAsJsonObject().entrySet()) {
                val version = ProtocolVersion.getClosest(entry.key)
                if (version == null) {
                    throw IllegalStateException("Unknown protocol version: " + entry.key)
                }
                blockMaterials.put(version, entry.value.asString)
            }
            BLOCK_MATERIALS.put(blockEntry.key, blockMaterials)
        }
    }

    class Material(
        private val blocksMovementValue: Boolean,
        private val burnableValue: Boolean,
        private val liquidValue: Boolean,
        private val blocksLightValue: Boolean,
        private val replaceableValue: Boolean,
        private val solidValue: Boolean
    ) {

        fun blocksMovement(): Boolean {
            return this.blocksMovementValue
        }

        fun burnable(): Boolean {
            return this.burnableValue
        }

        fun liquid(): Boolean {
            return this.liquidValue
        }

        fun blocksLight(): Boolean {
            return this.blocksLightValue
        }

        fun replaceable(): Boolean {
            return this.replaceableValue
        }

        fun solid(): Boolean {
            return this.solidValue
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Material) return false
            return this.blocksMovementValue == other.blocksMovementValue &&
                this.burnableValue == other.burnableValue &&
                this.liquidValue == other.liquidValue &&
                this.blocksLightValue == other.blocksLightValue &&
                this.replaceableValue == other.replaceableValue &&
                this.solidValue == other.solidValue
        }

        override fun hashCode(): Int {
            var result = 0
            result = 31 * result + this.blocksMovementValue.hashCode()
            result = 31 * result + this.burnableValue.hashCode()
            result = 31 * result + this.liquidValue.hashCode()
            result = 31 * result + this.blocksLightValue.hashCode()
            result = 31 * result + this.replaceableValue.hashCode()
            result = 31 * result + this.solidValue.hashCode()
            return result
        }

        override fun toString(): String {
            return "Material[blocksMovement=" + this.blocksMovementValue +
                ", burnable=" + this.burnableValue +
                ", liquid=" + this.liquidValue +
                ", blocksLight=" + this.blocksLightValue +
                ", replaceable=" + this.replaceableValue +
                ", solid=" + this.solidValue + "]"
        }

    }

    companion object {
        @JvmField
        val MATERIALS: MutableMap<String, Material> = HashMap()

        @JvmField
        val BLOCK_MATERIALS: MutableMap<String, MutableMap<ProtocolVersion, String>> = HashMap()

        @JvmField
        val INSTANCE: ViaProxyMappingDataLoader = ViaProxyMappingDataLoader()
    }

}

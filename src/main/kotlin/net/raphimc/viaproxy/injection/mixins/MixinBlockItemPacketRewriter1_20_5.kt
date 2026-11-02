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
package net.raphimc.viaproxy.injection.mixins

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.minecraft.HolderSet
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey
import com.viaversion.viaversion.api.minecraft.item.Item
import com.viaversion.viaversion.api.minecraft.item.data.ToolProperties
import com.viaversion.viaversion.api.minecraft.item.data.ToolRule
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import com.viaversion.viaversion.libs.fastutil.ints.IntOpenHashSet
import com.viaversion.viaversion.libs.fastutil.ints.IntSet
import com.viaversion.viaversion.libs.gson.JsonArray
import com.viaversion.viaversion.libs.gson.JsonElement
import com.viaversion.viaversion.libs.gson.JsonObject
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPacket1_20_3
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPacket1_20_5
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.rewriter.BlockItemPacketRewriter1_20_5
import com.viaversion.viaversion.rewriter.ItemRewriter
import net.raphimc.vialegacy.api.LegacyProtocolVersion
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.plugins.events.ViaLoadingEvent
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyMappingDataLoader
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashMap

@Mixin(value = [BlockItemPacketRewriter1_20_5::class], remap = false)
abstract class MixinBlockItemPacketRewriter1_20_5 : ItemRewriter<ClientboundPacket1_20_3, ServerboundPacket1_20_5, Protocol1_20_3To1_20_5>(null, null, null, null, null) {

    @field:Unique
    private val foodItems_b1_7_3: MutableSet<String> = HashSet()

    @field:Unique
    private val armorMaxDamage_b1_8_1: MutableMap<String, Int> = HashMap()

    @field:Unique
    private val toolDataChanges: MutableMap<ProtocolVersion, Map<String, ToolProperties>> = LinkedHashMap()

    @Inject(method = ["<init>"], at = [At("RETURN")])
    fun loadItemMappings(protocol: Protocol1_20_3To1_20_5, ci: CallbackInfo) {
        this.foodItems_b1_7_3.add("minecraft:apple")
        this.foodItems_b1_7_3.add("minecraft:mushroom_stew")
        this.foodItems_b1_7_3.add("minecraft:bread")
        this.foodItems_b1_7_3.add("minecraft:porkchop")
        this.foodItems_b1_7_3.add("minecraft:cooked_porkchop")
        this.foodItems_b1_7_3.add("minecraft:golden_apple")
        this.foodItems_b1_7_3.add("minecraft:cod")
        this.foodItems_b1_7_3.add("minecraft:cooked_cod")
        this.foodItems_b1_7_3.add("minecraft:cookie")

        val armorMaxDamages = ViaProxyMappingDataLoader.INSTANCE.loadData("armor-damages-b1.8.1.json")!!
        for (entry in armorMaxDamages.entrySet()) {
            this.armorMaxDamage_b1_8_1[entry.key] = entry.value.getAsInt()
        }

        val mixin = this
        ViaProxy.EVENT_MANAGER.registerRunnable(object : Runnable {
            override fun run() {
            val itemToolComponents = ViaProxyMappingDataLoader.INSTANCE.loadData("item-tool-components.json")!!
            for (entry in itemToolComponents.entrySet()) {
                val version = ProtocolVersion.getClosest(entry.key) ?: throw IllegalStateException("Unknown protocol version: " + entry.key)
                val toolProperties = HashMap<String, ToolProperties>()
                val toolComponents = entry.value.getAsJsonArray()
                for (toolComponent in toolComponents) {
                    val toolComponentObject = toolComponent.getAsJsonObject()
                    val item = toolComponentObject.get("item").getAsString()
                    val defaultMiningSpeed = toolComponentObject.get("default_mining_speed").getAsFloat()
                    val damagePerBlock = toolComponentObject.get("damage_per_block").getAsInt()
                    val suitableFor = mixin.blockJsonArrayToIds(version, toolComponentObject.getAsJsonArray("suitable_for"))
                    val toolRules = ArrayList<ToolRule>()
                    val miningSpeeds = toolComponentObject.getAsJsonArray("mining_speeds")
                    for (miningSpeed in miningSpeeds) {
                        val miningSpeedObject = miningSpeed.getAsJsonObject()
                        val blocks = mixin.blockJsonArrayToIds(version, miningSpeedObject.getAsJsonArray("blocks"))
                        val speed = miningSpeedObject.get("speed").getAsFloat()
                        toolRules.add(ToolRule(HolderSet.of(blocks), speed, null))
                    }
                    if (suitableFor.isNotEmpty()) {
                        toolRules.add(ToolRule(HolderSet.of(suitableFor), null, true))
                    }
                    toolProperties[item] = ToolProperties(toolRules.toTypedArray(), defaultMiningSpeed, damagePerBlock)
                }
                mixin.toolDataChanges[version] = toolProperties
            }
            }
        }, ViaLoadingEvent::class.java)
    }

    @Inject(method = ["appendItemDataFixComponents"], at = [At("RETURN")])
    private fun appendItemDataFixComponents(user: UserConnection, item: Item, ci: CallbackInfo) {
        val data: StructuredDataContainer = item.dataContainer()
        val identifier = this.protocol.mappingData!!.fullItemMappings!!.identifier(item.identifier())

        if (user.protocolInfo.serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
            val armorMaxDamage = this.armorMaxDamage_b1_8_1[identifier]
            if (armorMaxDamage != null) {
                data.set(StructuredDataKey.MAX_DAMAGE, armorMaxDamage)
            }
        }

        if (user.protocolInfo.serverProtocolVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_7tob1_7_3)) {
            if (this.foodItems_b1_7_3.contains(identifier)) {
                data.set(StructuredDataKey.MAX_STACK_SIZE, 1)
                data.setEmpty(StructuredDataKey.FOOD1_20_5)
            }
        }

        for (entry in this.toolDataChanges.entries) {
            if (user.protocolInfo.serverProtocolVersion().olderThanOrEqualTo(entry.key)) {
                val toolProperties = entry.value[identifier]
                if (toolProperties != null) {
                    data.set(StructuredDataKey.TOOL1_20_5, toolProperties)
                    break
                }
            }
        }
    }

    @Unique
    private fun blockJsonArrayToIds(protocolVersion: ProtocolVersion, jsonArray: JsonArray): IntArray {
        val ids: IntSet = IntOpenHashSet()
        for (element in jsonArray) {
            val name = element.getAsString()
            if (name.startsWith("#")) { // Material name
                val material = name.substring(1)
                for (entry in ViaProxyMappingDataLoader.BLOCK_MATERIALS.entries) {
                    for (materialEntry in entry.value.entries) {
                        if (protocolVersion.olderThanOrEqualTo(materialEntry.key) && materialEntry.value == material) {
                            ids.add(this.protocol.mappingData!!.blockId(entry.key))
                            break
                        }
                    }
                }
            } else if (name.startsWith("-")) { // Block name
                ids.remove(this.protocol.mappingData!!.blockId(name.substring(1)))
            } else { // Block name
                ids.add(this.protocol.mappingData!!.blockId(name))
            }
        }
        return ids.toIntArray()
    }

}

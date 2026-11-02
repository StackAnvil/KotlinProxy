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

import com.viaversion.nbt.tag.CompoundTag
import com.viaversion.nbt.tag.ListTag
import com.viaversion.viaversion.api.minecraft.item.Item
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifiers1_20_5
import com.viaversion.viaversion.api.type.Type
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap
import com.viaversion.viaversion.libs.gson.JsonElement
import com.viaversion.viaversion.libs.gson.JsonObject
import com.viaversion.viaversion.protocols.v1_8to1_9.Protocol1_8To1_9
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_8
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_9
import com.viaversion.viaversion.protocols.v1_8to1_9.rewriter.ItemPacketRewriter1_9
import com.viaversion.viaversion.rewriter.ItemRewriter
import com.viaversion.viaversion.util.Pair
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyMappingDataLoader
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.HashMap
import java.util.UUID

@Mixin(value = [ItemPacketRewriter1_9::class], remap = false)
abstract class MixinItemPacketRewriter1_9(
    protocol: Protocol1_8To1_9,
    itemType: Type<Item>,
    itemArrayType: Type<Array<Item>>,
    mappedItemType: Type<Item>,
    mappedItemArrayType: Type<Array<Item>>
) : ItemRewriter<ClientboundPackets1_8, ServerboundPackets1_9, Protocol1_8To1_9>(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType) {

    @field:Unique
    private val itemIdentifiers: Int2ObjectMap<String> = Int2ObjectOpenHashMap()

    @field:Unique
    private val itemAttributes: MutableMap<String, MutableMap<String, Pair<String, AttributeModifiers1_20_5.ModifierData>>> = HashMap()

    @Inject(method = ["<init>"], at = [At("RETURN")])
    private fun loadAdditionalData(ci: CallbackInfo) {
        val itemIdentifiers = ViaProxyMappingDataLoader.INSTANCE.loadData("item-identifiers-1.8.json")!!
        for (entry in itemIdentifiers.entrySet()) {
            this.itemIdentifiers.put(entry.value.getAsInt(), entry.key)
        }

        val itemAttributes = ViaProxyMappingDataLoader.INSTANCE.loadData("item-attributes-1.8.json")!!
        for (itemEntry in itemAttributes.entrySet()) {
            val itemIdentifier = itemEntry.key
            val attributes = HashMap<String, Pair<String, AttributeModifiers1_20_5.ModifierData>>()
            for (attributeEntry in itemEntry.value.getAsJsonObject().entrySet()) {
                val attribute = attributeEntry.key
                val attributeData = attributeEntry.value.getAsJsonObject()
                val modifierData = AttributeModifiers1_20_5.ModifierData(UUID.fromString(attributeData.get("id").getAsString()), attributeData.get("name").getAsString(), attributeData.get("amount").getAsDouble(), attributeData.get("operation").getAsInt())
                val slot = attributeData.get("slot").getAsString()
                attributes[attribute] = Pair(slot, modifierData)
            }
            this.itemAttributes[itemIdentifier] = attributes
        }
    }

    @Inject(method = ["handleItemToClient"], at = [At("RETURN")])
    private fun addAttributeFixData(cir: CallbackInfoReturnable<Item>) {
        val item = cir.returnValue
        if (item == null) return

        val identifier = this.itemIdentifiers[item.identifier()]
        if (identifier != null && this.itemAttributes.containsKey(identifier)) {
            val attributes = this.itemAttributes.getValue(identifier)
            val attributeFixTag = CompoundTag()
            var tag = item.tag()
            if (tag == null) {
                tag = CompoundTag()
                item.setTag(tag)
                attributeFixTag.putBoolean("RemoveTag", true)
            }
            tag.put(nbtTagName("attributeFix"), attributeFixTag)

            val attributeModifiers = tag.getListTag("AttributeModifiers", CompoundTag::class.java)
            if (attributeModifiers == null) {
                val newAttributeModifiers = ListTag<CompoundTag>(CompoundTag::class.java)
                for (entry in attributes.entries) {
                    val modifier = entry.value.value()!!
                    val attributeModifier = CompoundTag()
                    attributeModifier.putString("AttributeName", entry.key)
                    attributeModifier.putString("Name", modifier.name())
                    attributeModifier.putDouble("Amount", modifier.amount())
                    attributeModifier.putInt("Operation", modifier.operation())
                    attributeModifier.putLong("UUIDMost", modifier.uuid().mostSignificantBits)
                    attributeModifier.putLong("UUIDLeast", modifier.uuid().leastSignificantBits)
                    attributeModifier.putString("Slot", entry.value.key())
                    newAttributeModifiers.add(attributeModifier)
                }
                tag.put("AttributeModifiers", newAttributeModifiers)
                attributeFixTag.putBoolean("RemoveAttributeModifiers", true)
            }
        }
    }

    @Inject(method = ["handleItemToServer"], at = [At("RETURN")])
    private fun removeAttributeFixData(cir: CallbackInfoReturnable<Item>) {
        val item = cir.returnValue
        if (item == null) return
        val tag = item.tag()
        if (tag == null) return
        val attributeFixTag = tag.removeUnchecked<CompoundTag>(nbtTagName("attributeFix"))
        if (attributeFixTag == null) return

        if (attributeFixTag.contains("RemoveAttributeModifiers")) {
            tag.remove("AttributeModifiers")
        }
        if (attributeFixTag.contains("RemoveTag")) {
            item.setTag(null)
        }
    }

}

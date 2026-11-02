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

import com.viaversion.viaversion.api.minecraft.entitydata.EntityData
import com.viaversion.viaversion.protocols.v1_8to1_9.storage.EntityTracker1_9
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.Slice

@Mixin(value = [EntityTracker1_9::class], remap = false)
abstract class MixinEntityTracker1_9 {

    @Redirect(method = ["handleEntityData"], at = At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F"), slice = Slice(from = At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/configuration/ViaVersionConfig;isBossbarAntiflicker()Z")))
    private fun removeMin(a: Float, b: Float): Float {
        return a
    }

    @Redirect(method = ["handleEntityData"], at = At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), slice = Slice(from = At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/configuration/ViaVersionConfig;isBossbarAntiflicker()Z")))
    private fun removeMax(a: Float, b: Float): Float {
        return b
    }

    @Redirect(method = ["handleEntityData"], at = At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/minecraft/entitydata/EntityData;getValue()Ljava/lang/Object;"), slice = Slice(from = At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/configuration/ViaVersionConfig;isBossbarAntiflicker()Z")))
    private fun remapNaNToZero(instance: EntityData): Any? {
        val value = instance.getValue()
        if (value is Float && value.isNaN()) {
            return 0F
        }

        return value
    }

}

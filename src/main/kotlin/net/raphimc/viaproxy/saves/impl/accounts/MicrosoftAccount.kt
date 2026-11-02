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
package net.raphimc.viaproxy.saves.impl.accounts

import com.google.gson.JsonObject
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.java.JavaAuthManager
import net.raphimc.viaproxy.ViaProxy
import java.util.UUID

class MicrosoftAccount : Account {

    private val javaAuthManager: JavaAuthManager

    constructor(jsonObject: JsonObject) {
        this.javaAuthManager = JavaAuthManager.fromJson(MinecraftAuth.createHttpClient(), jsonObject)
        if (!this.javaAuthManager.getMinecraftProfile().hasValue()) {
            this.javaAuthManager.getMinecraftProfile().refreshUnchecked()
        }
        this.javaAuthManager.getChangeListeners().add { ViaProxy.getSaveManager().save() }
    }

    constructor(javaAuthManager: JavaAuthManager) {
        this.javaAuthManager = javaAuthManager
        javaAuthManager.getMinecraftToken().refreshIfExpired()
        javaAuthManager.getMinecraftProfile().refreshIfExpired()
        javaAuthManager.getMinecraftPlayerCertificates().refreshIfExpired()
        javaAuthManager.getChangeListeners().add { ViaProxy.getSaveManager().save() }
    }

    override fun toJson(): JsonObject = JavaAuthManager.toJson(this.javaAuthManager)

    override fun getName(): String = this.javaAuthManager.getMinecraftProfile().getCached().getName()

    override fun getUUID(): UUID = this.javaAuthManager.getMinecraftProfile().getCached().getId()

    fun getAuthManager(): JavaAuthManager = this.javaAuthManager

    override fun getDisplayString(): String = this.getName() + " (Microsoft)"

}

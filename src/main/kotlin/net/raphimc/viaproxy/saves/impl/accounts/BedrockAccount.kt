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
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager
import net.raphimc.viabedrock.protocol.data.ProtocolConstants
import net.raphimc.viaproxy.ViaProxy
import java.util.UUID

class BedrockAccount : Account {

    private val bedrockAuthManager: BedrockAuthManager

    constructor(jsonObject: JsonObject) {
        this.bedrockAuthManager = BedrockAuthManager.fromJson(MinecraftAuth.createHttpClient(), ProtocolConstants.BEDROCK_VERSION_NAME, jsonObject)
        if (!this.bedrockAuthManager.getMinecraftMultiplayerToken().hasValue()) {
            this.bedrockAuthManager.getMinecraftMultiplayerToken().refreshUnchecked()
        }
        this.bedrockAuthManager.getChangeListeners().add { ViaProxy.getSaveManager().save() }
    }

    constructor(bedrockAuthManager: BedrockAuthManager) {
        this.bedrockAuthManager = bedrockAuthManager
        bedrockAuthManager.getMinecraftMultiplayerToken().refreshIfExpired()
        bedrockAuthManager.getMinecraftCertificateChain().refreshIfExpired()
        bedrockAuthManager.getChangeListeners().add { ViaProxy.getSaveManager().save() }
    }

    override fun toJson(): JsonObject = BedrockAuthManager.toJson(this.bedrockAuthManager)

    override fun getName(): String = this.bedrockAuthManager.getMinecraftMultiplayerToken().getCached().getDisplayName()

    override fun getUUID(): UUID = this.bedrockAuthManager.getMinecraftMultiplayerToken().getCached().getUuid()

    fun getAuthManager(): BedrockAuthManager = this.bedrockAuthManager

    override fun getDisplayString(): String = this.getName() + " (Bedrock)"

}

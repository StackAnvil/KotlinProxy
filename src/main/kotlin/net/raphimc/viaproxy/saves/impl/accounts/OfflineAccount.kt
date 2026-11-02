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
import net.raphimc.vialegacy.api.util.GameProfileUtil
import java.util.UUID

open class OfflineAccount : Account {

    private val playerName: String
    private val playerUuid: UUID

    constructor(jsonObject: JsonObject) {
        this.playerName = jsonObject.get("name").asString
        this.playerUuid = UUID.fromString(jsonObject.get("uuid").asString)
    }

    constructor(name: String) {
        this.playerName = name
        this.playerUuid = GameProfileUtil.getOfflinePlayerUuid(name)
    }

    override fun toJson(): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.addProperty("name", this.playerName)
        jsonObject.addProperty("uuid", this.playerUuid.toString())
        return jsonObject
    }

    override fun getName(): String = this.playerName

    override fun getUUID(): UUID = this.playerUuid

    override fun getDisplayString(): String = this.playerName + " (Offline)"

}

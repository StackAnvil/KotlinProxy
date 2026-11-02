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
package net.raphimc.viaproxy.saves.impl

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.lenni0451.reflect.Classes
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.saves.AbstractSave
import net.raphimc.viaproxy.saves.impl.accounts.Account
import net.raphimc.viaproxy.saves.impl.accounts.OfflineAccount
import net.raphimc.viaproxy.util.logging.Logger
import java.util.Collections

class AccountsSave : AbstractSave("accountsV4") {

    private val accountList = ArrayList<Account>()

    val accounts: List<Account>
        get() = Collections.unmodifiableList(this.accountList)

    @Throws(Exception::class)
    override fun load(jsonElement: JsonElement) {
        val classLoaders = ArrayList<ClassLoader>()
        classLoaders.add(ViaProxy::class.java.classLoader)
        classLoaders.addAll(ViaProxy.getPluginManager().plugins.map { it.classLoader })

        this.accountList.clear()
        for (element in jsonElement.asJsonArray) {
            try {
                val jsonObject = element.asJsonObject
                val type = jsonObject.get("accountType").asString
                val clazz = Classes.find(type, true, classLoaders)

                val account = clazz.getConstructor(JsonObject::class.java).newInstance(jsonObject) as Account
                this.accountList.add(account)
            } catch (e: Throwable) {
                Logger.LOGGER.error("Failed to load an account", e)
            }
        }
    }

    override fun save(): JsonElement {
        val array = JsonArray()
        for (account in this.accountList) {
            val jsonObject = account.toJson()
            jsonObject.addProperty("accountType", account.javaClass.name)
            array.add(jsonObject)
        }
        return array
    }

    fun addAccount(username: String): Account {
        val account = OfflineAccount(username)
        this.accountList.add(account)
        return account
    }

    fun addAccount(account: Account): Account {
        this.accountList.add(account)
        return account
    }

    fun addAccount(index: Int, account: Account): Account {
        this.accountList.add(index, account)
        return account
    }

    fun removeAccount(account: Account) {
        this.accountList.remove(account)
    }

}

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
package net.raphimc.viaproxy.saves

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.lenni0451.reflect.stream.RStream
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.saves.impl.AccountsSave
import net.raphimc.viaproxy.saves.impl.UISave
import net.raphimc.viaproxy.util.logging.Logger
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.HashMap

class SaveManager {

    companion object {
        private val SAVE_FILE = File(ViaProxy.getCwd(), "saves.json")
        private val GSON = Gson()
    }

    @JvmField
    val accountsSave: AccountsSave = AccountsSave()

    @JvmField
    val uiSave: UISave = UISave()

    private val unknownSaves = HashMap<String, JsonElement>()

    init {
        try {
            if (!SAVE_FILE.exists()) {
                SAVE_FILE.createNewFile()
                this.save()
            }

            val reader = FileReader(SAVE_FILE)
            val saveObject = GSON.fromJson(reader, JsonObject::class.java)
            reader.close()

            SaveMigrator.migrate(saveObject)

            RStream
                .of(this)
                .fields()
                .filter { field -> AbstractSave::class.java.isAssignableFrom(field.type()) }
                .forEach { field ->
                    val save: AbstractSave = field.get()
                    try {
                        if (saveObject.has(save.getName())) {
                            save.load(saveObject.remove(save.getName()))
                        }
                    } catch (e: Throwable) {
                        Logger.LOGGER.error("Failed to load save " + save.getName(), e)
                    }
                }

            for (entry in saveObject.entrySet()) {
                this.unknownSaves[entry.key] = entry.value
            }

            this.save()
        } catch (e: Throwable) {
            throw RuntimeException("Failed to initialize SaveManager", e)
        }
    }

    @Synchronized
    fun save() {
        try {
            val saveObject = JsonObject()
            for ((key, value) in this.unknownSaves) {
                saveObject.add(key, value)
            }
            RStream
                .of(this)
                .fields()
                .filter { field -> AbstractSave::class.java.isAssignableFrom(field.type()) }
                .forEach { field ->
                    val save: AbstractSave = field.get()
                    try {
                        val saveData = save.save()
                        if (saveData != null) {
                            saveObject.add(save.getName(), saveData)
                        }
                    } catch (e: Throwable) {
                        Logger.LOGGER.error("Failed to save save " + save.getName(), e)
                    }
                }

            val writer = FileWriter(SAVE_FILE)
            GSON.toJson(saveObject, writer)
            writer.close()
        } catch (e: Throwable) {
            Logger.LOGGER.error("Failed to save saves to file", e)
        }
    }

}

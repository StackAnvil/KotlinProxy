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

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import net.raphimc.viaproxy.saves.AbstractSave
import java.util.HashMap
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel

class UISave : AbstractSave("ui") {

    private val values = HashMap<String, String>()

    override fun load(jsonElement: JsonElement) {
        this.values.clear()
        for (entry in jsonElement.asJsonObject.entrySet()) {
            this.values[entry.key] = entry.value.asString
        }
    }

    override fun save(): JsonElement {
        val jsonObject = JsonObject()
        for (entry in this.values.entries) {
            jsonObject.addProperty(entry.key, entry.value)
        }
        return jsonObject
    }

    fun put(key: String, value: String) {
        this.values[key] = value
    }

    fun get(key: String): String? = this.values[key]

    fun loadTextField(key: String, textField: JTextField) {
        if (this.values.containsKey(key)) {
            try {
                val value = this.values[key]
                if (value != null) textField.setText(value)
            } catch (ignored: Throwable) {
            }
        }
    }

    fun loadComboBox(key: String, comboBox: JComboBox<*>) {
        if (this.values.containsKey(key)) {
            try {
                val index = this.values[key]!!.toInt()
                if (index >= 0 && index < comboBox.getItemCount()) comboBox.setSelectedIndex(index)
            } catch (ignored: Throwable) {
            }
        }
    }

    fun loadComboBoxProtocolVersion(key: String, comboBox: JComboBox<ProtocolVersion>) {
        if (this.values.containsKey(key)) {
            try {
                val value = this.values[key]!!
                val version = ProtocolVersion.getClosest(value)
                if (version != null) {
                    comboBox.setSelectedItem(version)
                }
            } catch (ignored: Throwable) {
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun loadSpinner(key: String, spinner: JSpinner) {
        if (this.values.containsKey(key)) {
            try {
                val value = Integer.valueOf(this.values[key]!!)
                val model = spinner.getModel()
                if (model is SpinnerNumberModel) {
                    val minimum = model.getMinimum() as Comparable<Int>
                    val maximum = model.getMaximum() as Comparable<Int>
                    if (minimum.compareTo(value) <= 0 && maximum.compareTo(value) >= 0) spinner.setValue(value)
                } else {
                    spinner.setValue(value)
                }
            } catch (ignored: Throwable) {
            }
        }
    }

    fun loadCheckBox(key: String, checkBox: JCheckBox) {
        if (this.values.containsKey(key)) {
            try {
                val value = java.lang.Boolean.parseBoolean(this.values[key])
                checkBox.setSelected(value)
            } catch (ignored: Throwable) {
            }
        }
    }

}

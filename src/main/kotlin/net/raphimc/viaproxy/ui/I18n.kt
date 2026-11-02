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
package net.raphimc.viaproxy.ui

import net.raphimc.viabedrock.api.util.FileSystemUtil
import net.raphimc.viaproxy.ViaProxy
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Properties

object I18n {

    private const val DEFAULT_LOCALE = "en_US"
    private var LOCALES: MutableMap<String, Properties> = LinkedHashMap()
    private var currentLocale: String? = null

    init {
        if (ViaProxy.getSaveManager() == null) {
            throw IllegalStateException("ViaProxy is not yet initialized")
        }

        try {
            for (entry in FileSystemUtil.getFilesInDirectory("assets/viaproxy/language").entries) {
                val properties = Properties()
                properties.load(InputStreamReader(ByteArrayInputStream(entry.value), StandardCharsets.UTF_8))
                if (properties.isEmpty()) continue
                LOCALES[entry.key.fileName.toString().replace(".properties", "")] = properties
            }
        } catch (e: Throwable) {
            throw RuntimeException("Failed to load translations", e)
        }
        val sortedLocales = LOCALES.entries.sortedBy { entry -> entry.value.getProperty("language.name")!! }
        LOCALES = LinkedHashMap<String, Properties>().also { map ->
            for (entry in sortedLocales) {
                map[entry.key] = entry.value
            }
        }

        val savedLocale = ViaProxy.getSaveManager().uiSave.get("locale")
        currentLocale = savedLocale
        if (savedLocale == null || !LOCALES.containsKey(savedLocale)) {
            val systemLocale = Locale.getDefault().getLanguage() + '_' + Locale.getDefault().getCountry()
            if (LOCALES.containsKey(systemLocale)) {
                currentLocale = systemLocale
            } else {
                for (entry in LOCALES.entries) {
                    if (entry.key.startsWith(Locale.getDefault().getLanguage() + '_')) {
                        currentLocale = entry.key
                        break
                    }
                }
            }
        }

        val totalTranslation = LOCALES[DEFAULT_LOCALE]!!.size
        for (properties in LOCALES.values) {
            val translated = properties.size
            val percentage = translated.toFloat() / totalTranslation * 100
            properties.put("language.completion", Math.floor(percentage.toDouble()).toInt().toString() + "%")
        }
    }

    @JvmStatic
    fun get(key: String): String {
        return getSpecific(currentLocale, key)
    }

    @JvmStatic
    fun get(key: String, vararg args: String?): String {
        return String.format(getSpecific(currentLocale, key), *args)
    }

    @JvmStatic
    fun getSpecific(locale: String?, key: String): String {
        var properties = if (locale != null) LOCALES[locale] else null
        if (properties == null) {
            properties = LOCALES[DEFAULT_LOCALE]
        }
        var value = properties!!.getProperty(key)
        if (value == null) {
            value = LOCALES[DEFAULT_LOCALE]!!.getProperty(key)
        }
        if (value == null) {
            return "Missing translation for key: " + key
        }
        return value
    }

    @JvmStatic
    fun getCurrentLocale(): String? {
        return currentLocale
    }

    @JvmStatic
    fun setLocale(locale: String) {
        if (ViaProxy.getSaveManager() == null) {
            throw IllegalStateException("ViaProxy is not yet initialized")
        }

        currentLocale = locale
        ViaProxy.getSaveManager().uiSave.put("locale", locale)
        ViaProxy.getSaveManager().save()
    }

    @JvmStatic
    fun getAvailableLocales(): Collection<String> {
        return LOCALES.keys
    }

}

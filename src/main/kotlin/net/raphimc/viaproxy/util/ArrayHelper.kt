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
package net.raphimc.viaproxy.util

class ArrayHelper(array: Array<String>) {

    companion object {
        @JvmStatic
        fun instanceOf(vararg array: String): ArrayHelper {
            return ArrayHelper(arrayOf(*array))
        }
    }


    private var array: Array<String> = array

    fun getLength(): Int {
        return this.array.size
    }

    fun isLength(length: Int): Boolean {
        return this.getLength() == length
    }

    fun isSmaller(length: Int): Boolean {
        return this.getLength() < length
    }

    fun isSmallerOrEqual(length: Int): Boolean {
        return this.getLength() <= length
    }

    fun isLarger(length: Int): Boolean {
        return this.getLength() > length
    }

    fun isLargerOrEqual(length: Int): Boolean {
        return this.getLength() >= length
    }

    fun isEmpty(): Boolean {
        return this.getLength() == 0
    }

    fun isIndexValid(index: Int): Boolean {
        return index >= 0 && index < this.getLength()
    }

    fun get(index: Int): String? {
        if (!this.isIndexValid(index)) {
            return null
        }

        return this.array[index]
    }


    fun isString(index: Int): Boolean {
        return this.isIndexValid(index)
    }

    fun isBoolean(index: Int): Boolean {
        if (!this.isIndexValid(index)) {
            return false
        }

        try {
            java.lang.Boolean.valueOf(this.getString(index))
            return true
        } catch (ignored: Exception) {
        }

        return false
    }

    fun isChar(index: Int): Boolean {
        if (!this.isIndexValid(index) || !this.isString(index)) {
            return false
        }

        return this.getString(index)!!.length == 1
    }

    fun isShort(index: Int): Boolean {
        if (!this.isIndexValid(index)) {
            return false
        }

        try {
            java.lang.Short.valueOf(this.get(index))
            return true
        } catch (ignored: Exception) {
        }

        return false
    }

    fun isInteger(index: Int): Boolean {
        if (!this.isIndexValid(index)) {
            return false
        }

        try {
            java.lang.Integer.valueOf(this.get(index))
            return true
        } catch (ignored: Exception) {
        }

        return false
    }

    fun isLong(index: Int): Boolean {
        if (!this.isIndexValid(index)) {
            return false
        }

        try {
            java.lang.Long.valueOf(this.get(index))
            return true
        } catch (ignored: Exception) {
        }

        return false
    }

    fun isFloat(index: Int): Boolean {
        if (!this.isIndexValid(index)) {
            return false
        }

        try {
            java.lang.Float.valueOf(this.get(index))
            return true
        } catch (ignored: Exception) {
        }

        return false
    }

    fun isDouble(index: Int): Boolean {
        if (!this.isIndexValid(index)) {
            return false
        }

        try {
            java.lang.Double.valueOf(this.get(index))
            return true
        } catch (ignored: Exception) {
        }

        return false
    }


    fun getString(index: Int, standard: String): String? {
        if (!this.isIndexValid(index) || !this.isString(index)) {
            return standard
        }

        return this.get(index)
    }

    fun getBoolean(index: Int, standard: Boolean): Boolean {
        if (!this.isIndexValid(index) || !this.isBoolean(index)) {
            return standard
        }

        return java.lang.Boolean.parseBoolean(this.getString(index))
    }

    fun getChar(index: Int, standard: Char): Char {
        if (!this.isIndexValid(index) || !this.isChar(index)) {
            return standard
        }

        return this.getString(index, standard.toString())!![0]
    }

    fun getShort(index: Int, standard: Short): Short {
        if (!this.isIndexValid(index) || !this.isShort(index)) {
            return standard
        }

        return java.lang.Short.parseShort(this.get(index))
    }

    fun getInteger(index: Int, standard: Int): Int {
        if (!this.isIndexValid(index) || !this.isInteger(index)) {
            return standard
        }

        return java.lang.Integer.parseInt(this.get(index))
    }

    fun getLong(index: Int, standard: Long): Long {
        if (!this.isIndexValid(index) || !this.isLong(index)) {
            return standard
        }

        return java.lang.Long.parseLong(this.get(index))
    }

    fun getFloat(index: Int, standard: Float): Float {
        if (!this.isIndexValid(index) || !this.isFloat(index)) {
            return standard
        }

        return java.lang.Float.parseFloat(this.get(index))
    }

    fun getDouble(index: Int, standard: Double): Double {
        if (!this.isIndexValid(index) || !this.isDouble(index)) {
            return standard
        }

        return java.lang.Double.parseDouble(this.get(index))
    }


    fun getString(index: Int): String? {
        return this.getString(index, "")
    }

    fun getBoolean(index: Int): Boolean {
        return this.getBoolean(index, false)
    }

    fun getChar(index: Int): Char {
        return this.getChar(index, "A".toCharArray()[0])
    }

    fun getShort(index: Int): Short {
        return this.getShort(index, 0.toShort())
    }

    fun getInteger(index: Int): Int {
        return this.getInteger(index, 0)
    }

    fun getLong(index: Int): Long {
        return this.getLong(index, 0)
    }

    fun getFloat(index: Int): Float {
        return this.getFloat(index, 0f)
    }

    fun getDouble(index: Int): Double {
        return this.getDouble(index, 0.0)
    }

    fun add(`object`: String, vararg objects: String): ArrayHelper {
        this.array = this.advance(`object`, *objects)
        return this
    }


    fun advance(obToAdd: String, vararg obs: String): Array<String> {
        val newArray = Array<String>(this.getLength() + 1 + obs.size) { "" }

        var i = 0
        for (ob in this.array) {
            newArray[i] = ob

            i++
        }
        newArray[i] = obToAdd
        i++
        for (ob in obs) {
            newArray[i] = ob
            i++
        }

        return newArray
    }

    fun advanceToStrings(strToAdd: String, vararg strs: String): Array<String> {
        val newArray = Array<String>(this.getLength() + 1 + strs.size) { "" }

        var i = 0
        for (ob in this.array) {
            newArray[i] = ob.toString()

            i++
        }
        newArray[i] = strToAdd
        i++
        for (str in strs) {
            newArray[i] = str
            i++
        }

        return newArray
    }

    fun getAsArray(): Array<String> {
        return this.array
    }

    fun getAsString(): String {
        return this.getAsString(0, " ")
    }

    fun getAsString(combiner: String): String {
        return this.getAsString(0, combiner)
    }

    fun getAsString(start: Int): String {
        return this.getAsString(start, " ")
    }

    fun getAsString(start: Int, combiner: String): String {
        return this.getAsString(start, this.getLength() - 1, combiner)
    }

    fun getAsString(start: Int, end: Int): String {
        return this.getAsString(start, end, " ")
    }

    fun getAsString(start: Int, end: Int, combiner: String): String {
        var startIndex = start
        var endIndex = end
        if (startIndex < 0) {
            startIndex = 0
        }
        if (endIndex > this.getLength() - 1) {
            endIndex = this.getLength() - 1
        }
        if (endIndex < startIndex) {
            return ""
        }

        var out = StringBuilder()
        for (i in startIndex..endIndex) {
            if (out.isEmpty()) {
                out = StringBuilder(this.getString(i))
            } else {
                out.append(combiner).append(this.getString(i))
            }
        }
        return out.toString()
    }


    override fun toString(): String {
        val complete = StringBuilder()
        for (t in this.array) {
            complete.append(if (complete.isEmpty()) "" else ", ").append(t)
        }
        return "[$complete]"
    }

}

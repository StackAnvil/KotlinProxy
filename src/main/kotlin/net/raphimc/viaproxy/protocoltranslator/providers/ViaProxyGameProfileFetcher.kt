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
package net.raphimc.viaproxy.protocoltranslator.providers

import com.mojang.authlib.ProfileLookupCallback
import com.mojang.authlib.yggdrasil.ProfileNotFoundException
import com.viaversion.viaversion.api.minecraft.GameProfile
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.provider.GameProfileFetcher
import net.raphimc.viaproxy.proxy.external_interface.AuthLibServices
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ViaProxyGameProfileFetcher : GameProfileFetcher() {

    override fun loadMojangUuid(playerName: String): UUID {
        val future = CompletableFuture<com.mojang.authlib.GameProfile>()
        AuthLibServices.PROFILE_REPOSITORY.findProfilesByNames(arrayOf(playerName), object : ProfileLookupCallback {
            override fun onProfileLookupSucceeded(gameProfile: com.mojang.authlib.GameProfile) {
                future.complete(gameProfile)
            }

            override fun onProfileLookupFailed(profileName: String, exception: Exception) {
                future.completeExceptionally(exception)
            }
        })
        if (!future.isDone) {
            future.completeExceptionally(ProfileNotFoundException())
        }
        return future.get().getId()
    }

    override fun loadGameProfile(uuid: UUID): GameProfile {
        val result = AuthLibServices.SESSION_SERVICE.fetchProfile(uuid, true)
        if (result == null) {
            throw ProfileNotFoundException()
        }

        val gameProfile = result.profile()
        val properties = ArrayList<GameProfile.Property>(gameProfile.properties.size())
        for (entry in gameProfile.properties.entries()) {
            properties.add(GameProfile.Property(entry.value.name(), entry.value.value(), entry.value.signature()))
        }
        return GameProfile(gameProfile.name, gameProfile.id, properties.toTypedArray())
    }

}

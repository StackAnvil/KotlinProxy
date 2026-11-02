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
package net.raphimc.viaproxy.cli.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager
import net.raphimc.minecraftauth.java.JavaAuthManager
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService
import net.raphimc.viabedrock.protocol.data.ProtocolConstants
import net.raphimc.viaproxy.ViaProxy
import net.raphimc.viaproxy.cli.command.Command
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor
import net.raphimc.viaproxy.saves.impl.accounts.Account
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount
import net.raphimc.viaproxy.util.TFunction
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

class AccountCommand : Command(
    "account",
    "Manage the account list and the currently selected account",
    "account list/select <index>/deselect/add (offline <name>/microsoft/bedrock)/remove <index>"
) {

    override fun register(builder: LiteralArgumentBuilder<CommandExecutor>) {
        builder.then(literal("list").executes { ctx ->
            val accounts = ViaProxy.getSaveManager().accountsSave.accounts
            if (accounts.isEmpty()) {
                ctx.source.sendMessage("No accounts added yet.")
            } else {
                for (i in accounts.indices) {
                    val isSelected = ViaProxy.getConfig().getAccount() === accounts[i]
                    ctx.source.sendMessage("[" + i + "] " + accounts[i].getDisplayString() + (if (isSelected) " <--" else ""))
                }
            }
            1
        })
        builder.then(literal("deselect").executes { ctx ->
            ViaProxy.getConfig().setAccount(null)
            ctx.source.sendMessage("Deselected current account.")
            1
        })
        builder.then(literal("select").then(argument("index", IntegerArgumentType.integer(0)).executes { ctx ->
            val accounts = ViaProxy.getSaveManager().accountsSave.accounts
            if (accounts.isEmpty()) {
                ctx.source.sendMessage("No accounts to select from.")
                return@executes 0
            }
            val index = IntegerArgumentType.getInteger(ctx, "index")
            if (index < 0 || index >= accounts.size) {
                ctx.source.sendMessage("Invalid account index (run 'account list' to see a list of all accounts).")
                return@executes 0
            }
            val account = ViaProxy.getSaveManager().accountsSave.accounts[index]
            ViaProxy.getConfig().setAccount(account)
            ctx.source.sendMessage("Selected account " + index + ": " + account.getDisplayString() + ".")
            1
        }))
        builder.then(
            literal("add")
                .then(literal("offline").then(argument("name", StringArgumentType.string()).executes { ctx ->
                    val name = StringArgumentType.getString(ctx, "name")
                    ViaProxy.getSaveManager().accountsSave.addAccount(name)
                    ViaProxy.getSaveManager().save()
                    ctx.source.sendMessage("Added offline account '" + name + "'.")
                    1
                }))
                .then(literal("microsoft").executes { ctx ->
                    this.handleLogin(ctx.source) { msaDeviceCodeConsumer ->
                        MicrosoftAccount(
                            JavaAuthManager.create(MinecraftAuth.createHttpClient()).login<Consumer<MsaDeviceCode>>(
                                { httpClient, applicationConfig, consumer ->
                                    DeviceCodeMsaAuthService(httpClient, applicationConfig, consumer)
                                },
                                msaDeviceCodeConsumer
                            )
                        )
                    }
                })
                .then(literal("bedrock").executes { ctx ->
                    this.handleLogin(ctx.source) { msaDeviceCodeConsumer ->
                        BedrockAccount(
                            BedrockAuthManager.create(MinecraftAuth.createHttpClient(), ProtocolConstants.BEDROCK_VERSION_NAME)
                                .login<Consumer<MsaDeviceCode>>(
                                    { httpClient, applicationConfig, consumer ->
                                        DeviceCodeMsaAuthService(httpClient, applicationConfig, consumer)
                                    },
                                    msaDeviceCodeConsumer
                                )
                        )
                    }
                })
        )
        builder.then(literal("remove").then(argument("index", IntegerArgumentType.integer(0)).executes { ctx ->
            val accounts = ViaProxy.getSaveManager().accountsSave.accounts
            if (accounts.isEmpty()) {
                ctx.source.sendMessage("No accounts to remove.")
                return@executes 0
            }
            val index = IntegerArgumentType.getInteger(ctx, "index")
            if (index < 0 || index >= accounts.size) {
                ctx.source.sendMessage("Invalid account index (run 'account list' to see a list of all accounts).")
                return@executes 0
            }
            val account = ViaProxy.getSaveManager().accountsSave.accounts[index]
            ViaProxy.getSaveManager().accountsSave.removeAccount(account)
            ViaProxy.getSaveManager().save()
            if (ViaProxy.getConfig().getAccount() === account) {
                ViaProxy.getConfig().setAccount(null)
            }
            ctx.source.sendMessage("Removed account " + index + ": " + account.getDisplayString() + ".")
            1
        }))
    }

    private fun handleLogin(source: CommandExecutor, requestHandler: TFunction<Consumer<MsaDeviceCode>, Account>): Int {
        try {
            val account = requestHandler.apply(Consumer { code ->
                source.sendMessage("Please open your browser and visit " + code.directVerificationUri + " and login with your Microsoft account.")
                source.sendMessage("If the code is not inserted automatically, please enter the code: " + code.userCode + ".")
            })
            ViaProxy.getSaveManager().accountsSave.addAccount(account)
            ViaProxy.getSaveManager().save()
            return 1
        } catch (ignored: InterruptedException) {
        } catch (e: TimeoutException) {
            source.sendMessage("The authentication process timed out.")
        } catch (t: Throwable) {
            source.sendMessage("An error occurred while trying to authenticate: " + t.message)
            t.printStackTrace()
        }
        return 0
    }

}

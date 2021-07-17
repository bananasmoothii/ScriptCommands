/*
 *    Copyright 2020 ScriptCommands
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fr.bananasmoothii.scriptcommands.bukkit;


import fr.bananasmoothii.scriptcommands.Util;
import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.execution.*;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static fr.bananasmoothii.scriptcommands.core.execution.ScriptValue.NONE;

@SuppressWarnings("unused")
public class BukkitUsableFunctions {

    private static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(ScriptCommands.inst(), runnable);
    }

    private static List<@Nullable Player> getPlayersFromList(ScriptValue<?> arg, @NotNull Context context) {
        List<Player> players = new ArrayList<>();
        Server server = Bukkit.getServer();
        if (arg.is(ScriptValue.ScriptValueType.TEXT)) {
            players.add(server.getPlayer(arg.asString()));
            return players;
        } else if (arg.is(ScriptValue.ScriptValueType.LIST)) {
            for (ScriptValue<Object> arg1 : arg.asList()) {
                players.add(server.getPlayer(arg1.asString()));
            }
            return players;
        }
        throw new ScriptException(ScriptException.ExceptionType.INVALID_TYPE, context, "Passed argument is not a list or a text containing a player name. It is: " +
                Types.getPrettyArgAndType(arg));
    }

    @ScriptFunctionMethod
    public static ScriptValue<NoneType> console_cmd(Args args) {
        runSync(() -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), args.getSingleArg().asString()));
        return NONE;
    }

    @NamingPatternProvider
    public static final Args.NamingPattern player_cmd = new Args.NamingPattern()
            .setNamingPattern("cmd", "player");
    @ScriptFunctionMethod
    public static ScriptValue<NoneType> player_cmd(Args args) {
        String cmd = args.getArg("cmd").asString();

        ScriptValue<Object> playersArg = args.getArgIfExist("player");
        if (playersArg == null) playersArg = new ScriptValue<>(args.context.getTriggeringPlayer());
        List<@Nullable Player> players = getPlayersFromList(playersArg, args.context);

        CustomLogger.finer("making player " + Util.join(", ", players) + " run " + cmd);
        for (@Nullable Player player : players) {
            if (player != null)
                player.performCommand(cmd);
        }
        return NONE;
    }

    @NamingPatternProvider
    public static final Args.NamingPattern player_msg = new Args.NamingPattern()
            .setNamingPattern("msg", "player");
    @ScriptFunctionMethod
    public static ScriptValue<NoneType> player_msg(Args args) {
        String msg = args.getArg("msg").asString();

        ScriptValue<Object> playersArg = args.getArgIfExist("player");
        if (playersArg == null) playersArg = new ScriptValue<>(args.context.getTriggeringPlayer());
        List<@Nullable Player> players = getPlayersFromList(playersArg, args.context);

        CustomLogger.finer("making player " + Util.join(", ", players) + " run " + msg);
        for (@Nullable Player player : players) {
            if (player != null)
                player.performCommand(msg);
        }
        return NONE;
    }

    // TODO: import and manage functions
}

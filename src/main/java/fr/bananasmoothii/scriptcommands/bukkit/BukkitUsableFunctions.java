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


import fr.bananasmoothii.scriptcommands.core.CustomLogger;
import fr.bananasmoothii.scriptcommands.core.execution.*;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class BukkitUsableFunctions {
    private static final ScriptValue<NoneType> NONE = new ScriptValue<>(null);

    private static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(ScriptCommands.inst(), runnable);
    }

    private static List<@Nullable Player> getPlayersFromList(ScriptValue<Object> arg) {
        List<Player> players = new ArrayList<>();
        Server server = Bukkit.getServer();
        if (arg.is(ScriptValue.SVType.TEXT)) {
            players.add(server.getPlayer(arg.asString()));
            return players;
        } else if (arg.is(ScriptValue.SVType.LIST)) {
            for (ScriptValue<Object> arg1 : arg.asList()) {
                players.add(server.getPlayer(arg1.asString()));
            }
            return players;
        }
        throw new ScriptException(ScriptException.ExceptionType.INVALID_TYPE, "[getting player]", arg.toString(), "passed argument is not a list or a text containing a player name.");
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
        ScriptValue<Object> playersArg = args.getArgIfExist("player");
        String cmd = args.getArg("cmd").asString();
        List<@Nullable Player> players = getP
        CustomLogger.finer("making player " + String.join(", ", players) + " run " + cmd);
        return NONE;
    }

    @NamingPatternProvider
    public static final Args.NamingPattern player_msg = new Args.NamingPattern()
            .setNamingPattern("msg", "player");
    @ScriptFunctionMethod
    public static ScriptValue<NoneType> player_msg(Args args) {
        ScriptValue<Object> players = args.getArgIfExist("player");
        String msg = args.getArg("msg").asString();

        ArrayList<String> playersInArg = new ArrayList<>();
        if (players == null) {
            playersInArg.add(args.context.normalVariables.get("player").asString());
        }
        else {
            for (ScriptValue<?> player: players.asList()) {
                playersInArg.add(player.asString());
            }
        }
        CustomLogger.finer("player " + String.join(", ", playersInArg) + " was sent a message: " + msg);
        return NONE;
    }

    // TODO: import and manage functions
}

/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.commands;

import com.djrapitops.plan.commands.subcommands.Confirmation;
import com.djrapitops.plan.commands.subcommands.LinkCommands;
import com.djrapitops.plan.commands.subcommands.RegistrationCommands;
import com.djrapitops.plan.commands.use.Arguments;
import com.djrapitops.plan.commands.use.CMDSender;
import com.djrapitops.plan.commands.use.CommandWithSubcommands;
import com.djrapitops.plan.commands.use.Subcommand;
import com.djrapitops.plan.settings.locale.Locale;
import com.djrapitops.plan.utilities.logging.ErrorContext;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import com.djrapitops.plugin.command.ColorScheme;
import com.djrapitops.plugin.logging.L;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
public class PlanCommand {

    private final Locale locale;
    private final ColorScheme colors;
    private final Confirmation confirmation;
    private final LinkCommands linkCommands;
    private final RegistrationCommands registrationCommands;
    private final ErrorLogger errorLogger;

    @Inject
    public PlanCommand(
            Locale locale,
            ColorScheme colors,
            Confirmation confirmation,
            LinkCommands linkCommands,
            RegistrationCommands registrationCommands,
            ErrorLogger errorLogger
    ) {
        this.locale = locale;
        this.colors = colors;
        this.confirmation = confirmation;
        this.linkCommands = linkCommands;
        this.registrationCommands = registrationCommands;
        this.errorLogger = errorLogger;
    }

    private void handleException(RuntimeException error, CMDSender sender, Arguments arguments) {
        if (error instanceof IllegalArgumentException) {
            sender.send("§c" + error.getMessage());
        } else {
            errorLogger.log(L.WARN, error, ErrorContext.builder().related(sender, arguments).build());
        }
    }

    public CommandWithSubcommands build() {
        return CommandWithSubcommands.builder()
                .alias("plan")
                .colorScheme(colors)
                .subcommand(serverCommand())
                .subcommand(serversCommand())
                .subcommand(playerCommand())
                .subcommand(playersCommand())
                .subcommand(networkCommand())
                .subcommand(registerCommand())
                .subcommand(unregisterCommand())
                .subcommand(acceptCommand())
                .subcommand(cancelCommand())
                .exceptionHandler(this::handleException)
                .build();
    }

    public List<String> serverNames(CMDSender sender, Arguments arguments) {
        return Collections.emptyList(); // TODO
    }

    private List<String> playerNames(CMDSender sender, Arguments arguments) {
        return Collections.emptyList(); // TODO
    }

    private Subcommand serverCommand() {
        return Subcommand.builder()
                .aliases("server", "analyze", "a", "analyse", "analysis")
                .optionalArgument("server", "Name, ID or UUID of a server")
                .requirePermission("plan.server")
                .description("View a server page")
                .inDepthDescription("Obtain a link to the /server page of a specific server, or the current server if no arguments are given.")
                .onTabComplete(this::serverNames)
                .onCommand(linkCommands::onServerCommand)
                .build();
    }

    private Subcommand serversCommand() {
        return Subcommand.builder()
                .aliases("servers", "serverlist", "listservers", "sl", "ls")
                .requirePermission("plan.servers")
                .description("List servers")
                .inDepthDescription("List ids, names and uuids of servers in the database.")
                .onCommand(linkCommands::onServersCommand)
                .build();
    }

    private Subcommand playerCommand() {
        return Subcommand.builder()
                .aliases("player", "inspect")
                .optionalArgument("name/uuid", "Name or UUID of a player")
                .requirePermission("plan.player.self")
                .description("View a player page")
                .inDepthDescription("Obtain a link to the /player page of a specific player, or the current player.")
                .onTabComplete(this::playerNames)
                .onCommand(linkCommands::onPlayerCommand)
                .build();
    }

    private Subcommand playersCommand() {
        return Subcommand.builder()
                .aliases("players", "pl", "playerlist", "list")
                .requirePermission("plan.player.other")
                .description("View players page")
                .inDepthDescription("Obtain a link to the /players page to see a list of players.")
                .onCommand(linkCommands::onPlayersCommand)
                .build();
    }

    private Subcommand networkCommand() {
        return Subcommand.builder()
                .aliases("network", "netw")
                .requirePermission("plan.network")
                .description("View network page")
                .inDepthDescription("Obtain a link to the /network page, only does so on networks.")
                .onCommand(linkCommands::onNetworkCommand)
                .build();
    }

    private Subcommand registerCommand() {
        return Subcommand.builder()
                .aliases("register")
                .requirePermission("plan.register.self")
                .optionalArgument("--code ${code}", "Code used to finalize registration.")
                .description("Register a user for Plan website")
                .inDepthDescription("Use without arguments to get link to register page. Use --code [code] after registration to get a user.")
                .onCommand(registrationCommands::onRegister)
                .onTabComplete((sender, arguments) -> arguments.isEmpty() ? Collections.singletonList("--code") : Collections.emptyList())
                .build();
    }

    private Subcommand unregisterCommand() {
        return Subcommand.builder()
                .aliases("unregister")
                .requirePermission("plan.unregister.self")
                .optionalArgument("username", "Username of another user. If not specified linked user is used.")
                .description("Unregister user of Plan website")
                .inDepthDescription("Use without arguments to unregister linked user, or with username argument to unregister another user.")
                .onCommand(((sender, arguments) -> registrationCommands.onUnregister("plan", sender, arguments)))
                .build();
    }

    private Subcommand acceptCommand() {
        return Subcommand.builder()
                .aliases("accept", "yes", "y")
                .onCommand(confirmation::onAcceptCommand)
                .build();
    }

    private Subcommand cancelCommand() {
        return Subcommand.builder()
                .aliases("cancel", "deny", "no", "n")
                .onCommand(confirmation::onCancelCommand)
                .build();
    }
}

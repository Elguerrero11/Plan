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
package com.djrapitops.plan.commands.use;

import com.djrapitops.plan.commands.Arguments;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface SubcommandBuilder {

    SubcommandBuilder alias(String alias);

    SubcommandBuilder aliases(String... aliases);

    SubcommandBuilder requirePermission(String permission);

    SubcommandBuilder description(String description);

    default SubcommandBuilder inDepthDescription(String inDepthDescription) {
        return inDepthDescription(StringUtils.split(inDepthDescription, '\n'));
    }

    SubcommandBuilder inDepthDescription(String... lines);

    SubcommandBuilder onCommand(BiConsumer<CMDSender, Arguments> executor);

    SubcommandBuilder onTabComplete(BiFunction<CMDSender, Arguments, List<String>> resolver);

    Subcommand build();

}
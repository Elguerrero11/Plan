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
package com.djrapitops.plan.delivery.webserver.pages.json;

import com.djrapitops.plan.delivery.rendering.json.network.NetworkTabJSONParser;
import com.djrapitops.plan.delivery.webserver.Request;
import com.djrapitops.plan.delivery.webserver.RequestTarget;
import com.djrapitops.plan.delivery.webserver.auth.Authentication;
import com.djrapitops.plan.delivery.webserver.cache.DataID;
import com.djrapitops.plan.delivery.webserver.cache.JSONCache;
import com.djrapitops.plan.delivery.webserver.pages.PageHandler;
import com.djrapitops.plan.delivery.webserver.response.Response;
import com.djrapitops.plan.delivery.webserver.response.data.JSONResponse;
import com.djrapitops.plan.exceptions.WebUserAuthException;

import java.util.function.Supplier;

/**
 * Generic Tab JSON handler for any tab's data.
 *
 * @author Rsl1122
 */
public class NetworkTabJSONHandler<T> implements PageHandler {

    private final DataID dataID;
    private final Supplier<T> jsonParser;

    public NetworkTabJSONHandler(DataID dataID, NetworkTabJSONParser<T> jsonParser) {
        this.dataID = dataID;
        this.jsonParser = jsonParser;
    }

    @Override
    public Response getResponse(Request request, RequestTarget target) {
        return JSONCache.getOrCache(dataID, () -> new JSONResponse(jsonParser.get()));
    }

    @Override
    public boolean isAuthorized(Authentication auth, RequestTarget target) throws WebUserAuthException {
        return auth.getWebUser().getPermLevel() <= 0;
    }
}
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
package com.djrapitops.plan.delivery.webserver;

import com.djrapitops.plan.delivery.domain.container.PlayerContainer;
import com.djrapitops.plan.delivery.rendering.html.icon.Family;
import com.djrapitops.plan.delivery.rendering.html.icon.Icon;
import com.djrapitops.plan.delivery.rendering.pages.Page;
import com.djrapitops.plan.delivery.rendering.pages.PageFactory;
import com.djrapitops.plan.delivery.web.resolver.MimeType;
import com.djrapitops.plan.delivery.web.resolver.Response;
import com.djrapitops.plan.delivery.webserver.auth.FailReason;
import com.djrapitops.plan.exceptions.WebUserAuthException;
import com.djrapitops.plan.exceptions.connection.NotFoundException;
import com.djrapitops.plan.settings.locale.Locale;
import com.djrapitops.plan.settings.locale.lang.ErrorPageLang;
import com.djrapitops.plan.settings.theme.Theme;
import com.djrapitops.plan.storage.database.DBSystem;
import com.djrapitops.plan.storage.database.Database;
import com.djrapitops.plan.storage.database.queries.containers.ContainerFetchQueries;
import com.djrapitops.plan.storage.file.PlanFiles;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Factory for creating different {@link Response} objects.
 *
 * @author Rsl1122
 */
@Singleton
public class ResponseFactory {

    private final PlanFiles files;
    private final PageFactory pageFactory;
    private final Locale locale;
    private final DBSystem dbSystem;
    private final Theme theme;

    @Inject
    public ResponseFactory(
            PlanFiles files,
            PageFactory pageFactory,
            Locale locale,
            DBSystem dbSystem,
            Theme theme
    ) {
        this.files = files;
        this.pageFactory = pageFactory;
        this.locale = locale;
        this.dbSystem = dbSystem;
        this.theme = theme;
    }

    public Response debugPageResponse() {
        try {
            return forPage(pageFactory.debugPage());
        } catch (IOException e) {
            return forInternalError(e, "Failed to generate debug page");
        }
    }

    private Response forPage(Page page) {
        return Response.builder()
                .setMimeType(MimeType.HTML)
                .setContent(page.toHtml())
                .build();
    }

    private Response forInternalError(Throwable error, String cause) {
        return Response.builder()
                .setMimeType(MimeType.HTML)
                .setContent(pageFactory.internalErrorPage(cause, error).toHtml())
                .setStatus(500)
                .build();
    }

    public Response playersPageResponse() {
        try {
            Optional<Response> error = checkDbClosedError();
            if (error.isPresent()) return error.get();
            return forPage(pageFactory.playersPage());
        } catch (IOException e) {
            return forInternalError(e, "Failed to generate players page");
        }
    }

    private Optional<Response> checkDbClosedError() {
        Database.State dbState = dbSystem.getDatabase().getState();
        if (dbState != Database.State.OPEN) {
            try {
                return Optional.of(buildDBNotOpenResponse(dbState));
            } catch (IOException e) {
                return Optional.of(forInternalError(e, "Database was not open, additionally failed to generate error page for that"));
            }
        }
        return Optional.empty();
    }

    private Response buildDBNotOpenResponse(Database.State dbState) throws IOException {
        return Response.builder()
                .setMimeType(MimeType.HTML)
                .setContent(pageFactory.errorPage(
                        "503 Resources Unavailable",
                        "Database is " + dbState.name() + " - Please try again later. You can check database status with /plan info"
                ).toHtml())
                .setStatus(503)
                .build();
    }

    public Response internalErrorResponse(Throwable e, String s) {
        return forInternalError(e, s);
    }

    public Response networkPageResponse() {
        Optional<Response> error = checkDbClosedError();
        if (error.isPresent()) return error.get();
        try {
            return forPage(pageFactory.networkPage());
        } catch (IOException e) {
            return forInternalError(e, "Failed to generate network page");
        }
    }

    public Response serverPageResponse(UUID serverUUID) {
        Optional<Response> error = checkDbClosedError();
        if (error.isPresent()) return error.get();
        try {
            return forPage(pageFactory.serverPage(serverUUID));
        } catch (NotFoundException e) {
            return notFound404(e.getMessage());
        } catch (IOException e) {
            return forInternalError(e, "Failed to generate server page");
        }
    }

    public Response rawPlayerPageResponse(UUID playerUUID) {
        PlayerContainer player = dbSystem.getDatabase().query(ContainerFetchQueries.fetchPlayerContainer(playerUUID));
        return Response.builder()
                .setMimeType(MimeType.JSON)
                .setJSONContent(player.mapToNormalMap())
                .build();
    }

    public Response javaScriptResponse(String fileName) {
        try {
            String content = locale.replaceLanguageInJavascript(files.getCustomizableResourceOrDefault(fileName).asString());
            return Response.builder()
                    .setMimeType(MimeType.JS)
                    .setContent(content)
                    .setStatus(200)
                    .build();
        } catch (IOException e) {
            return notFound404("JS File not found from jar: " + fileName + ", " + e.toString());
        }
    }

    public Response cssResponse(String fileName) {
        try {
            String content = theme.replaceThemeColors(files.getCustomizableResourceOrDefault(fileName).asString());
            return Response.builder()
                    .setMimeType(MimeType.CSS)
                    .setContent(content)
                    .setStatus(200)
                    .build();
        } catch (IOException e) {
            return notFound404("CSS File not found from jar: " + fileName + ", " + e.toString());
        }
    }

    public Response imageResponse(String fileName) {
        try {
            return Response.builder()
                    .setMimeType(MimeType.IMAGE)
                    .setContent(files.getCustomizableResourceOrDefault(fileName).asBytes())
                    .setStatus(200)
                    .build();
        } catch (IOException e) {
            return notFound404("Image File not found from jar: " + fileName + ", " + e.toString());
        }
    }

    public Response fontResponse(String fileName) {
        String type;
        if (fileName.endsWith(".woff")) {
            type = MimeType.FONT_WOFF;
        } else if (fileName.endsWith(".woff2")) {
            type = MimeType.FONT_WOFF2;
        } else if (fileName.endsWith(".eot")) {
            type = MimeType.FONT_EOT;
        } else if (fileName.endsWith(".ttf")) {
            type = MimeType.FONT_TTF;
        } else {
            type = MimeType.FONT_BYTESTREAM;
        }
        try {
            return Response.builder()
                    .setMimeType(type)
                    .setContent(files.getCustomizableResourceOrDefault(fileName).asBytes())
                    .build();
        } catch (IOException e) {
            return notFound404("Font File not found from jar: " + fileName + ", " + e.toString());
        }
    }

    public Response redirectResponse(String location) {
        return Response.builder().redirectTo(location).build();
    }

    public Response faviconResponse() {
        try {
            return Response.builder()
                    .setMimeType(MimeType.FAVICON)
                    .setContent(files.getCustomizableResourceOrDefault("web/favicon.ico").asBytes())
                    .build();
        } catch (IOException e) {
            return forInternalError(e, "Could not read favicon");
        }
    }

    public Response pageNotFound404() {
        return notFound404(locale.getString(ErrorPageLang.UNKNOWN_PAGE_404));
    }

    public Response uuidNotFound404() {
        return notFound404(locale.getString(ErrorPageLang.UUID_404));
    }

    public Response playerNotFound404() {
        return notFound404(locale.getString(ErrorPageLang.NOT_PLAYED_404));
    }

    public Response notFound404(String message) {
        try {
            return Response.builder()
                    .setMimeType(MimeType.HTML)
                    .setContent(pageFactory.errorPage(Icon.called("map-signs").build(), "404 " + message, message).toHtml())
                    .setStatus(404)
                    .build();
        } catch (IOException e) {
            return forInternalError(e, "Failed to generate 404 page with message '" + message + "'");
        }
    }

    public Response basicAuthFail(WebUserAuthException e) {
        try {
            FailReason failReason = e.getFailReason();
            String reason = failReason.getReason();
            if (failReason == FailReason.ERROR) {
                StringBuilder errorBuilder = new StringBuilder("</p><pre>");
                for (String line : getStackTrace(e.getCause())) {
                    errorBuilder.append(line);
                }
                errorBuilder.append("</pre>");

                reason += errorBuilder.toString();
            }
            return Response.builder()
                    .setMimeType(MimeType.HTML)
                    .setContent(pageFactory.errorPage(Icon.called("lock").build(), "401 Unauthorized", "Authentication Failed.</p><p><b>Reason: " + reason + "</b></p><p>").toHtml())
                    .setStatus(401)
                    .setHeader("WWW-Authenticate", "Basic realm=\"" + failReason.getReason() + "\"")
                    .build();
        } catch (IOException jarReadFailed) {
            return forInternalError(e, "Failed to generate PromptAuthorizationResponse");
        }
    }

    private List<String> getStackTrace(Throwable throwable) {
        List<String> stackTrace = new ArrayList<>();
        stackTrace.add(throwable.toString());
        for (StackTraceElement element : throwable.getStackTrace()) {
            stackTrace.add("    " + element.toString());
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            List<String> causeTrace = getStackTrace(cause);
            if (!causeTrace.isEmpty()) {
                causeTrace.set(0, "Caused by: " + causeTrace.get(0));
                stackTrace.addAll(causeTrace);
            }
        }

        return stackTrace;
    }

    public Response forbidden403() {
        return forbidden403("Your user is not authorized to view this page.<br>"
                + "If you believe this is an error contact staff to change your access level.");
    }

    public Response forbidden403(String message) {
        try {
            return Response.builder()
                    .setMimeType(MimeType.HTML)
                    .setContent(pageFactory.errorPage(Icon.called("hand-paper").of(Family.REGULAR).build(), "403 Forbidden", message).toHtml())
                    .setStatus(403)
                    .build();
        } catch (IOException e) {
            return forInternalError(e, "Failed to generate 403 page");
        }
    }

    public Response basicAuth() {
        try {
            String tips = "<br>- Ensure you have registered a user with <b>/plan register</b><br>"
                    + "- Check that the username and password are correct<br>"
                    + "- Username and password are case-sensitive<br>"
                    + "<br>If you have forgotten your password, ask a staff member to delete your old user and re-register.";
            return Response.builder()
                    .setMimeType(MimeType.HTML)
                    .setContent(pageFactory.errorPage(Icon.called("lock").build(), "401 Unauthorized", "Authentication Failed." + tips).toHtml())
                    .setStatus(401)
                    .setHeader("WWW-Authenticate", "Basic realm=\"Plan WebUser (/plan register)\"")
                    .build();
        } catch (IOException e) {
            return forInternalError(e, "Failed to generate PromptAuthorizationResponse");
        }
    }

    public Response badRequest(String errorMessage, String target) {
        return Response.builder()
                .setMimeType(MimeType.HTML)
                .setContent("400 Bad Request: " + errorMessage + " (when requesting '" + target + "')")
                .setStatus(400)
                .build();
    }

    public Response playerPageResponse(UUID playerUUID) {
        try {
            return forPage(pageFactory.playerPage(playerUUID));
        } catch (IllegalStateException e) {
            return playerNotFound404();
        } catch (IOException e) {
            return forInternalError(e, "Failed to generate player page");
        }
    }
}
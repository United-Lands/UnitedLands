package org.unitedlands.unitedlands;

import java.io.File;
import java.util.ArrayList;

import org.unitedlands.classes.ConfigFile;
import org.unitedlands.unitedlands.classes.webservices.ApiRouter;
import org.unitedlands.unitedlands.classes.webservices.handlers.AuthFinalizeHandler;
import org.unitedlands.unitedlands.classes.webservices.handlers.AuthStartHandler;
import org.unitedlands.unitedlands.classes.webservices.handlers.AuthStatusHandler;
import org.unitedlands.unitedlands.classes.webservices.handlers.CitizenGetHandler;
import org.unitedlands.unitedlands.classes.webservices.handlers.CountriesGetHandler;
import org.unitedlands.unitedlands.classes.webservices.handlers.CountriesListHandler;
import org.unitedlands.unitedlands.classes.webservices.handlers.SettlementsGetHandler;
import org.unitedlands.unitedlands.classes.webservices.handlers.SettlementsListHandler;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.HttpString;

public class UnitedLandsWebServices {

    private ConfigFile config;
    private Undertow server;

    public UnitedLandsWebServices() {
        config = new ConfigFile(UnitedLands.instance(), "webservices.yml");

        cleanChallenges();
        startWebServices();
    }

    public void reloadConfig() {
        config.reload();
    }

    private void cleanChallenges() {
        var challengesFuture = UnitedLandsDataManager.instance().getDatabaseManager().getLoginChallengeService()
                .getAllAsync();
        try {
            var challenges = new ArrayList<>(challengesFuture.get());
            for (var challenge : challenges) {
                if (System.currentTimeMillis() > challenge.getExpiresAt())
                    UnitedLandsDataManager.instance().getDatabaseManager().getLoginChallengeService().deleteAsync(challenge);
            }

        } catch (Exception ex) {
            United.logger().error("Error cleaning login challenges: " + ex.getMessage());
        }

    }

    public void startWebServices() {

        var host = config.get().getString("host");
        var port = config.get().getInt("port", 8090);

        File webDir = new File(UnitedLands.instance().getDataFolder(), "web");
        if (!webDir.exists()) {
            webDir.mkdirs();
            UnitedLands.instance().saveResource("web/index.html", false);
        }

        try {
            server = buildServer(host, port, webDir);
            server.start();
            United.logger().info("Web server started on " + host + ":" + port, "UnitedLands");
        } catch (Exception e) {
            United.logger().error("Failed to start web server: " + e.getMessage(), "UnitedLands");
        }
    }

    private Undertow buildServer(String host, int port, File webDir) {

        var secret = config.get().getString("signing-key");
        if (secret == null)
            throw new IllegalStateException("No signing key found, cannot start web services.");

        HttpHandler apiRouter = new ApiRouter()
                .get("/auth/status/{id}", new AuthStatusHandler())
                .get("/auth/start", new AuthStartHandler())
                .post("/auth/finalize", new AuthFinalizeHandler(secret))
                .get("/citizens/{id}", new CitizenGetHandler(UnitedLands.instance()))
                .get("/countries/{slug}", new CountriesGetHandler(UnitedLands.instance()))
                .get("/settlements/{slug}", new SettlementsGetHandler(UnitedLands.instance()))
                .get("/countries", new CountriesListHandler(UnitedLands.instance()))
                .get("/settlements", new SettlementsListHandler(UnitedLands.instance()));
                
        HttpHandler resourceHandler = new ResourceHandler(
                new FileResourceManager(webDir, 1024))
                .setWelcomeFiles("index.html")
                .setDirectoryListingEnabled(false);

        HttpHandler root = Handlers.path()
                .addPrefixPath("/api", apiRouter)
                .addPrefixPath("/", resourceHandler);

        HttpHandler rootWithCors = addCorsHeaders(root);

        return Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(rootWithCors)
                .build();
    }

    private HttpHandler addCorsHeaders(HttpHandler next) {

        var origins = config.get().getStringList("cors-origins");

        return exchange -> {

            String requestOrigin = exchange.getRequestHeaders().getFirst(HttpString.tryFromString("Origin"));

            if (requestOrigin != null && origins.contains(requestOrigin)) {
                exchange.getResponseHeaders()
                        .put(HttpString.tryFromString("Access-Control-Allow-Origin"), requestOrigin);
                exchange.getResponseHeaders()
                        .put(HttpString.tryFromString("Access-Control-Allow-Methods"),
                                "GET, POST, PUT, DELETE, OPTIONS");
                exchange.getResponseHeaders()
                        .put(HttpString.tryFromString("Access-Control-Allow-Headers"),
                                "Authorization, Content-Type");
                exchange.getResponseHeaders()
                        .put(HttpString.tryFromString("Access-Control-Allow-Credentials"),
                                "true");
                exchange.getResponseHeaders()
                        .put(HttpString.tryFromString("Access-Control-Max-Age"), "3600");
            }

            if (exchange.getRequestMethod().equalToString("OPTIONS")) {
                exchange.setStatusCode(200);
                exchange.endExchange();
                return;
            }

            next.handleRequest(exchange);
        };
    }

    public void stopWebServices() {
        server.stop();
    }

}

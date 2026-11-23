package org.unitedlands.unitedlands.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import javax.imageio.ImageIO;

import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.utils.United;

public class IconUtils {

    private static final int REQUIRED_WIDTH = 32;
    private static final int REQUIRED_HEIGHT = 32;
    private static final int MAX_BYTES = 256 * 1024;
    private static final int MAX_REDIRECTS = 3;
    private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
    private static final int IHDR_TYPE = 0x49484452;

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private IconUtils() {
    }

    public static URI parseUri(String url) throws IconException {
        final URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            throw new IconException(United.messenger().get("player.country.seticon.invalid-url"));
        }
        checkUri(uri);
        return uri;
    }

    public static void downloadAndSave(URI uri, String fileName)
            throws IOException, InterruptedException, IconException {
        byte[] data = download(uri);
        validatePng(data);
        save(fileName, data);
    }

    private static byte[] download(URI uri) throws IOException, InterruptedException, IconException {
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            checkUri(uri);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", UnitedLands.instance().getName() + "/" + UnitedLands.instance().getPluginMeta().getVersion())
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream body = response.body()) {
                int status = response.statusCode();

                if (status >= 300 && status < 400) {
                    String location = response.headers().firstValue("Location")
                            .orElseThrow(() -> new IconException(United.messenger().get("player.country.seticon.no-location-header")));
                    uri = uri.resolve(location);
                    continue;
                }
                if (status != 200) {
                    throw new IconException(United.messenger().get("player.country.seticon.wrong-status", status));
                }

                long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (declared > MAX_BYTES) {
                    throw new IconException(United.messenger().get("player.country.seticon.file-too-large"));
                }

                byte[] data = body.readNBytes(MAX_BYTES + 1);
                if (data.length > MAX_BYTES) {
                    throw new IconException(United.messenger().get("player.country.seticon.file-too-large"));
                }
                return data;
            }
        }
        throw new IconException(United.messenger().get("player.country.seticon.too-many-redirects"));
    }

    private static void checkUri(URI uri) throws IconException {
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IconException(United.messenger().get("player.country.seticon.only-http"));
        }
        if (uri.getHost() == null) {
            throw new IconException(United.messenger().get("player.country.seticon.no-host"));
        }
        try {
            for (InetAddress addr : InetAddress.getAllByName(uri.getHost())) {
                if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()
                        || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()
                        || addr.isMulticastAddress()) {
                    throw new IconException(United.messenger().get("player.country.seticon.local-not-allowed"));
                }
            }
        } catch (IOException e) {
            throw new IconException(United.messenger().get("player.country.seticon.unresolvable-host", uri.getHost()));
        }
    }

    private static void validatePng(byte[] data) throws IconException {

        if (data.length < 33 || !Arrays.equals(Arrays.copyOf(data, 8), PNG_SIGNATURE)) {
            throw new IconException(United.messenger().get("player.country.seticon.invalid-png"));
        }

        ByteBuffer buf = ByteBuffer.wrap(data);
        if (buf.getInt(12) != IHDR_TYPE) {
            throw new IconException(United.messenger().get("player.country.seticon.invalid-png"));
        }
        int width = buf.getInt(16);
        int height = buf.getInt(20);
        if (width != REQUIRED_WIDTH || height != REQUIRED_HEIGHT) {
            throw new IconException(United.messenger().get("player.country.seticon.wrong-size", REQUIRED_WIDTH, REQUIRED_HEIGHT));
        }

        try {
            if (ImageIO.read(new ByteArrayInputStream(data)) == null) {
                throw new IconException(United.messenger().get("player.country.seticon.png-read-error"));
            }
        } catch (IOException e) {
            throw new IconException(United.messenger().get("player.country.seticon.png-read-error"));
        }
    }

    private static void save(String fileName, byte[] data) throws IOException, IconException {

        Path iconDir = UnitedLands.instance().getDataFolder().toPath().resolve("icons");
        Files.createDirectories(iconDir);

        Path target = iconDir.resolve(fileName).normalize();
        if (!target.startsWith(iconDir)) {
            throw new IconException(United.messenger().get("player.country.seticon.invalid-filename"));
        }

        try {
            Files.write(target, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ex) {
            throw new IconException(United.messenger().get("player.country.seticon.write-error", ex.getMessage()));
        }
    }

    public static class IconException extends Exception {
        public IconException(String message) {
            super(message);
        }
    }

}

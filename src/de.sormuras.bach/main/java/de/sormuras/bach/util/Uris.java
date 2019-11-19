package de.sormuras.bach.util;

import de.sormuras.bach.Log;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/** Uniform Resource Identifier ({@link URI}) read and download support. */
public class Uris {

  public static Uris ofSystem() {
    var log = Log.ofSystem();
    var httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    return new Uris(log, httpClient);
  }

  private final Log log;
  private final HttpClient http;

  public Uris(Log log, HttpClient http) {
    this.log = log;
    this.http = http;
  }

  public HttpResponse<Void> head(URI uri, int timeout) throws Exception {
    var nobody = HttpRequest.BodyPublishers.noBody();
    var duration = Duration.ofSeconds(timeout);
    var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).timeout(duration).build();
    return http.send(request, HttpResponse.BodyHandlers.discarding());
  }

  /** Copy all content from a uri to a target file. */
  public Path copy(URI uri, Path path, CopyOption... options) throws Exception {
    log.debug("Copy %s to %s", uri, path);
    Files.createDirectories(path.getParent());
    if ("file".equals(uri.getScheme())) {
      try {
        return Files.copy(Path.of(uri), path, options);
      } catch (Exception e) {
        throw new IllegalArgumentException("copy file failed:" + uri, e);
      }
    }
    var request = HttpRequest.newBuilder(uri).GET();
    if (Files.exists(path) && Files.getFileStore(path).supportsFileAttributeView("user")) {
      try {
        var etagBytes = (byte[]) Files.getAttribute(path, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        request.setHeader("If-None-Match", etag);
      } catch (Exception e) {
        log.warning("Couldn't get 'user:etag' file attribute: %s", e);
      }
    }
    var handler = HttpResponse.BodyHandlers.ofFile(path);
    var response = http.send(request.build(), handler);
    if (response.statusCode() == 200) {
      if (Set.of(options).contains(StandardCopyOption.COPY_ATTRIBUTES)) {
        var etagHeader = response.headers().firstValue("etag");
        if (etagHeader.isPresent()) {
          if (Files.getFileStore(path).supportsFileAttributeView("user")) {
            try {
              var etag = etagHeader.get();
              Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
            } catch (Exception e) {
              log.warning("Couldn't set 'user:etag' file attribute: %s", e);
            }
          }
        } else {
          log.warning("No etag provided in response: %s", response);
        }
        var lastModifiedHeader = response.headers().firstValue("last-modified");
        if (lastModifiedHeader.isPresent()) {
          try {
            //noinspection SpellCheckingInspection
            var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
            var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
            Files.setLastModifiedTime(path, fileTime);
          } catch (Exception e) {
            log.warning("Couldn't set last modified file attribute: %s", e);
          }
        }
      }
      log.debug("%s <- %s", path, uri);
    }
    return path;
  }

  /** Read all content from a uri into a string. */
  public String read(URI uri) throws Exception {
    log.debug("Read %s", uri);
    if ("file".equals(uri.getScheme())) {
      return Files.readString(Path.of(uri));
    }
    var request = HttpRequest.newBuilder(uri).GET();
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
  }
}

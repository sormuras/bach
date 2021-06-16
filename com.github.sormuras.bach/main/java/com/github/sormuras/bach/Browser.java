package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Strings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class Browser {

  public static Browser of(Options options, Logbook logbook) {
    var timeout = Duration.ofSeconds(9);
    var policy = HttpClient.Redirect.NORMAL;
    var builder = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(policy);
    return new Browser(logbook, builder);
  }

  private final Logbook logbook;
  private final HttpClient.Builder builder;
  private final AtomicReference<HttpClient> atomicHttpClient = new AtomicReference<>();

  public Browser(Logbook logbook, HttpClient.Builder builder) {
    this.logbook = logbook;
    this.builder = builder;
  }

  private HttpClient http() {
    var oldClient = atomicHttpClient.get();
    if (oldClient != null) return oldClient;
    var client = builder.build();
    logbook.debug(
        "New HttpClient created with %s connect timeout and %s redirect policy"
            .formatted(
                client.connectTimeout().map(Strings::toString).orElse("no"),
                client.followRedirects()));
    return atomicHttpClient.compareAndSet(null, client) ? client : atomicHttpClient.get();
  }

  public void httpLoad(String uri, Path file) {
    logbook.debug("Load %s from %s".formatted(file, uri));
    var request = HttpRequest.newBuilder(URI.create(uri)).build();
    try {
      Files.createDirectories(file.getParent());
      http().send(request, HttpResponse.BodyHandlers.ofFile(file));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public void httpLoad(Map<String, Path> map) {
    logbook.debug("Load %d file%s".formatted(map.size(), map.size() == 1 ? "" : "s"));
    if (map.isEmpty()) return;
    var futures = new ArrayList<CompletableFuture<Path>>();
    for (var entry : map.entrySet()) {
      var uri = entry.getKey();
      var file = entry.getValue();
      logbook.debug("Load %s from %s".formatted(file, uri));
      try {
        Files.createDirectories(file.getParent());
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
      var request = HttpRequest.newBuilder(URI.create(uri)).build();
      var handler = HttpResponse.BodyHandlers.ofFile(file);
      futures.add(http().sendAsync(request, handler).thenApply(HttpResponse::body));
    }
    CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new)).join();
  }

  public String httpRead(String uri) {
    return httpRead(URI.create(uri));
  }

  public String httpRead(URI uri) {
    logbook.debug("Read %s".formatted(uri));
    var request = HttpRequest.newBuilder(uri).build();
    try {
      return http().send(request, HttpResponse.BodyHandlers.ofString()).body();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}

package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Folders;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public record Core(
    Logbook logbook, ModuleLayer layer, Options options, Factory factory, Folders folders) {

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

  private HttpClient http() {
    return factory.defaultHttpClient(this);
  }
}

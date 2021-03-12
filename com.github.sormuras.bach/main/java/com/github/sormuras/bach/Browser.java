package com.github.sormuras.bach;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** A http client helper. */
public class Browser {

  private final Bach bach;
  private final HttpClient client;

  public Browser(Bach bach) {
    this.bach = bach;
    this.client = bach.newHttpClient();
  }

  public void load(String uri, Path file) {
    bach.log("Load %s from %s", file, uri);
    var request = HttpRequest.newBuilder(URI.create(uri)).build();
    try {
      Files.createDirectories(file.getParent());
      client.send(request, HttpResponse.BodyHandlers.ofFile(file));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public void load(Map<String, Path> map) {
    bach.log("Load %d file%s", map.size(), map.size() == 1 ? "" : "s");
    if (map.isEmpty()) return;
    var futures = new ArrayList<CompletableFuture<Path>>();
    for (var entry : map.entrySet()) {
      var uri = entry.getKey();
      var file = entry.getValue();
      bach.log("Load %s from %s", file, uri);
      try {
        Files.createDirectories(file.getParent());
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
      var request = HttpRequest.newBuilder(URI.create(uri)).build();
      var handler = HttpResponse.BodyHandlers.ofFile(file);
      futures.add(client.sendAsync(request, handler).thenApply(HttpResponse::body));
    }
    CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new)).join();
  }

  public String read(String uri) {
    var request = HttpRequest.newBuilder(URI.create(uri)).build();
    try {
      bach.log("Read %s", uri);
      return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}

package com.github.sormuras.bach;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

public interface ExternalPropertiesStorage {

  GitHub DEFAULT = new GitHub("sormuras", "bach-info", "HEAD");

  static ExternalPropertiesStorage find(String storage) {
    var scanner = new Scanner(storage);
    scanner.useDelimiter("/");
    var host = scanner.next(); // "github.com"
    if (host.equals("github.com")) {
      var user = scanner.hasNext() ? scanner.next() : DEFAULT.user();
      var repo = scanner.hasNext() ? scanner.next() : DEFAULT.repo();
      var hash = scanner.hasNext() ? String.join("/", scanner.tokens().toList()) : DEFAULT.hash();
      return new GitHub(user, repo, hash);
    }
    return DEFAULT;
  }

  private static Map<String, List<String>> map(InputStream stream) throws Exception {
    var map = new TreeMap<String, List<String>>();
    map.put("external-modules", List.of());
    map.put("external-tools", List.of());
    try (var zip = new ZipInputStream(stream)) {
      while (true) {
        var entry = zip.getNextEntry();
        if (entry == null) break;
        var name = entry.getName();
        if (!name.endsWith(".properties")) continue;
        var last = name.lastIndexOf('/');
        var item = name.substring(last + 1, name.length() - 11);
        if (name.contains("/external-modules/")) {
          map.merge(
              "external-modules",
              List.of(item),
              (prev, next) -> Stream.concat(prev.stream(), next.stream()).toList());
        }
        if (name.contains("/external-tools/")) {
          map.merge(
              "external-tools",
              List.of(item),
              (prev, next) -> Stream.concat(prev.stream(), next.stream()).toList());
        }
        map.merge(
            "*",
            List.of(name),
            (prev, next) -> Stream.concat(prev.stream(), next.stream()).toList());
      }
    }
    return Map.copyOf(map);
  }

  String uri(String type, String name);

  default Map<String, List<String>> map() {
    return Map.of();
  }

  record GitHub(String user, String repo, String hash) implements ExternalPropertiesStorage {
    @Override
    public String uri(String type, String name) {
      return new StringJoiner("/", "https://github.com/", ".properties")
          .add(user)
          .add(repo)
          .add("raw")
          .add(hash)
          .add(".bach")
          .add(type)
          .add(name)
          .toString();
    }

    @Override
    public Map<String, List<String>> map() {
      var uri =
          new StringJoiner("/", "https://github.com/", ".zip")
              .add(user)
              .add(repo)
              .add("archive")
              .add(hash)
              .toString();
      try {
        var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        var request = HttpRequest.newBuilder(URI.create(uri)).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return ExternalPropertiesStorage.map(response.body());
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }
}

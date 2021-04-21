package com.github.sormuras.bach.internal;

import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.spi.ToolProvider;

public class CopyToolProvider implements ToolProvider {

  private static final boolean DEBUG =
      Boolean.getBoolean("ebug") || System.getProperty("ebug") != null;

  @Override
  public String name() {
    return "copy";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length < 1) {
      var usage =
          """
          Usage: copy URI [FILE]
             URI = source
            FILE = target (defaults to last element of the source URI)
          Examples:
          copy https://repo.maven.apache.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
          copy https://repo.maven.apache.org/[...]/junit-4.13.2.jar path/jars/junit.jar
          """;
      out.println(usage.stripTrailing());
      return 0;
    }
    var uri = URI.create(args[0]);
    var path = uri.getPath();
    var file = Path.of(args.length == 2 ? args[1] : path.substring(path.lastIndexOf('/') + 1));
    try {
      switch (uri.getScheme()) {
        case "file" -> copy(out, uri, file);
        case "http", "https" -> load(out, uri, file);
      }
      return 0;
    } catch (Exception e) {
      err.println(e);
      return 1;
    }
  }

  void copy(PrintWriter out, URI uri, Path file) throws Exception {
    if (DEBUG) out.printf("Copying %s from: %s%n", file, uri);
    var source = Path.of(uri);
    var directory = file.toAbsolutePath().getParent();
    if (directory != null) Files.createDirectories(directory);
    Files.copy(source, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    out.printf("<< %s (%d bytes, %s)%n", file.getFileName(), Files.size(file), directory);
  }

  void load(PrintWriter out, URI uri, Path file) throws Exception {
    if (DEBUG) out.printf("Downloading %s from: %s%n", file, uri);
    var directory = file.toAbsolutePath().getParent();
    var httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    var request = HttpRequest.newBuilder(uri).GET();
    if (Files.exists(file)) {
      try {
        var etagBytes = (byte[]) Files.getAttribute(file, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        if (DEBUG) out.println("Set 'If-None-Match' header to etag: " + etag);
        request.setHeader("If-None-Match", etag);
      } catch (UnsupportedOperationException | IllegalArgumentException e) {
        if (DEBUG) out.println("Couldn't get 'user:etag' file attribute: " + e);
      }
    } else {
      if (directory != null) Files.createDirectories(directory);
    }
    var handler = HttpResponse.BodyHandlers.ofFile(file);
    var response = httpClient.send(request.build(), handler);
    if (DEBUG) out.println(response);
    if (response.statusCode() == 200) {
      out.printf("<< %s (%d bytes, %s)%n", file.getFileName(), Files.size(file), directory);
      var etagHeader = response.headers().firstValue("etag");
      if (etagHeader.isPresent()) {
        try {
          var etag = etagHeader.get();
          if (DEBUG) out.println("Set 'user:etag' attribute: " + etag);
          Files.setAttribute(file, "user:etag", StandardCharsets.UTF_8.encode(etag));
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
          if (DEBUG) out.println("Couldn't set 'user:etag' file attribute: " + e);
        }
      }
      var lastModifiedHeader = response.headers().firstValue("last-modified");
      if (lastModifiedHeader.isPresent()) {
        var source = lastModifiedHeader.get();
        if (DEBUG) out.println("Header last-modified: " + source);
        var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        var millis = format.parse(source).getTime(); // 0 means "unknown"
        var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
        if (DEBUG) out.println("Set last modified time: " + fileTime);
        Files.setLastModifiedTime(file, fileTime);
      }
    }
    if (response.statusCode() == 304) out.println("Not modified: " + file.toUri());
  }
}

/*
 * Get.java - URL getter using Java 11's HttpClient
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// default package

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Get {
  public static void main(String[] args) throws Exception {
    var out = System.out;
    if (args.length < 1) {
      out.println("Usage: java Get.java URI [FILE]");
      out.println("       URI = source");
      out.println("      FILE = target, defaults to last element of URI");
      out.println("Examples:");
      out.println("java Get.java https://repo1.maven.org/maven2/junit/junit/3.7/junit-3.7.jar");
      out.println("java Get.java https://repo1.maven.org/[...]/junit-3.7.jar junit.zip");
      return;
    }
    var debug = Boolean.getBoolean("ebug");
    var uri = URI.create(args[0]);
    var path = uri.getPath();
    var file = Path.of(args.length == 2 ? args[1] : path.substring(path.lastIndexOf('/') + 1));
    if (debug) out.printf("Get %s from: %s %n", file, uri);

    var httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    var request = HttpRequest.newBuilder(uri).GET();
    if (Files.exists(file)) {
      try {
        var etagBytes = (byte[]) Files.getAttribute(file, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        if (debug) out.println("Set 'If-None-Match' header to etag: " + etag);
        request.setHeader("If-None-Match", etag);
      } catch (UnsupportedOperationException | IllegalArgumentException e) {
        if (debug) out.println("Couldn't get 'user:etag' file attribute: " + e);
      }
    }
    var handler = HttpResponse.BodyHandlers.ofFile(file);
    var response = httpClient.send(request.build(), handler);
    if (debug) out.println(response);
    if (response.statusCode() == 200) {
      out.printf("Loaded: %s (%d bytes)%n", file.toUri(), Files.size(file));
      var etagHeader = response.headers().firstValue("etag");
      if (etagHeader.isPresent()) {
        try {
          var etag = etagHeader.get();
          if (debug) out.println("Set 'user:etag' attribute: " + etag);
          Files.setAttribute(file, "user:etag", StandardCharsets.UTF_8.encode(etag));
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
          if (debug) out.println("Couldn't set 'user:etag' file attribute: " + e);
        }
      }
      var lastModifiedHeader = response.headers().firstValue("last-modified");
      if (lastModifiedHeader.isPresent()) {
        var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
        var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
        if (debug) out.println("Set last modified time: " + fileTime);
        Files.setLastModifiedTime(file, fileTime);
      }
    }
    if (response.statusCode() == 304) out.println("Not modified: " + file.toUri());
  }
}

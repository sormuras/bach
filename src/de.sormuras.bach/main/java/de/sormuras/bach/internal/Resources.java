/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

package de.sormuras.bach.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/** Uniform Resource Identifier ({@link URI}) head, read, and copy support. */
public class Resources {

  private final HttpClient client;

  public Resources(HttpClient client) {
    this.client = client;
  }

  /** Request head-only from the specified uri. */
  public HttpResponse<Void> head(URI uri, int timeout) throws Exception {
    var nobody = HttpRequest.BodyPublishers.noBody();
    var duration = Duration.ofSeconds(timeout);
    var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).timeout(duration).build();
    return client.send(request, BodyHandlers.discarding());
  }

  /** Copy all content and attributes from a uri to a target file. */
  public Path copy(URI uri, Path file) throws Exception {
    return copy(uri, file, StandardCopyOption.COPY_ATTRIBUTES);
  }

  /** Copy all content from a uri to a target file. */
  public Path copy(URI uri, Path file, CopyOption... options) throws Exception {
    var request = HttpRequest.newBuilder(uri).GET();
    if (Files.exists(file) && Paths.isViewSupported(file, "user")) {
      var etagBytes = (byte[]) Files.getAttribute(file, "user:etag");
      var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
      request.setHeader("If-None-Match", etag);
    }
    var directory = file.getParent();
    if (directory != null) Files.createDirectories(directory);
    var handler = BodyHandlers.ofFile(file);
    var response = client.send(request.build(), handler);
    if (response.statusCode() == 200) {
      if (Set.of(options).contains(StandardCopyOption.COPY_ATTRIBUTES)) {
        var etagHeader = response.headers().firstValue("etag");
        if (etagHeader.isPresent() && Paths.isViewSupported(file, "user")) {
          var etag = StandardCharsets.UTF_8.encode(etagHeader.get());
          Files.setAttribute(file, "user:etag", etag);
        }
        var lastModifiedHeader = response.headers().firstValue("last-modified");
        if (lastModifiedHeader.isPresent()) {
          var text = lastModifiedHeader.get(); // force " GMT" suffix
          if (!text.endsWith(" GMT")) text = text.substring(0, text.lastIndexOf(' ')) + " GMT";
          var time = ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME);
          Files.setLastModifiedTime(file, FileTime.from(Instant.from(time)));
        }
      }
      return file;
    }
    if (response.statusCode() == 304 /*Not Modified*/) return file;
    Files.deleteIfExists(file);
    throw new IllegalStateException("Copy " + uri + " failed: response=" + response);
  }

  /** Read all content from a uri into a string. */
  public String read(URI uri) throws Exception {
    var request = HttpRequest.newBuilder(uri).GET();
    return client.send(request.build(), BodyHandlers.ofString()).body();
  }
}

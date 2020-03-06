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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/** Uniform Resource Identifier ({@link URI}) head, read, and copy support. */
public /*static*/ class Resources {

  private final Logger logger;
  private final HttpClient client;

  Resources(Logger logger, HttpClient client) {
    this.logger = logger != null ? logger : System.getLogger(getClass().getName());
    this.client = client;
  }

  /** Request head-only from the specified uri. */
  public HttpResponse<Void> head(URI uri, int timeout) throws Exception {
    var nobody = HttpRequest.BodyPublishers.noBody();
    var duration = Duration.ofSeconds(timeout);
    var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).timeout(duration).build();
    return client.send(request, BodyHandlers.discarding());
  }

  /** Copy all content from a uri to a target file. */
  public Path copy(URI uri, Path file, CopyOption... options) throws Exception {
    logger.log(Level.DEBUG, "Copy {0} to {1}", uri, file);
    Files.createDirectories(file.getParent());
    if ("file".equals(uri.getScheme())) {
      try {
        return Files.copy(Path.of(uri), file, options);
      } catch (Exception e) {
        throw new IllegalArgumentException("copy file failed:" + uri, e);
      }
    }
    var request = HttpRequest.newBuilder(uri).GET();
    if (Files.exists(file) && Files.getFileStore(file).supportsFileAttributeView("user")) {
      try {
        var etagBytes = (byte[]) Files.getAttribute(file, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        request.setHeader("If-None-Match", etag);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Couldn't get 'user:etag' file attribute: {0}", e);
      }
    }
    var handler = BodyHandlers.ofFile(file);
    var response = client.send(request.build(), handler);
    if (response.statusCode() == 200) {
      if (Set.of(options).contains(StandardCopyOption.COPY_ATTRIBUTES)) {
        var etagHeader = response.headers().firstValue("etag");
        if (etagHeader.isPresent()) {
          if (Files.getFileStore(file).supportsFileAttributeView("user")) {
            try {
              var etag = etagHeader.get();
              Files.setAttribute(file, "user:etag", StandardCharsets.UTF_8.encode(etag));
            } catch (Exception e) {
              logger.log(Level.WARNING, "Couldn't set 'user:etag' file attribute: {0}", e);
            }
          }
        } else logger.log(Level.WARNING, "No etag provided in response: {0}", response);
        var lastModifiedHeader = response.headers().firstValue("last-modified");
        if (lastModifiedHeader.isPresent()) {
          try {
            @SuppressWarnings("SpellCheckingInspection")
            var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            var current = System.currentTimeMillis();
            var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
            var fileTime = FileTime.fromMillis(millis == 0 ? current : millis);
            Files.setLastModifiedTime(file, fileTime);
          } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't set last modified file attribute: {0}", e);
          }
        }
      }
      logger.log(Level.DEBUG, "{0} <- {1}", file, uri);
      return file;
    }
    if (response.statusCode() == 304 /*Not Modified*/) return file;
    throw new RuntimeException("response=" + response);
  }

  /** Read all content from a uri into a string. */
  public String read(URI uri) throws Exception {
    logger.log(Level.DEBUG, "Read {0}", uri);
    if ("file".equals(uri.getScheme())) return Files.readString(Path.of(uri));
    var request = HttpRequest.newBuilder(uri).GET();
    return client.send(request.build(), BodyHandlers.ofString()).body();
  }
}

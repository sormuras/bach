/*
 * Bach - Java Shell Builder
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

package de.sormuras.bach;

import java.io.IOException;
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
import java.util.Locale;
import java.util.Set;

/*BODY*/
/** Uniform Resource Identifier ({@link java.net.URI}) read and download support. */
public /*STATIC*/ class Resources {

  public static Resources ofSystem() {
    var log = Log.ofSystem();
    var httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    return new Resources(log, httpClient);
  }

  private final Log log;
  private final HttpClient http;

  public Resources(Log log, HttpClient http) {
    this.log = log;
    this.http = http;
  }

  public HttpResponse<Void> head(URI uri) throws IOException, InterruptedException {
    var nobody = HttpRequest.BodyPublishers.noBody();
    var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).build();
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
    if (Files.exists(path)) {
      try {
        var etagBytes = (byte[]) Files.getAttribute(path, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        request.setHeader("If-None-Match", etag);
      } catch (Exception e) {
        log.warn("Couldn't get 'user:etag' file attribute: %s", e);
      }
    }
    var handler = HttpResponse.BodyHandlers.ofFile(path);
    var response = http.send(request.build(), handler);
    if (response.statusCode() == 200) {
      if (Set.of(options).contains(StandardCopyOption.COPY_ATTRIBUTES)) {
        var etagHeader = response.headers().firstValue("etag");
        if (etagHeader.isPresent()) {
          try {
            var etag = etagHeader.get();
            Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
          } catch (Exception e) {
            log.warn("Couldn't set 'user:etag' file attribute: %s", e);
          }
        }
        var lastModifiedHeader = response.headers().firstValue("last-modified");
        if (lastModifiedHeader.isPresent()) {
          try {
            var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
            var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
            Files.setLastModifiedTime(path, fileTime);
          } catch (Exception e) {
            log.warn("Couldn't set last modified file attribute: %s", e);
          }
        }
      }
      log.debug("%s <- %s", path, uri);
    }
    return path;
  }

  /** Read all content from a uri into a string. */
  public String read(URI uri) throws IOException, InterruptedException {
    log.debug("Read %s", uri);
    if ("file".equals(uri.getScheme())) {
      return Files.readString(Path.of(uri));
    }
    var request = HttpRequest.newBuilder(uri).GET();
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
  }
}

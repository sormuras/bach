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
import java.io.PrintWriter;
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
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

/*BODY*/
/** File transfer. */
/*STATIC*/ class Transfer {

  static class Item {

    static Item of(URI uri, String file) {
      return new Item(uri, file);
    }

    private final URI uri;
    private final String file;

    private Item(URI uri, String file) {
      this.uri = uri;
      this.file = file;
    }
  }

  private final PrintWriter out, err;
  private final HttpClient client;

  Transfer(PrintWriter out, PrintWriter err) {
    this(out, err, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
  }

  private Transfer(PrintWriter out, PrintWriter err, HttpClient client) {
    this.out = out;
    this.err = err;
    this.client = client;
  }

  void getFiles(Path path, Collection<Item> items) {
    Util.treeCreate(path);
    items.stream()
        .parallel()
        .map(item -> getFile(item.uri, path.resolve(item.file)))
        .collect(Collectors.toSet());
  }

  Path getFile(URI uri, Path path) {
    var request = HttpRequest.newBuilder(uri).GET();
    if (Files.exists(path)) {
      try {
        var etagBytes = (byte[]) Files.getAttribute(path, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        request.setHeader("If-None-Match", etag);
      } catch (Exception e) {
        err.println("Couldn't get 'user:etag' file attribute: " + e);
      }
    }
    try {
      var handler = HttpResponse.BodyHandlers.ofFile(path);
      var response = client.send(request.build(), handler);
      if (response.statusCode() == 200) {
        var etagHeader = response.headers().firstValue("etag");
        if (etagHeader.isPresent()) {
          try {
            var etag = etagHeader.get();
            Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
          } catch (Exception e) {
            err.println("Couldn't set 'user:etag' file attribute: " + e);
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
            err.println("Couldn't set last modified file attribute: " + e);
          }
        }
        synchronized (out) {
          out.println(path + " <- " + uri);
        }
      }
    } catch (IOException | InterruptedException e) {
      err.println("Failed to load: " + uri + " -> " + e);
      e.printStackTrace(err);
    }
    return path;
  }
}

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

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.TRACE;

import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/*BODY*/
/** Downloader. */
public /*STATIC*/ class Downloader {

  /** Extract last path element from the supplied uri. */
  static String extractFileName(URI uri) {
    var path = uri.getPath(); // strip query and fragment elements
    return path.substring(path.lastIndexOf('/') + 1);
  }

  /** Extract target file name either from 'Content-Disposition' header or. */
  static String extractFileName(URLConnection connection) throws Exception {
    var contentDisposition = connection.getHeaderField("Content-Disposition");
    if (contentDisposition != null && contentDisposition.indexOf('=') > 0) {
      return contentDisposition.split("=")[1].replaceAll("\"", "");
    }
    return extractFileName(connection.getURL().toURI());
  }

  final Run run;
  final Path destination;

  Downloader(Run run, Path destination) {
    this.run = run;
    this.destination = destination;
  }

  /** Download a file denoted by the specified uri. */
  Path download(URI uri) throws Exception {
    run.log(TRACE, "download(%s)", uri);
    var fileName = extractFileName(uri);
    var target = Files.createDirectories(destination).resolve(fileName);
    var url = uri.toURL(); // fails for non-absolute uri
    if (Boolean.getBoolean("offline")) { // TODO run.offline
      if (Files.exists(target)) {
        return target;
      }
      throw new IllegalStateException("Target is missing and being offline: " + target);
    }
    return download(url.openConnection());
  }

  /** Download a file using the given URL connection. */
  Path download(URLConnection connection) throws Exception {
    var millis = connection.getLastModified(); // 0 means "unknown"
    var lastModified = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
    run.log(TRACE, "Remote was modified on %s", lastModified);
    var target = destination.resolve(extractFileName(connection));
    run.log(TRACE, "Local target file is %s", target.toUri());
    var file = target.getFileName().toString();
    if (Files.exists(target)) {
      var fileModified = Files.getLastModifiedTime(target);
      run.log(TRACE, "Local last modified on %s", fileModified);
      if (fileModified.equals(lastModified)) {
        run.log(TRACE, "Timestamp match: %s, %d bytes.", file, Files.size(target));
        return target;
      }
      run.log(DEBUG, "Local target file differs from remote source -- replacing it...");
    }
    try (var sourceStream = connection.getInputStream()) {
      try (var targetStream = Files.newOutputStream(target)) {
        run.log(DEBUG, "Transferring %s...", file);
        sourceStream.transferTo(targetStream);
      }
      Files.setLastModifiedTime(target, lastModified);
    }
    run.log(DEBUG, "Downloaded %s, %d bytes.", file, Files.size(target));
    return target;
  }
}

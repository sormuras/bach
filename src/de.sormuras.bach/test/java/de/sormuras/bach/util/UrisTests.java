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

package de.sormuras.bach.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

class UrisTests {
  @Test
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  void headRequestOfHttpsGoogleComUsingSystemUrisIsRespondedWithStatus200() throws Exception {
    assertEquals(200, Uris.ofSystem().head(URI.create("https://google.com"), 9).statusCode());
  }

  @Test
  void copyFilesUsingFileScheme(@TempDir Path temp, TestReporter reporter) throws Exception {
    var source = Files.createDirectories(temp.resolve("source"));
    var sa = Files.writeString(source.resolve("a.txt"), "a");
    var sb = Files.writeString(source.resolve("b.txt"), "b");
    var sc = Files.writeString(source.resolve("c.txt"), "c");

    var target = Files.createDirectories(temp.resolve("target"));
    var ta = target.resolve("a.txt");
    var tb = target.resolve("b.txt");
    var tc = target.resolve("c.txt");

    var log = new Log();
    var uris = new Uris(log, HttpClient.newBuilder().build());
    uris.copy(sa.toUri(), ta);
    uris.copy(sb.toUri(), tb);
    uris.copy(sc.toUri(), tc);

    if (!log.errors().isEmpty()) {
      reporter.publishEntry("errors", String.join(System.lineSeparator(), log.errors()));
    }
    assertEquals(Files.readString(sa), uris.read(ta.toUri()));
    assertEquals(Files.readString(sb), uris.read(tb.toUri()));
    assertEquals(Files.readString(sc), uris.read(tc.toUri()));
  }
}

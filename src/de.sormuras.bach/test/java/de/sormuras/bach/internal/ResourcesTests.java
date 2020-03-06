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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import test.base.Log;

class ResourcesTests {

  final HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();

  @Test
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  void requestHeadOfHttpsGoogleComUsingSystemUrisIsRespondedWithStatus200() throws Exception {
    var log = new Log();
    var resources = new Resources(log, client);
    assertEquals(200, resources.head(URI.create("https://google.com"), 9).statusCode());
    log.assertThatEverythingIsFine();
    assertLinesMatch(List.of(), log.lines());
  }

  @Test
  void copyFilesUsingFileScheme(@TempDir Path temp) throws Exception {
    var source = Files.createDirectories(temp.resolve("source"));
    var sa = Files.writeString(source.resolve("a.txt"), "a");
    var sb = Files.writeString(source.resolve("b.txt"), "b");
    var sc = Files.writeString(source.resolve("c.txt"), "c");
    var target = Files.createDirectories(temp.resolve("target"));
    var ta = target.resolve("a.txt");
    var tb = target.resolve("b.txt");
    var tc = target.resolve("c.txt");
    var log = new Log();
    var resources = new Resources(log, client);
    resources.copy(sa.toUri(), ta);
    resources.copy(sb.toUri(), tb);
    resources.copy(sc.toUri(), tc);
    assertEquals(Files.readString(sa), resources.read(ta.toUri()));
    assertEquals(Files.readString(sb), resources.read(tb.toUri()));
    assertEquals(Files.readString(sc), resources.read(tc.toUri()));
    log.assertThatEverythingIsFine();
    assertLinesMatch(
        List.of(
            "L Copy .+source/a.txt to \\Q" + ta,
            "L Copy .+source/b.txt to \\Q" + tb,
            "L Copy .+source/c.txt to \\Q" + tc,
            "L Read .+target/a.txt",
            "L Read .+target/b.txt",
            "L Read .+target/c.txt"),
        log.lines());
  }
}

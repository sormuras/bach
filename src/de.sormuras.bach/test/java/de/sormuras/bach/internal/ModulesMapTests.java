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
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

class ModulesMapTests {

  @TestFactory
  Stream<DynamicNode> mapValueIsValidUri() {
    var resources = new Resources(HttpClient.newHttpClient());
    return new ModulesMap()
        .entrySet().stream().map(e -> checkUri(resources, e.getKey(), e.getValue()));
  }

  private DynamicContainer checkUri(Resources resources, String module, String uri) {
    return DynamicContainer.dynamicContainer(
        module,
        List.of(
            dynamicTest("syntax check", () -> URI.create(uri)),
            dynamicTest("head request", () -> checkHead(resources, uri))));
  }

  private static void checkHead(Resources resources, String uri) {
    try {
      var status = resources.head(URI.create(uri), 1).statusCode();
      assertEquals(200, status, "Not status 200: " + uri);
    } catch (Exception exception) {
      fail("HEAD request failed for: " + uri, exception);
    }
  }
}

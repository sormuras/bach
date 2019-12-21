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

package test.base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

public class Strings {

  private Strings() {}

  public static List<String> lines(StringWriter writer) {
    return List.copyOf(writer.toString().lines().collect(Collectors.toList()));
  }

  public static List<String> lines(InputStream stream) {
    try (var reader = new BufferedReader(new InputStreamReader(stream))) {
      return reader.lines().collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException("Reading from stream failed!", e);
    }
  }
}

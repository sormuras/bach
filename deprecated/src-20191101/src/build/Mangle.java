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

// default package

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Mangle a file into a Base64-encoded Java {@code String} constant. */
public class Mangle {
  public static void main(String... args) throws Exception {
    if (args.length < 1) throw new Error("Usage: java Mangle.java FILE");
    var path = Path.of(args[0]);
    var input = Files.readString(path);
    // System.out.println(input);
    System.out.printf("%d chars with hash: 0x%H%n", input.length(), input.hashCode());

    var bytes = new ByteArrayOutputStream();
    try (var zos = new GZIPOutputStream(bytes)) {
      zos.write(input.getBytes());
    }
    var base64 = Base64.getEncoder().encodeToString(bytes.toByteArray());
    // System.out.println("....o....|".repeat(10));
    // System.out.println(base64);

    var indent = "  ";
    var indent3 = indent.repeat(1 + 2);
    var indent5 = indent.repeat(1 + 4);
    var code = new ArrayList<String>();
    code.add(indent + "static String " + "TEMPLATE" + " =");
    code.add(indent3 + '"' + base64.substring(0, 91) + '"');
    Arrays.stream(base64.substring(91).split("(?<=\\G.{85})"))
        .forEach(line -> code.add(indent5 + "+ \"" + line + '"'));
    var last = code.size() - 1;
    code.set(last, code.get(last) + ';');
    System.out.println("....o....|".repeat(10));
    code.forEach(System.out::println);
    System.out.println("....o....|".repeat(10));

    var lines = new ArrayList<String>();
    try (var zip =
        new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(base64)))) {
      var bf = new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8));
      String line;
      while ((line = bf.readLine()) != null) {
        lines.add(line);
      }
      lines.add("");
    }
    var result = String.join(System.lineSeparator(), lines);
    // System.out.println(result);
    System.out.printf("%d chars with hash: 0x%H%n", result.length(), result.hashCode());
  }
}

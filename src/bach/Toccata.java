/*
 * Bach - Toccata
 *
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/** Generate Java program building a modular Java project. */
public class Toccata {

  static final String VERSION = "1-ea";

  static String TEMPLATE =
      "H4sIAAAAAAAAAG2SQXObMBCF757xf9jhZGdskebW5kRst2Wa4owhyeQowwJqsUSlxcTj8X/vAo4nmZbhIu3T07dP61+"
          + "NR3AFdzItYQ6JSVNJstvqtxemPlhVlASTxRRurj99hkVplSMlNcSESl+k9ypF7TCDRmdogUqEoGZXfKvM4Amt"
          + "U0bDjbiGSSfwziVvett7HEwDO3kAbQgah2yiHOSqQsDXFGsCpSE1u7ri61OEVlHZX3S2Eb3Jy9nEbEmyXvKJm"
          + "lf5eyVIuoB3X0lUuy++37atkD21MLbwq0Ht/PtwsYri1ZzJL+cedYXOgcU/jbLc9/YAsma0VG4ZuJItGAuysM"
          + "g1Mh16axUpXczAmZxaabH3yThOq7YNfcjuDZQDeC/g9Dh5L4ghjD24C+IwnvUuz2Hyff2YwHOw2QRREq5iWG9"
          + "gsY6WYRKuI159hSB6gR9htJwBcnJ8Eb7WtuuBQVWXKmZDhDHiB4jcDFCuxlTlKuXudNHIAqEwe7Sam4Ia7U65"
          + "7n0dI2a9T6V2iiT1e/+01l/lj0fjke9DhrlsKgLO/jf7drtMZCzBL7mXQisjujkQD5LK265aN1uOGtJKMv95b"
          + "BPkJiQhHNkazgrXAaSwNyrjsVB6EnOYuhBCgLSFmw5igPjgCHfCNCRqFlClJ9431GglDY97PEIU/FzNg2g5f1"
          + "ptYk4VTqdhdP9/voMVJp+cSwXSgzWcEx0mHo+3FZmy3nQ6OJzGI/7/Ao3saMaPAwAA";

  public static void main(String... args) {
    System.out.println("Toccata " + VERSION);
    System.out.println(Path.of(System.getProperty("user.dir")));

    var toccata = new Toccata();
    var lines = toccata.generateJavaProgram();
    lines.forEach(System.out::println);
  }

  List<String> generateJavaProgram() {
    return generateJavaProgram(this::mustaches);
  }

  List<String> generateJavaProgram(UnaryOperator<String> mustaches) {
    var bytes = Base64.getDecoder().decode(TEMPLATE);
    try (var zip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
      var reader = new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8));
      return reader.lines().map(mustaches).collect(Collectors.toList());
    } catch (Exception e) {
      throw new Error("Error while inflating program template!", e);
    }
  }

  String mustaches(String line) {
    if (!line.contains("{{")) return line;
    if (!line.contains("}}")) return line;
    line = line.replaceAll("\\{\\{\\s*NAME-AND-VERSION\\s*}}", "Toccata " + VERSION);
    return line;
  }
}

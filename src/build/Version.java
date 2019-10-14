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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Set various version values to the one of this program's first argument. */
public class Version {
  public static void main(String... args) throws Exception {
    if (args.length != 1) {
      throw new Error("Exactly one argument, the new version, expected! args=" + List.of(args));
    }
    var version = args[0];
    var properties = Path.of(".bach/.properties");
    var mergedBach = Path.of("src/bach/Bach.java");
    var moduleBach = Path.of("src/modules/de.sormuras.bach/main/java/de/sormuras/bach/Bach.java");
    var mavenPom = Path.of("src/modules/de.sormuras.bach/main/maven/pom.xml");

    sed(properties, "version=.+", "version=" + version);
    sed(mergedBach, "VERSION = \".+\";", "VERSION = \"" + version + "\";");
    sed(moduleBach, "VERSION = \".+\";", "VERSION = \"" + version + "\";");
    sed(mavenPom, "<version>.+</version>", "<version>" + version + "</version>");
  }

  private static void sed(Path path, String regex, String replacement) throws Exception {
    Files.writeString(path, Files.readString(path).replaceAll(regex, replacement));
  }
}

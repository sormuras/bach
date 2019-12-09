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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Set various version values to the one of this program's first argument. */
public class Version {

  private static final Path PROPERTIES = Path.of("src/project-info.java.properties");

  public static void main(String... args) throws Exception {
    if (args.length == 0) {
      var properties = new Properties();
      try (var reader = Files.newBufferedReader(PROPERTIES)) {
        properties.load(reader);
      }
      var version = properties.getProperty("version");
      if (version == null) throw new Error("Key 'version' not found in: " + PROPERTIES.toUri());
      System.out.println(version);
      return;
    }

    if (args.length > 1) {
      throw new Error("Exactly one argument, the new version, expected! args=" + List.of(args));
    }
    var version = args[0];
    var readmeMd = Path.of("README.md");
    var mavenPom = Path.of("src/de.sormuras.bach/main/maven/pom.xml");
    var bachJava = Path.of("src/bach/Bach.java");
    var bachJsh = Path.of("src/bach/Bach.jsh");
    // var mergedBach = Path.of("src/bach/MergedBach.java");

    sed(PROPERTIES, "version=.+", "version=" + version);
    sed(readmeMd, "# Bach.java .+ -", "# Bach.java " + version + " -");
    sed(mavenPom, "<version>.+</version>", "<version>" + version + "</version>");
    sed(bachJava, "String VERSION = \".+\";", "String VERSION = \"" + version + "\";");
    sed(bachJsh, "raw/.+/src", "raw/" + (version.endsWith("-ea") ? "master" : version) + "/src");
    // mergedBach.toFile().setWritable(true);
    // sed(mergedBach, "String VERSION = \".+\";", "String VERSION = \"" + version + "\";");
    // mergedBach.toFile().setWritable(false);
  }

  private static void sed(Path path, String regex, String replacement) throws Exception {
    Files.writeString(path, Files.readString(path).replaceAll(regex, replacement));
  }
}

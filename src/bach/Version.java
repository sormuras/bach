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

// default package

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/** Set various version values to the one of this program's first argument. */
public class Version {

  public static void main(String... args) throws Exception {
    if (args.length > 1) throw new Error("Only new version expected: " + List.of(args));

    var bach = Path.of("src/de.sormuras.bach/main/java/de/sormuras/bach/Bach.java");
    var pattern = Pattern.compile("Version VERSION = Version.parse\\(\"(.+)\"\\);");
    var matcher = pattern.matcher(Files.readString(bach));
    if (!matcher.find()) throw new Error("Version constant not found in: " + bach);

    // Only print current version?
    var current = matcher.group(1);
    if (args.length == 0) {
      System.out.println(current);
      return;
    }

    // Set new version
    var version = args[0];
    System.out.println("Set version of Bach.java to: " + version + " (was: " + current + ")");
    sed(bach, pattern.pattern(), "Version VERSION = Version.parse(\"" + version + "\");");

    var readmeMd = Path.of("README.md");
    var pomXml = Path.of("src/de.sormuras.bach/main/maven/pom.xml");
    sed(readmeMd, "# Bach.java .+ -", "# Bach.java " + version + " -");
    sed(pomXml, "<version>.+</version>", "<version>" + version + "</version>");

    var versionOrSNAPSHOT = version.endsWith("-ea") ? "master-SNAPSHOT" : version;
    var bachBootJsh = Path.of("src/bach/bach-boot.jsh");
    sed(bachBootJsh, "\"version\", \".+\"", "\"version\", \"" + versionOrSNAPSHOT + '"');

    var versionOrHEAD = version.endsWith("-ea") ? "HEAD" : version;
    var bachBuildJsh = Path.of("src/bach/bach-build.jsh");
    var bachFetchJsh = Path.of("src/bach/bach-fetch.jsh");
    var bachHelpJsh = Path.of("src/bach/bach-help.jsh");
    var bachInitJsh = Path.of("src/bach/bach-init.jsh");
    sed(bachBuildJsh, "\"version\", \".+\"", "\"version\", \"" + versionOrHEAD + '"');
    sed(bachHelpJsh, "\"version\", \".+\"", "\"version\", \"" + versionOrHEAD + '"');
    sed(bachInitJsh, "\"version\", \".+\"", "\"version\", \"" + versionOrHEAD + '"');
    sed(bachFetchJsh, "raw/.+/src", "raw/" + versionOrHEAD + "/src");
  }

  private static void sed(Path path, String regex, String replacement) throws Exception {
    Files.writeString(path, Files.readString(path).replaceAll(regex, replacement));
  }
}

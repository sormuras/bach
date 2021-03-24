package com.github.sormuras.bach.lookup;

import com.github.sormuras.bach.Bach;
import java.util.Optional;

/** Maps "Java/JavaFX/Kotlin Game Library" module names to their Maven Central artifacts. */
public record FXGLModuleLookup(Bach bach, String version) implements ModuleLookup {

  private static final String MODULE_PREFIX = "com.almasb.fxgl";
  private static final String MAVEN_GROUP = "com.github.almasb";

  @Override
  public Optional<String> lookupUri(String module) {
    if (!module.startsWith(MODULE_PREFIX)) return Optional.empty();
    if (module.equals("com.almasb.fxgl.all")) return via("fxgl");
    return via("fxgl-" + module.substring(MODULE_PREFIX.length() + 1));
  }

  private Optional<String> via(String artifact) {
    if (version.equals("dev-SNAPSHOT")) {
      var repository = "https://oss.sonatype.org/content/repositories/snapshots";
      var snapshot = repository + "/com/github/almasb/" + artifact + "/dev-SNAPSHOT/";
      var metadata = bach.browser().read(snapshot + "maven-metadata.xml");
      var timestamp = find("timestamp", metadata);
      var build = find("buildNumber", metadata);
      var file = artifact + "-dev-" + timestamp + "-" + build + ".jar";
      return Optional.of(snapshot + file);
    }
    return Optional.of(Maven.central(MAVEN_GROUP, artifact, version));
  }

  private static String find(String tag, String text) {
    int start = text.indexOf("<" + tag + ">");
    int end = text.indexOf("</" + tag + ">");
    if (start < 0 || end < 0) return "?";
    return text.substring(start + 1 + tag.length() + 1, end);
  }
}

package com.github.sormuras.bach.external;

import com.github.sormuras.bach.ModuleLocator;

/** Locates "Gluon Attach" modules via their Maven Central artifacts. */
public record GluonAttach(String version) implements ModuleLocator {

  private static final String MODULE_PREFIX = "com.gluonhq.attach";
  private static final String MAVEN_GROUP = "com.gluonhq.attach";

  /**
   * Constructs a new Gluon Attach module locator with the given version.
   *
   * @param version the Gluon Attach version
   */
  public static GluonAttach version(String version) {
    return new GluonAttach(version);
  }

  @Override
  public String caption() {
    return "com.gluonhq.attach.[*] -> Gluon Attach " + version;
  }

  @Override
  public String locate(String module) {
    if (!module.startsWith(MODULE_PREFIX)) return null;
    var artifact = switch (module) {
      case "com.gluonhq.attach.audiorecording" -> "audio-recording";
      case "com.gluonhq.attach.augmentedreality" -> "augmented-reality";
      case "com.gluonhq.attach.barcodescan" -> "barcode-scan";
      case "com.gluonhq.attach.inappbilling" -> "in-app-billing";
      case "com.gluonhq.attach.localnotifications" -> "local-notifications";
      case "com.gluonhq.attach.pushnotifications" -> "push-notifications";
      case "com.gluonhq.attach.runtimeargs" -> "runtime-args";
      default -> module.substring(19).replace('.', '-');
    };
    return Maven.central(MAVEN_GROUP, artifact, version);
  }
}

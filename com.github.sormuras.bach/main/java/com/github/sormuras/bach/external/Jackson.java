package com.github.sormuras.bach.external;

import com.github.sormuras.bach.ExternalModuleLocator;

/** Locates "Jackson" modules via their Maven Central artifacts. */
public record Jackson(String version) implements ExternalModuleLocator {

  private static final String MODULE_PREFIX = "com.fasterxml.jackson";
  private static final String MAVEN_GROUP = "com.fasterxml.jackson";

  /**
   * Constructs a new Jackson module locator with the given version.
   *
   * @param version the Jackson version
   */
  public static Jackson version(String version) {
    return new Jackson(version);
  }

  @Override
  public String caption() {
    return "com.fasterxml.jackson.[*] -> Jackson " + version;
  }

  @Override
  public String locate(String module) {
    if (!module.startsWith(MODULE_PREFIX)) return null;
    var group =
        switch (module) {
          case "com.fasterxml.jackson.annotation",
              "com.fasterxml.jackson.core",
              "com.fasterxml.jackson.databind" -> "com.fasterxml.jackson.core";
          default -> {
            if (module.startsWith("com.fasterxml.jackson.dataformat."))
              yield "com.fasterxml.jackson.dataformat";
            if (module.startsWith("com.fasterxml.jackson.datatype."))
              yield "com.fasterxml.jackson.datatype";
            if (module.startsWith("com.fasterxml.jackson.jakarta.rs."))
              yield "com.fasterxml.jackson.jakarta.rs";
            if (module.startsWith("com.fasterxml.jackson.module."))
              yield "com.fasterxml.jackson.module";
            if (module.startsWith("com.fasterxml.jackson.jaxrs."))
              yield "com.fasterxml.jackson.jaxrs";
            if (module.startsWith("com.fasterxml.jackson.jr.")) yield "com.fasterxml.jackson.jr";
            yield MAVEN_GROUP;
          }
        };
    var artifact =
        switch (module) {
          case "com.fasterxml.jackson.annotation" -> "jackson-annotations";
          case "com.fasterxml.jackson.jakarta.rs.cbor" -> "jackson-jakarta-rs-cbor-provider";
          case "com.fasterxml.jackson.jakarta.rs.json" -> "jackson-jakarta-rs-json-provider";
          case "com.fasterxml.jackson.jakarta.rs.smile" -> "jackson-jakarta-rs-smile-provider";
          case "com.fasterxml.jackson.jakarta.rs.xml" -> "jackson-jakarta-rs-xml-provider";
          case "com.fasterxml.jackson.jakarta.rs.yaml" -> "jackson-jakarta-rs-yaml-provider";
          case "com.fasterxml.jackson.jaxrs.cbor" -> "jackson-jaxrs-cbor-provider";
          case "com.fasterxml.jackson.jaxrs.smile" -> "jackson-jaxrs-smile-provider";
          case "com.fasterxml.jackson.jaxrs.json" -> "jackson-jaxrs-json-provider";
          case "com.fasterxml.jackson.jaxrs.xml" -> "jackson-jaxrs-xml-provider";
          case "com.fasterxml.jackson.jaxrs.yaml" -> "jackson-jaxrs-yaml-provider";
          case "com.fasterxml.jackson.jr.annotationsupport" -> "jackson-jr-annotation-support";
          case "com.fasterxml.jackson.jr.ob" -> "jackson-jr-objects";
          case "com.fasterxml.jackson.datatype.joda" -> "jackson-datatype-joda-money";
          case "com.fasterxml.jackson.datatype.jsonorg" -> "jackson-datatype-json-org";
          case "com.fasterxml.jackson.datatype.jsonp" -> "jackson-datatype-jakarta-jsonp";
          case "com.fasterxml.jackson.dataformat.javaprop" -> "jackson-dataformat-properties";
          case "com.fasterxml.jackson.module.jakarta.xmlbind" -> "jackson-module-jakarta-xmlbind-annotations";
          case "com.fasterxml.jackson.module.jaxb" -> "jackson-module-jaxb-annotations";
          case "com.fasterxml.jackson.module.noctordeser" -> "jackson-module-no-ctor-deser";
          case "com.fasterxml.jackson.module.paramnames" -> "jackson-module-parameter-names";
          default -> module.substring(14).replace('.', '-');
        };
    return Maven.central(group, artifact, version);
  }
}

// default package

import java.lang.module.ModuleDescriptor.Version;

/** Custom build program using {@code Bach${N}.java}. */
class Build {

  public static void main(String... args) {
    var summary =
        Bach.build(
            project ->
                project
                    .version("47.11")
                    .requires("org.junit.jupiter.api", "5.6.0")
                    .requires("org.junit.jupiter.params", "5.6.0")
                    .resolver(Build::resolve));
    if (summary.throwable() != null) {
      summary.throwable().printStackTrace();
      System.exit(1);
    }
  }

  private static Bach.Project.Relation resolve(String module, Version version) {
    switch (module) {
      case "org.hamcrest":
        return Bach.Project.Relation.ofMavenCentral(
            module,
            version != null ? version : Version.parse("2.2"),
            "org.hamcrest",
            "hamcrest",
            "");
    }
    return new Bach.Project.Resolver().apply(module, version);
  }
}

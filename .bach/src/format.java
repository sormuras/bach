class format {
  public static void main(String... args) {
    try (var bach = new com.github.sormuras.bach.Bach(args)) {
      if (bach.configuration().tooling().finder().find("format@1.13.0").isEmpty()) {
        bach.logCaption("Grab external tools");
        bach.run("grab", grab -> grab.add(bach.path().root(".bach", "external.properties")));
      }
      var files = bach.explorer().findJavaFiles();
      bach.logCaption("Format %d .java files".formatted(files.size()));
      bach.run("format@1.13.0", format -> format.add("--replace").addAll(files));
    }
  }
}

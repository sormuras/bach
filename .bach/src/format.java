class format {
  public static void main(String... args) {
    try (var bach = new com.github.sormuras.bach.Bach(args)) {
      var files = bach.explorer().findJavaFiles();
      bach.logCaption("Format %d .java files".formatted(files.size()));
      bach.run("format@1.11.0", format -> format.with("--replace").withAll(files));
    }
  }
}

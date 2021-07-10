class build {
  public static void main(String... args) {
    com.github.sormuras.bach.Bach.build(
        project ->
            project
                .withName("Simplicissimus")
                .withVersion("99")
                .withMainSpace(main -> main.withModule("module-info.java")));
  }
}

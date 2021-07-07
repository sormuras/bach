class build {
  public static void main(String... args) {
    com.github.sormuras.bach.Bach.build(
        project ->
            project
                .withName("Simple")
                .withVersion("1.0.1")
                .withMainProjectSpace(main -> main.withModule("module-info.java")));
  }
}
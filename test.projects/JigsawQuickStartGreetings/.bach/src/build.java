class build {
  public static void main(String... args) {
    com.github.sormuras.bach.Bach.build(
        project ->
            project
                .withName("JigsawQuickStartGreetings")
                .withVersion("99")
                .withMainProjectSpace(main -> main.withModule("com.greetings")));
  }
}

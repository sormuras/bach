class build {
  public static void main(String... args) {
    com.github.sormuras.bach.Bach.build(
        project ->
            project
                .withName("JigsawQuickStartGreetings")
                .withVersion("99")
                .withMainSpace(main -> main.withModule("com.greetings")));
  }
}

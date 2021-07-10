class build {
  public static void main(String... args) {
    com.github.sormuras.bach.Bach.build(
        project ->
            project
                .withName("JigsawQuickStartWorld")
                .withVersion("99")
                .withMainSpace(
                    main -> main.withModule("com.greetings").withModule("org.astro")));
  }
}

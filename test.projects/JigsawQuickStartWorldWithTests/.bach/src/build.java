class build {
  public static void main(String... args) {
    com.github.sormuras.bach.Bach.build(
        project ->
            project
                .withName("JigsawQuickStartWorldWithTests")
                .withVersion("99")
                .withMainSpace(
                    main ->
                        main.withModule("com.greetings/main/java")
                            .withModule("org.astro/main/java"))
                .withTestSpace(
                    test ->
                        test.withModule("test.modules/test/java")
                            .withModulePaths(".bach/workspace/modules")));
  }
}

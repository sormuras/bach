class build {
  public static void main(String... args) {
    com.github.sormuras.bach.Bach.build(
        project ->
            project
                .withName("JigsawQuickStartWorldWithTests")
                .withVersion("99")
                .withMainProjectSpace(
                    main ->
                        main.withModule("com.greetings/main/java")
                            .withModule("org.astro/main/java"))
                .withTestProjectSpace(
                    test ->
                        test.withModule("test.modules/test/java")
                            .withModulePaths(".bach/workspace/modules")));
  }
}

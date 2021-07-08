class build {
  public static void main(String... args) {
    com.github.sormuras.bach.Bach.build(
        project ->
            project
                .withName("Simple")
                .withVersion("1.0.1")
                .withMainProjectSpace(
                    main ->
                        main.withModule(
                            "module-info.java",
                            module ->
                                module.withResources(
                                    "module-info.java",
                                    "simple/Main.java",
                                    "simple/Main.txt",
                                    "simple/internal/Interface.java"))));
  }
}

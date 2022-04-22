@Project(

    init = @Space(modules = {}),
    main = @Space(modules = {}),
    test = @Space(modules = {}),

    tools = @Tools(

        externals = {
            @ExternalTool(
                name = "sormuras-hello-jar",
                from = "https://github.com/sormuras/hello/releases/download/1-ea+1/hello-1-ea+1.jar#SIZE=909"
            ),
            @ExternalTool(
                name = "sormuras-hello-java-07819f3ee7",
                assets = {
                    @Asset(
                        name = "Hello.java",
                        from = "https://github.com/sormuras/hello/raw/07819f3ee7/Hello.java#SIZE=200"
                    ),
                    @Asset(
                        name = "README.md",
                        from = """
                               string:# Hello.java Program
                               
                               Find source code at <https://github.com/sormuras/hello/blob/07819f3ee7/Hello.java>
                               """
                    )
                }
            )
        },

        scripts = {
            @Script(name = "hello", code = "sormuras-hello-jar"),
            @Script(
                name = "bootstrap",
                code = {
                    "info",
                    "bootstrap-download",
                    "bootstrap-format",
                    "bootstrap-compile",
                    "bootstrap-test"
                }
            ),
            @Script(
                name = "bootstrap-download",
                code = {
                    """
                    load-and-verify
                      .bach/external-tools/format@1.15.0/google-java-format-1.15.0-all-deps.jar
                      https://github.com/google/google-java-format/releases/download/v1.15.0/google-java-format-1.15.0-all-deps.jar\
                    #SIZE=3519780\
                    &SHA-256=a356bb0236b29c57a3ab678f17a7b027aad603b0960c183a18f1fe322e4f38ea
                    """,
                    """
                    load-and-verify
                      .bach/external-tools/junit/junit-platform-console-standalone-1.8.2.jar
                      https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.8.2/junit-platform-console-standalone-1.8.2.jar\
                    #SIZE=2410942\
                    &SHA-256=dc498f234228f81c818becfb6b7f71f73df2b0e995f93f25c05fced0c77e6356
                    """,
                    """
                    load-and-verify
                      .bach/external-tools/japicmp/japicmp-0.15.7-jar-with-dependencies.jar
                      https://repo.maven.apache.org/maven2/com/github/siom79/japicmp/japicmp/0.15.7/japicmp-0.15.7-jar-with-dependencies.jar\
                    #SIZE=5781766\
                    &SHA-256=f955be13d679e41b29ab143ab7471d37e77dd2eb9b5aed7c39ad726b7ebdb9aa
                    """
                }
            ),
            @Script(
                name = "bootstrap-format",
                code = """
                    format@1.15.0
                      --replace
                      src/Bach.java
                      test/BachTests.java
                      test/IntegrationTests.java
                    """
            ),
            @Script(
                name = "bootstrap-compile",
                code = {
                    """
                    javac
                      --release
                        17
                      -d
                        .bach/out/bach/classes
                      src/Bach.java
                    """,
                    """
                    jar
                      --create
                      --file
                        .bach/out/bach/bach.jar
                      --main-class
                        Bach
                      -C
                        .bach/out/bach/classes
                        .
                    """
                }
            ),
            @Script(
                name = "bootstrap-test",
                code = {
                    """
                    javac
                      --class-path
                        .bach/out/bach/bach.jar{{path.separator}}.bach/external-tools/junit/junit-platform-console-standalone-1.8.2.jar
                      -d
                        .bach/out/bach/test-classes
                      test/BachTests.java
                      test/IntegrationTests.java
                      test/SwallowSystem.java
                      test/SwallowSystemTests.java
                    """,
                    """
                    junit
                      --class-path
                        .bach/out/bach/bach.jar{{path.separator}}.bach/out/bach/test-classes
                      --disable-banner
                      --scan-class-path
                    """
                }
            )
        }
    )
)
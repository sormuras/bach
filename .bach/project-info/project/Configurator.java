package project;

import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.project.Project;

public class Configurator implements com.github.sormuras.bach.Configurator {
  @Override
  public Project configure(Project project) {
    // Operate on the preconfigured project instance.
    return project
        // Modules of main module space target Java release 17.
        // Modules of test module space target current Java release.
        .withTargetsJava(17)
        .withTargetsJava("test", Runtime.version().feature())

        // Main module space's entry-point.
        .withLauncher(Main.class.getCanonicalName())

        // Additional names of external modules required by this project.
        // All other dependencies are acquired from `module-info.java` files.
        .withRequiresModule("com.github.sormuras.hello")
        .withRequiresModule("org.junit.platform.console")

        // Specify remote locations of all required external modules and tools.
        // Locate module "com.github.sormuras.hello" via an explicit URL with checksum fragments.
        .withExternalModule(
            "com.github.sormuras.hello",
            """
            https://github.com/sormuras/hello/releases\
            /download/1-M3/com.github.sormuras.hello@1-M3.jar\
            #SHA-256=0c993996571cee9555ef947eb198145ddc30a2d834fe606a6f16780afd8aea7b
            """)
        // Locate JUnit modules via https://github.com/sormuras/bach-external-modules
        .withExternalModules("junit", "5.8.2")
        // Locate tool "format@1.15.0" via an explicit URL with checksum fragments.
        .withExternalTool(
            "format@1.15.0",
            """
            https://github.com/google/google-java-format/releases\
            /download/v1.15.0/google-java-format-1.15.0-all-deps.jar\
            #SIZE=3519780\
            &SHA-256=a356bb0236b29c57a3ab678f17a7b027aad603b0960c183a18f1fe322e4f38ea
            """);
  }
}

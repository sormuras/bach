package project;

import com.github.sormuras.bach.Configurator;
import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Paths;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolCallTweak;
import com.github.sormuras.bach.ToolFinder;

public class ProjectConfigurator implements Configurator {
  @Override
  public Project configureProject(Project project) {
    // Operate on the preconfigured project instance.
    return project
        // Modules of main module space target Java release 17.
        .withTargetsJava(17)
        // Main module space's entry-point.
        .withLauncher(Main.class.getCanonicalName())
        // Main module tool call tweaks
        .withTweak(
            ToolCallTweak.WORKFLOW_COMPILE_CLASSES_JAVAC,
            javac ->
                javac
                    .with("-g")
                    .with("-parameters")
                    .with("-Werror")
                    .with("-Xlint")
                    .with("-encoding", "UTF-8"))

        // Modules of test module space target current Java release.
        .withTargetsJava("test", Runtime.version().feature())

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
        .withExternalModules("junit", "5.9.0-M1");
  }

  @Override
  public ToolCallTweak configureToolCallTweak() {
    return ToolCallTweak.identity();
  }

  @Override
  public ToolFinder configureToolFinder(Paths paths) {
    return ToolFinder.compose(
        ToolFinder.ofToolsInModuleLayer(getClass().getModule()),
        ToolFinder.ofToolsInModulePath(paths.externalModules()),
        ToolFinder.ofJavaTools(paths.externalTools()),
        ToolFinder.ofSystemTools(),
        ToolFinder.ofNativeToolsInJavaHome("jarsigner", "java", "jdeprscan", "jfr"));
  }
}

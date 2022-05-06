package project;

import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.project.Project;
import java.util.Map;

public class Configurator implements Project.Configurator {
  @Override
  public Project apply(Project project) {
    return project
        .withTargetsJava(17)
        .withTargetsJava("test", Runtime.version().feature())
        .withLauncher(Main.class.getCanonicalName())
        .withRequiresModule("com.github.sormuras.hello", "org.junit.platform.console")
        .withExternalModule(
            "com.github.sormuras.hello",
            """
                https://github.com/sormuras/hello/releases\
                /download/1-M3/com.github.sormuras.hello@1-M3.jar\
                #SHA-256=0c993996571cee9555ef947eb198145ddc30a2d834fe606a6f16780afd8aea7b""")
        .withExternalModules(Map.of(/* here be name-from pairs */ ))
        .withExternalModules("junit", "5.8.2")
        .withExternalModules("lwjgl", "3.3.1", "windows-x64")
        .withExternalTool(
            "format@1.15.0",
            """
            https://github.com/google/google-java-format/releases\
            /download/v1.15.0/google-java-format-1.15.0-all-deps.jar\
            #SIZE=3519780\
            &SHA-256=a356bb0236b29c57a3ab678f17a7b027aad603b0960c183a18f1fe322e4f38ea""");
  }
}

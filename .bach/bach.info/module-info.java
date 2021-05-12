import static com.github.sormuras.bach.api.CodeSpace.*;
import static com.github.sormuras.bach.api.ExternalLibraryName.*;

import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.api.ProjectInfo.*;

@ProjectInfo(
    // <editor-fold desc="Basic Settings">
    name = "bach",
    version = "17-ea",
    requires = {"org.junit.platform.console"},
    // </editor-fold>
    // <editor-fold desc="Code Spaces">
    main = @Main(modulesPatterns = {"*/main/java"}),
    test = @Test(modulesPatterns = {"*/test/java", "*/test/java-module"}),
    // </editor-fold>+
    // <editor-fold desc="Tool Tweaks">
    tool =
        @Tool(
            skip = {"jlink"},
            tweaks = {
              @Tweak(
                  tool = "javac",
                  with =
                      """
                      -encoding
                       UTF-8
                      -Xlint
                      """),
              @Tweak(tool = "javac", with = "-Werror", spaces = MAIN),
              @Tweak(
                  tool = "javadoc",
                  with =
                      """
                      -encoding
                       UTF-8
                      -notimestamp
                      -Xdoclint:-missing
                      -Werror
                      """),
              @Tweak(tool = "jlink", with = "--launcher", more = "bach=com.github.sormuras.bach"),
              @Tweak(
                  tool = "junit",
                  with = """
                  --config
                    junit.jupiter.execution.parallel.enabled=true
                  --config
                    junit.jupiter.execution.parallel.mode.default=concurrent
                  """),
            }),
    // </editor-fold>
    // <editor-fold desc="External Modules">
    external =
        @External(
            modules = {
              @ExternalModule(name = "junit", link = "junit:junit:4.13.2"),
              @ExternalModule(name = "org.hamcrest", link = "org.hamcrest:hamcrest:2.2"),
            },
            libraries = {
              @ExternalLibrary(name = JUNIT, version = "5.8.0-M1"),
            })
    // </editor-fold>
    )
module bach.info {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.Factory with
      bach.info.MyFactory;
}

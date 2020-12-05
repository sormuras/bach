import com.github.sormuras.bach.project.Feature;
import com.github.sormuras.bach.project.ProjectInfo;
import com.github.sormuras.bach.project.ProjectInfo.ExternalModules;
import com.github.sormuras.bach.project.ProjectInfo.ExternalModules.Link;
import com.github.sormuras.bach.project.ProjectInfo.Test;
import com.github.sormuras.bach.project.ProjectInfo.Tweak;

@ProjectInfo(
    name = "bach",
    version = "16-ea",
    modules = "com.github.sormuras.bach/main/java/module-info.java",
    compileModulesForJavaRelease = 16,
    features = {
      Feature.GENERATE_API_DOCUMENTATION,
      Feature.GENERATE_CUSTOM_RUNTIME_IMAGE,
      Feature.INCLUDE_SOURCES_IN_MODULAR_JAR
    },
    tweaks = {
      @Tweak(
          tool = "javac",
          args = {"-encoding", "UTF-8", "-g", "-parameters", "-Werror", "-Xlint"}),
      @Tweak(
          tool = "javadoc",
          args = {
            "-encoding",
            "UTF-8",
            "-windowtitle",
            "\uD83C\uDFBC Bach",
            "-header",
            "\uD83C\uDFBC Bach",
            "-use",
            "-linksource",
            "-notimestamp",
            "-Werror",
            "-Xdoclint",
            "-quiet"
          })
    },
    externalModules =
        @ExternalModules(
            requires = {"org.junit.platform.console"},
            links = {
              @Link(module = "org.apiguardian.api", to = "org.apiguardian:apiguardian-api:1.1.0"),
              @Link(module = "org.opentest4j", to = "org.opentest4j:opentest4j:1.2.0"),
            }),
    test =
        @Test(
            modules = {
              "com.github.sormuras.bach/test/java-module/module-info.java",
              "test.base/test/java/module-info.java",
              "test.integration/test/java/module-info.java",
            },
            tweaks =
                @Tweak(
                    tool = "junit",
                    args = {"--fail-if-no-tests"})))
module build {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.BuildProgram with
      build.BachBuildProgram;
  provides com.github.sormuras.bach.project.ModuleLookup with
      build.JUnitModuleLookup;
}

import com.github.sormuras.bach.project.Feature;
import com.github.sormuras.bach.project.ProjectInfo;
import com.github.sormuras.bach.project.ProjectInfo.Link;
import com.github.sormuras.bach.project.ProjectInfo.Tweak;

@ProjectInfo(
    name = "bach",
    version = "16-ea",
    modules = "com.github.sormuras.bach/main/java/module-info.java",
    compileModulesForJavaRelease = 16,
    features = {
      Feature.GENERATE_API_DOCUMENTATION,
      Feature.GENERATE_CUSTOM_RUNTIME_IMAGE,
      Feature.GENERATE_MAVEN_POM_FILES,
      Feature.INCLUDE_SOURCES_IN_MODULAR_JAR
    },
    tests = {
      "com.github.sormuras.bach/test/java-module/module-info.java",
      "test.base/test/java/module-info.java",
      "test.integration/test/java/module-info.java",
    },
    requires = {"org.junit.platform.console"},
    links = {
      @Link(module = "org.apiguardian.api", to = "org.apiguardian:apiguardian-api:1.1.0"),
      @Link(module = "org.opentest4j", to = "org.opentest4j:opentest4j:1.2.0"),
    },
    tweaks = {
      @Tweak(tool = "javac", with = {"-encoding", "UTF-8"}),
      @Tweak(tool = "javac", with = "-g"),
      @Tweak(tool = "javac", with = "-parameters"),
      @Tweak(tool = "javac", with = "-Xlint"),
      @Tweak(tool = "javac", with = "-Werror", in = Tweak.Space.MAIN),
      @Tweak(tool = "javadoc", with = {"-encoding", "UTF-8"}),
      @Tweak(tool = "javadoc", with = {"-overview", "documentation/api/overview.html"}),
      @Tweak(tool = "javadoc", with = "-quiet"),
      @Tweak(tool = "javadoc", with = "-notimestamp"),
      @Tweak(tool = "javadoc", with = "-use"),
      @Tweak(tool = "javadoc", with = "-Xdoclint"),
      @Tweak(tool = "javadoc", with = "-Werror"),
      @Tweak(tool = "junit", with = "--fail-if-no-tests", in = Tweak.Space.TEST)
    })
module build {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.BuildProgram with
      build.BachBuildProgram;
  provides com.github.sormuras.bach.project.ModuleLookup with
      build.JUnitModuleLookup;
}

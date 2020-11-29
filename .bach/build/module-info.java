import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.Library;
import com.github.sormuras.bach.ProjectInfo.Library.Link;
import com.github.sormuras.bach.ProjectInfo.Library.Searcher;
import com.github.sormuras.bach.ProjectInfo.Main;
import com.github.sormuras.bach.ProjectInfo.Test;
import com.github.sormuras.bach.ProjectInfo.Tweak;
import com.github.sormuras.bach.module.ModuleSearcher.JUnitJupiterSearcher;
import com.github.sormuras.bach.module.ModuleSearcher.JUnitPlatformSearcher;

@ProjectInfo(
    name = "bach",
    version = "16-ea",
    library =
        @Library(
            requires = {"org.junit.platform.console"},
            links = {
              @Link(module = "org.apiguardian.api", target = "org.apiguardian:apiguardian-api:1.1.0"),
              @Link(module = "org.opentest4j", target = "org.opentest4j:opentest4j:1.2.0"),
            },
            searchers = {
              @Searcher(with = JUnitJupiterSearcher.class, version = "5.7.0"),
              @Searcher(with = JUnitPlatformSearcher.class, version = "1.7.0"),
            }),
    main =
        @Main(
            modules = "com.github.sormuras.bach/main/java/module-info.java",
            release = 16,
            generateApiDocumentation = true,
            generateCustomRuntimeImage = true,
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
            }),
    test =
        @Test(
            modules = {
                "com.github.sormuras.bach/test/java-module/module-info.java",
                "test.base/test/java/module-info.java",
                "test.integration/test/java/module-info.java",
            }))
module build {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.BuildProgram with
      build.BachBuildProgram;
}

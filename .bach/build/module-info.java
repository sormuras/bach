import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.Library;
import com.github.sormuras.bach.ProjectInfo.Main;
import com.github.sormuras.bach.ProjectInfo.Test;
import com.github.sormuras.bach.ProjectInfo.Tweak;

@ProjectInfo(
    name = "bach",
    version = "16-ea",
    library = @Library(requires = {"org.junit.platform.console"}),
    main =
        @Main(
            release = 16,
            generateApiDocumentation = true,
            tweaks = {
              @Tweak(
                  tool = "javac",
                  args = {"-encoding", "UTF-8", "-g", "-parameters", "-Werror", "-Xlint"}),
              @Tweak(
                  tool = "jar(com.github.sormuras.bach)",
                  args = {"--main-class", "com.github.sormuras.bach.Main"}),
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
            tweaks =
                @Tweak(
                    tool = "javac",
                    args = {
                      "--patch-module",
                      "com.github.sormuras.bach=com.github.sormuras.bach/main/java"
                    })))
module build {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.BuildProgram with
      build.BachBuildProgram;
}

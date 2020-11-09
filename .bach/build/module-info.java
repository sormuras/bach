import build.BachBuildProgram;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.Main;
import com.github.sormuras.bach.ProjectInfo.Tweak;

@ProjectInfo(
    name = "bach",
    main =
        @Main(
            modules = "com.github.sormuras.bach",
            moduleSourcePaths = "./*/main/java",
            release = 16,
            generateApiDocumentation = true,
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
            }))
module build {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.BuildProgram with BachBuildProgram;
}

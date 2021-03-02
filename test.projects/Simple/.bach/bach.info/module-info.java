import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.Tweak;

@ProjectInfo(
    name = "simple",
    version = "1.0.1",
    modules = "simple",
    compileModulesForJavaRelease = 11,
    modulePaths = {},
    tweaks = {
      @Tweak(tool = "javac", option = "--module-source-path", value = "simple=."),
      @Tweak(tool = "jar(simple)", option = "module-info.java"),
      @Tweak(tool = "jar(simple)", option = "simple/Main.txt"),
    })
module bach.info {
  requires com.github.sormuras.bach;
}

import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.*;

@ProjectInfo(
    options = @Options(tools = @Tools(skip = {"jdeps", "javadoc", "jlink"})),
    test =
        @TestSpace(
            tweaks = {
              @Tweak(
                  tool = "javac",
                  option = "--processor-module-path",
                  value = ".bach/workspace/modules"),
              @Tweak(tool = "javac", option = "-Xplugin:showPlugin"),
            }))
module bach.info {
  requires com.github.sormuras.bach;
}

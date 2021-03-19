import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.*;

@ProjectInfo(compileModulesForJavaRelease = 8, tools = @Tools(limit = {"javac", "jar"}))
module bach.info {
  requires com.github.sormuras.bach;
}

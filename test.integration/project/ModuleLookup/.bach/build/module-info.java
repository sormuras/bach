import com.github.sormuras.bach.project.ProjectInfo;
import com.github.sormuras.bach.project.ProjectInfo.Link;

@ProjectInfo(links = @Link(module = "foo", to = "module-foo.zip", type = Link.Type.PATH))
module build {
  requires com.github.sormuras.bach;
}

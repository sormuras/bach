import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Project.Link;

@Project(
    name = "bach",
    java = 16,
    links = {@Link(module = "junit", target = "junit:junit:4.13.1")})
module build {
  requires com.github.sormuras.bach;

  provides java.util.spi.ToolProvider with
      build.Build;
}

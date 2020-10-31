import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Project.Link;

@Project(
    name = "bach",
    java = 16,
    links = {
      @Link(module = "junit", target = "junit:junit:4.13.1"),
      @Link(module = "org.junit.jupiter", target = "org.junit.jupiter:junit-jupiter:5.7.0"),
      @Link(module = "org.junit.jupiter.api", target = "org.junit.jupiter:junit-jupiter-api:5.7.0"),
      @Link(module = "org.junit.jupiter.engine", target = "org.junit.jupiter:junit-jupiter-engine:5.7.0"),
      @Link(module = "org.junit.jupiter.params", target = "org.junit.jupiter:junit-jupiter-params:5.7.0"),
    })
module build {
  requires com.github.sormuras.bach;

  provides java.util.spi.ToolProvider with
      build.Build;
}

import com.github.sormuras.bach.module.Link;

@Link(
    module = "junit",
    uri = "https://repo.maven.apache.org/maven2/junit/junit/4.13.1/junit-4.13.1.jar")
module build {
  requires com.github.sormuras.bach;

  provides java.util.spi.ToolProvider with
      build.Build;
}

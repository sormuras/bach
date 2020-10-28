import com.github.sormuras.bach.module.Link;

@Link(module = "junit", target = "junit:junit:4.13.1")
module build {
  requires com.github.sormuras.bach;

  provides java.util.spi.ToolProvider with
      build.Build;
}

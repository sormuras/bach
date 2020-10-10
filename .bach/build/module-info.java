module build {
  requires com.github.sormuras.bach;

  provides java.util.spi.ToolProvider with
      build.Build;
}

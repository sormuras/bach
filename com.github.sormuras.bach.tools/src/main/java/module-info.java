module com.github.sormuras.bach.tools {
  requires com.github.sormuras.bach.project;

  provides java.util.spi.ToolProvider with
      com.github.sormuras.bach.tools.Compiler;
}

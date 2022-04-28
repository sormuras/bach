module project {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.project.Project.Configurator with
      project.Configurator;
  provides java.util.spi.ToolProvider with
      project.Hello;
}

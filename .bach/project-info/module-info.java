module project {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.Configurator with
      project.ProjectConfigurator;
  provides java.util.spi.ToolProvider with
      project.Build,
      project.World;
}

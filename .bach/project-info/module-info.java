import com.github.sormuras.bach.ProjectInfoConfigurator;

module project {
  requires com.github.sormuras.bach;

  provides ProjectInfoConfigurator with
      project.Configurator;
  provides java.util.spi.ToolProvider with
      project.World;
}

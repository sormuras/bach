import com.github.sormuras.bach.project.Project;
import project.Configurator;

module project {
  requires com.github.sormuras.bach;

  provides Project.Configurator with
      Configurator;
  provides java.util.spi.ToolProvider with
      project.Hello;
}

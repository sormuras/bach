import com.github.sormuras.bach.tools.CompileToolProvider;
import java.util.spi.ToolProvider;

module com.github.sormuras.bach.tools {
  requires com.github.sormuras.bach.project;

  provides ToolProvider with
      CompileToolProvider;
}

import com.github.sormuras.bach.tools.BuildToolProvider;
import com.github.sormuras.bach.tools.CompileToolProvider;
import com.github.sormuras.bach.tools.TestToolProvider;
import java.util.spi.ToolProvider;

module com.github.sormuras.bach.tools {
  requires com.github.sormuras.bach.project;

  provides ToolProvider with
      BuildToolProvider,
      CompileToolProvider,
      TestToolProvider;
}

import java.util.spi.*;
import project.*;
import run.bach.*;

module project {
  requires run.bach;

  provides BachFactory with
      ProjectLocalBachFactory;
  provides ToolOperator with
      format,
      ProjectLocalOperator;
  provides ToolProvider with
      ProjectLocalTool;
}

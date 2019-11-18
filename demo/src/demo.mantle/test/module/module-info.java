open /*test*/ module demo.mantle /*extends "main" module*/ {
  requires demo.core;

  provides java.util.spi.ToolProvider with
      demo.mantle.TestTool;
}

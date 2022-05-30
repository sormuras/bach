/** Defines Bach's API. */
module com.github.sormuras.bach {
  exports com.github.sormuras.bach;
  exports com.github.sormuras.bach.project;

  requires java.base;
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jfr;
  requires jdk.jlink;

  uses com.github.sormuras.bach.Configurator;
  uses java.util.spi.ToolProvider;

  provides java.util.spi.ToolProvider with
      com.github.sormuras.bach.Main,
      com.github.sormuras.bach.core.Banner,
      com.github.sormuras.bach.core.Checksum.Tool,
      com.github.sormuras.bach.core.Info,
      com.github.sormuras.bach.core.Load,
      com.github.sormuras.bach.core.LoadAndVerify,
      com.github.sormuras.bach.core.LoadTool,
      com.github.sormuras.bach.core.Tree,
      com.github.sormuras.bach.workflow.Build,
      com.github.sormuras.bach.workflow.Cache,
      com.github.sormuras.bach.workflow.Compile,
      com.github.sormuras.bach.workflow.CompileClasses,
      com.github.sormuras.bach.workflow.CompileModules,
      com.github.sormuras.bach.workflow.Launch,
      com.github.sormuras.bach.workflow.Test;
}

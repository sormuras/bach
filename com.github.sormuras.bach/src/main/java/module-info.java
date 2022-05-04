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

  uses com.github.sormuras.bach.project.Project.Configurator;
  uses java.util.spi.ToolProvider;

  provides java.util.spi.ToolProvider with
      com.github.sormuras.bach.Main,
      com.github.sormuras.bach.core.Banner,
      com.github.sormuras.bach.core.Checksum,
      com.github.sormuras.bach.core.Info,
      com.github.sormuras.bach.core.Load,
      com.github.sormuras.bach.core.LoadAndVerify,
      com.github.sormuras.bach.core.Tree,
      com.github.sormuras.bach.project.workflow.Build,
      com.github.sormuras.bach.project.workflow.Compile,
      com.github.sormuras.bach.project.workflow.Download,
      com.github.sormuras.bach.project.workflow.Launch;
}

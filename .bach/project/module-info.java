module project {
  requires run.bach;

  provides run.bach.Project.Factory with
      project.Info;
  provides run.bach.ToolOperator with
      project.overlay.CompileClasses,
      project.toolbox.build,
      project.toolbox.format,
      project.toolbox.rebuild;
  provides run.bach.ToolTweak with
      project.tweak.CallWithUtf8EncodingTweak;
}

@run.bach.Project.Info(name = "Bach")
module project {
  requires run.bach;

  provides run.bach.Project.Composer with
      project.Info;
  provides run.bach.ToolOperator with
      project.overlay.CompileClasses,
      project.toolbox.build,
      project.toolbox.format;
  provides run.bach.ToolTweak with
      project.tweak.CallWithUtf8EncodingTweak;
}
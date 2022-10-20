module project {
  requires run.bach;

  provides run.bach.Project.Composer with
      project.ProjectInfo;
  provides run.bach.ToolTweak with
      project.CallWithUtf8EncodingTweak;
  provides run.bach.ToolOperator with
      project.CompileClasses,
      project.build,
      project.format;
}

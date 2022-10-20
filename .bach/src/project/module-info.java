module project {
  requires run.bach;

  provides run.bach.project.ProjectComposer with
      project.ProjectInfo;
  provides run.bach.ToolTweak with
      project.JavacEncodingUtf8Tweak;
  provides run.bach.ToolOperator with
      project.CompileClasses,
      project.build,
      project.format;
}

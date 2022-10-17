module project {
  requires run.bach;

  provides run.bach.ToolOperator with
      project.build,
      project.format;
}

module project {
  requires run.bach;

  provides run.bach.project.ProjectComposer with
      project.ProjectInfo;
  provides run.bach.ToolOperator with
      project.build,
      project.format;
}

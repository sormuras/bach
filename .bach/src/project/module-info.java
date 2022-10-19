module project {
  requires run.bach;

  provides run.bach.project.ProjectComposer with
      project.ProjectInfo;
  provides run.bach.project.workflow.CompileClasses.JavacTweak with
      project.CompileClassesJavacTweak;
  provides run.bach.ToolOperator with
      project.CompileModules,
      project.build,
      project.format;
}

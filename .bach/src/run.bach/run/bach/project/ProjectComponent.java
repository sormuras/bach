package run.bach.project;

public sealed interface ProjectComponent
    permits ProjectExternals, ProjectName, ProjectSpaces, ProjectVersion {}

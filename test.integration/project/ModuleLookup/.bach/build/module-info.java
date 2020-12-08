@com.github.sormuras.bach.project.ProjectInfo
module build {
  requires com.github.sormuras.bach;
  provides com.github.sormuras.bach.project.ModuleLookup with build.FooModuleLookup;
}

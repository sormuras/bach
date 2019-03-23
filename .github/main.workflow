workflow "Execute smoke tests" {
  on = "push"
  resolves = ["Print OpenJDK URIs"]
}

action "Print OpenJDK URIs" {
  uses = "./.github/action/test-install-jdk-action"
}

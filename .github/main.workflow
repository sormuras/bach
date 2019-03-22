workflow "New workflow" {
  on = "push"
  resolves = ["Test install-jdk.sh action"]
}

action "Test install-jdk.sh action" {
  uses = "./.github/action/test-install-jdk-action"
}

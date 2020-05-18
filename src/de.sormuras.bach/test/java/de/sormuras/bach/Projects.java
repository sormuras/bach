package de.sormuras.bach;

public interface Projects {
  static Project zero() {
    return Project.builder().title("Zero").version("0").newProject();
  }
}

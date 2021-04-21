package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Option;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.Spaces;

public class ProjectBuilder {

  protected final Logbook logbook;
  protected final Options options;

  public ProjectBuilder(Logbook logbook, Options options) {
    this.logbook = logbook;
    this.options = options;
  }

  public Project build() {
    var name = options.get(Option.PROJECT_NAME);
    var folders = Folders.of(options.get(Option.CHROOT));
    var spaces = new Spaces(new CodeSpaceMain(), new CodeSpaceTest());
    return new Project(name, folders, spaces);
  }
}

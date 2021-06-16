package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Folders;

public record Settings(
    Options options, Logbook logbook, Folders folders, Browser browser, Workflows workflows) {

  public static Settings of(Options options, Logbook logbook) {
    var folders = Folders.of(options.chroot());
    var browser = Browser.of(options, logbook);
    var workflows = Workflows.of();
    return new Settings(options, logbook, folders, browser, workflows);
  }

  public Settings with(Folders folders) {
    return new Settings(options, logbook, folders, browser, workflows);
  }
}

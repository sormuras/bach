package com.github.sormuras.bach;

import com.github.sormuras.bach.settings.BrowserSettings;
import com.github.sormuras.bach.settings.Folders;
import com.github.sormuras.bach.settings.Logbook;

public interface Settings {

  Logbook logbook();

  Folders folders();

  BrowserSettings browserSettings();

  static NewSettings newSettings() {
    return new NewSettings(
        Logbook.ofSystem(),
        Folders.of(""),
        BrowserSettings.ofConnectTimeoutSeconds(10));
  }

  record NewSettings(
      Logbook logbook,
      Folders folders,
      BrowserSettings browserSettings)
      implements Settings {

    public NewSettings with(Object component) {
      return new NewSettings(
          component instanceof Logbook logbook ? logbook : logbook,
          component instanceof Folders folders ? folders : folders,
          component instanceof BrowserSettings settings ? settings : browserSettings);
    }

    public NewSettings withBrowserConnectTimeout(int seconds) {
      return with(BrowserSettings.ofConnectTimeoutSeconds(seconds));
    }
  }
}

package com.github.sormuras.bach;

import java.lang.System.Logger.Level;

public sealed interface Note permits Logbook.CaptionNote, Logbook.MessageNote, Logbook.RunNote {
  static Logbook.CaptionNote caption(String text) {
    return new Logbook.CaptionNote(text);
  }

  static Logbook.MessageNote message(String text) {
    return new Logbook.MessageNote(Level.INFO, text);
  }

  static Logbook.MessageNote message(Level level, String text) {
    return new Logbook.MessageNote(level, text);
  }
}

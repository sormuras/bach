package project;

import run.bach.Composer;
import run.bach.Tweaks;

public class BachComposer extends Composer {
  @Override
  public Tweaks createTweaks() {
    return new Tweaks(
        call ->
            switch (call.tool()) {
              case "javac" -> call.withTweak(0, tweak -> tweak.with("-g").with("-parameters"));
              case "junit" -> call.with("--details", "NONE").with("--disable-banner");
              default -> call;
            });
  }
}

package run.bach.conf;

import run.bach.Configurator;
import run.bach.Tweaks;
import run.bach.Workbench;

public class TweaksConfigurator implements Configurator {
  @Override
  public void configure(Workbench bench) {
    bench.put(new Tweaks());
  }
}

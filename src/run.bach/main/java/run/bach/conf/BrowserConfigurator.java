package run.bach.conf;

import run.bach.Browser;
import run.bach.Configurator;
import run.bach.Workbench;

public class BrowserConfigurator implements Configurator {
  @Override
  public void configure(Workbench bench) {
    bench.put(new Browser());
  }
}

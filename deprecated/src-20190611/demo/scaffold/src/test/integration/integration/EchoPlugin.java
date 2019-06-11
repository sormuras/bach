package integration;

import scaffold.api.ScaffoldPlugin;

public class EchoPlugin implements ScaffoldPlugin {
  @Override
  public String apply(String string) {
    return string;
  }
}

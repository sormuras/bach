package project;

import run.bach.Bach;
import run.bach.BachFactory;
import run.bach.Configuration;

public class ProjectLocalBachFactory implements BachFactory {
  @Override
  public Bach createBach(Configuration configuration) {
    return new ProjectLocalBach(configuration);
  }
}

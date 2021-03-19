package engine;

enum OverlaySingleton implements Overlay {
  INSTANCE;

  @Override
  public String display() {
    return "Engine (Java 8)";
  }
}

package run;

class Start {
  public static void main(String... args) {
    Project.ofCurrentWorkingDirectory().start(args);
  }
}

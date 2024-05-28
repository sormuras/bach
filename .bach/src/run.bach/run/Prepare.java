package run;

class Prepare {
  public static void main(String... args) {
    Project.ofCurrentWorkingDirectory().prepare();
  }
}

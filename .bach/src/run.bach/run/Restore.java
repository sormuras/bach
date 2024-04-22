package run;

class Restore {
  public static void main(String... args) {
    Project.ofCurrentWorkingDirectory().restore();
  }
}

package run;

class Status {
  public static void main(String... args) {
    Project.ofCurrentWorkingDirectory().printStatus();
  }
}

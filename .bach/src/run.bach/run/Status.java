package run;

class Status {
  public static void main(String... args) {
    Project.ofCurrentWorkingDirectory().print(Project.PrinterTopic.STATUS);
  }
}

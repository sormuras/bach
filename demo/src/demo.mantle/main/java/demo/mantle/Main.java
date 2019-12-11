package demo.mantle;

import demo.core.PublicCore;

class Main {
  public static void main(String[] args) {
    System.out.println("Main in " + Main.class.getModule());
    System.out.println("Core in " + PublicCore.class.getModule());
  }
}

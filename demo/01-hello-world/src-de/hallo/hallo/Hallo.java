package hallo;

import com.greetings.Greeter;

public class Hallo implements Greeter {

  @Override
  public String greet(String name) {
    return "Hallo " + name;
  }

  @Override
  public String world() {
    return "Welt";
  }
}

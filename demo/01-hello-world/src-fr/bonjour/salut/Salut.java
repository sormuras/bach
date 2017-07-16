package salut;

import com.greetings.Greeter;

public class Salut implements Greeter {

  @Override
  public String greet(String name) {
    return "Salut " + name;
  }

  @Override
  public String world() {
    return "monde";
  }
}

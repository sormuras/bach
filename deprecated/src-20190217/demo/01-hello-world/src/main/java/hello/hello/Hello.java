package hello;

import com.greetings.Greeter;

public class Hello implements Greeter {

  @Override
  public String greet(String name) {
    return "Hello " + name;
  }
}

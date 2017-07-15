package hello;

import com.greetings.Greeter;

public class Hello implements Greeter {

    public String greet(String name) {
        return "Hello " + name;
    }

}

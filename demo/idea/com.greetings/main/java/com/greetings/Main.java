package com.greetings;

import java.util.Locale;
import java.util.ResourceBundle;
import org.astro.World;

public class Main {

  public static void main(String[] args) {
    System.out.format("Greetings %s!%n", World.name());

    Locale locale = Locale.getDefault();
    Module module = Main.class.getModule();
    ResourceBundle messages =
        ResourceBundle.getBundle("com.greetings.MessagesBundle", locale, module);

    System.out.println(messages.getString("greetings"));
    System.out.println(messages.getString("inquiry"));
    System.out.println(messages.getString("farewell"));
  }
}

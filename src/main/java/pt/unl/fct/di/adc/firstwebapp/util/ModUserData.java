package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.Map;

public class ModUserData {
  public Input input;
  public AuthToken token;

  public class Input {
    public String username;
    public Map<String, String> attributes;
  }

  public ModUserData() {
  }
}
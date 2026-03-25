package pt.unl.fct.di.adc.firstwebapp.util;

public class ModUserData {
  public Input input;
  public AuthToken token;

  public class Input {
    public String username;
    public Attribute attributes;

    public class Attribute {
      public String phone;
      public String address;
    }
  }

  public ModUserData() {
  }

  public ModUserData(AuthToken token, String username) {
    this.input = new Input();
    this.token = token;
    this.input.username = username;
  }
}
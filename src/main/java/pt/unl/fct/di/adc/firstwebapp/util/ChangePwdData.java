package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangePwdData {
  public Input input;
  public AuthToken token;

  public class Input {
    public String username;
    public String oldPassword;
    public String newPassword;
  }

  public ChangePwdData() {
  }

  public ChangePwdData(String username, String oldPassword, String newPassword, AuthToken token) {
    this.input = new Input();
    this.token = token;
    this.input.username = username;
    this.input.oldPassword = oldPassword;
    this.input.newPassword = newPassword;
  }

}

package pt.unl.fct.di.adc.firstwebapp.util;

public class ShowUsersData {

  public Input input;
  public AuthToken token;

  public class Input {
  }

  public ShowUsersData() {
  }

  public ShowUsersData(AuthToken token) {
    this.input = new Input();
    this.token = token;
  }

  public boolean hasTokenExpired() {
    return token != null && token.isExpired();
  }

  public boolean hasPermission(Role... allowedRoles) {
    return token != null && token.hasPermission(allowedRoles);
  }

}

package pt.unl.fct.di.adc.firstwebapp.util;

public class ShowData {

  public Input input;
  public AuthToken token;

  public class Input {
  }

  public ShowData() {
  }

  public ShowData(AuthToken token) {
    this.input = new Input();
    this.token = token;
  }
}

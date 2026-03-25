package pt.unl.fct.di.adc.firstwebapp.util;

public class LoginData {
	public Input input;

	public class Input {
		public String username;
		public String password;
	}

	public LoginData() {
	}

	public LoginData(String username, String password) {
		this.input = new Input();
		this.input.username = username;
		this.input.password = password;
	}

}

package pt.unl.fct.di.adc.firstwebapp.util;

public class RegisterData {

	public Input input;

	public class Input {
		public String username;
		public String password;
		public String confirmation;
		public String phone;
		public String address;
		public Role role;
	}

	public RegisterData() {

	}

	public RegisterData(String username, String password, String confirmation, String phone, String address, Role role) {
		this.input = new Input();
		this.input.username = username;
		this.input.password = password;
		this.input.confirmation = confirmation;
		this.input.phone = phone;
		this.input.address = address;
		this.input.role = role;
	}

	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	private boolean checkRole(Role role) {
		for (Role r : Role.values()) {
			if (r.equals(role))
				return true;
		}
		return false;
	}

	public boolean validRegistration() {
		if (input == null)
			return false;
		return nonEmptyOrBlankField(input.username) &&
				nonEmptyOrBlankField(input.password) &&
				nonEmptyOrBlankField(input.confirmation) &&
				nonEmptyOrBlankField(input.address) &&
				nonEmptyOrBlankField(input.phone) &&
				checkRole(input.role) &&
				input.username.contains("@") &&
				input.password.equals(input.confirmation);
	}
}
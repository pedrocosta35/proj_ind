package pt.unl.fct.di.adc.firstwebapp.util;

public class RegisterData {

	public String username;
	public String password;
	public String confirmation;
	public String phone;
	public String address;
	public Role role;

	public RegisterData() {

	}

	public RegisterData(String username, String password, String confirmation, String phone, String address, Role role) {
		this.username = username;
		this.password = password;
		this.confirmation = confirmation;
		this.phone = phone;
		this.address = address;
		this.role = role;
	}

	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	private boolean checkRole(Role role) {
		return role.equals(Role.ADMIN) || role.equals(Role.BOFFICER) || role.equals(Role.USER);
	}

	public boolean validRegistration() {

		return nonEmptyOrBlankField(username) &&
				nonEmptyOrBlankField(password) &&
				nonEmptyOrBlankField(address) &&
				nonEmptyOrBlankField(phone) &&
				checkRole(role) &&
				address.contains("@") &&
				password.equals(confirmation);
	}
}
package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

import jakarta.servlet.http.HttpServletRequest;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.DeleteUserData;
import pt.unl.fct.di.adc.firstwebapp.util.FailedResponse;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;
import pt.unl.fct.di.adc.firstwebapp.util.ModUserData;
import pt.unl.fct.di.adc.firstwebapp.util.Role;
import pt.unl.fct.di.adc.firstwebapp.util.ShowData;
import pt.unl.fct.di.adc.firstwebapp.util.SuccessResponse;
import pt.unl.fct.di.adc.firstwebapp.util.SessionsData;
import pt.unl.fct.di.adc.firstwebapp.util.UsersData;
import pt.unl.fct.di.adc.firstwebapp.util.FailedResponse.AppError;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;

import com.google.gson.Gson;

@Path("/")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class PostRegistrationResource {

	private static final String LOG_MESSAGE_LOGIN_ATTEMP = "Login attempt by user: ";
	private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful by user: ";
	private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password for: ";

	/**
	 * Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(PostRegistrationResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	private final Gson g = new Gson();

	public PostRegistrationResource() {
	} // Nothing to be done here

	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(LoginData data) {
		LOG.fine(LOG_MESSAGE_LOGIN_ATTEMP + data.input.username);

		Key userKey = userKeyFactory.newKey(data.input.username);

		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if (user == null) {
				// Username does not exist
				LOG.warning(LOG_MESSAGE_LOGIN_ATTEMP + data.input.username);
				return Response.status(Status.OK)
						.entity(g.toJson(new FailedResponse(AppError.USER_NOT_FOUND)))
						.build();
			}

			String hashedPWD = (String) user.getString("password");
			if (hashedPWD.equals(DigestUtils.sha512Hex(data.input.password))) {
				// Retrieve role from the stored user entity
				Role role = Role.valueOf(user.getString("role"));

				// Build and persist the token
				AuthToken token = new AuthToken(data.input.username, role);
				Key tokenKey = datastore.newKeyFactory()
						.addAncestors(PathElement.of("User", data.input.username))
						.setKind("AuthToken")
						.newKey(token.tokenID);

				Entity tokenEntity = Entity.newBuilder(tokenKey)
						.set("tokenId", token.tokenID)
						.set("username", token.username)
						.set("role", token.role.name())
						.set("issuedAt", token.issuedAt)
						.set("expiresAt", token.expiresAt)
						.build();

				// Batch operation
				txn.put(tokenEntity);
				txn.commit();

				// Return token
				LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.input.username);
				return Response
						.ok(g.toJson(new SuccessResponse<>(new LoginResponse(token))))
						.build();
			} else {
				// Wrong password
				LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.input.username);
				return Response.status(Status.OK)
						.entity(g.toJson(new FailedResponse(AppError.INVALID_CREDENTIALS))).build();
			}
		} catch (Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}

	private class LoginResponse {
		public AuthToken token;

		public LoginResponse(AuthToken token) {
			this.token = token;
		}
	}

	@POST
	@Path("/showusers")
	@Consumes(MediaType.APPLICATION_JSON) // dar 200 sempre em erros
	@Produces(MediaType.APPLICATION_JSON)
	public Response showUsers(ShowData data) {
		if (data == null || data.token == null || data.token.tokenID == null) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.INVALID_TOKEN)))
					.build();
		}
		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.token.username))
				.setKind("AuthToken")
				.newKey(data.token.tokenID);

		Entity tokenEntity = datastore.get(tokenKey);
		Response tokenCheck = checkToken(tokenEntity, Role.ADMIN, Role.BOFFICER);
		if (tokenCheck != null) {
			return tokenCheck; // stops here if token is invalid
		}

		Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
		QueryResults<Entity> users = datastore.run(query);

		List<UsersData> userList = new ArrayList<>();
		while (users.hasNext()) {
			userList.add(new UsersData(users.next()));
		}
		return Response.ok(g.toJson(new SuccessResponse<>(new ShowUsersResponse(userList)))).build();
	}

	private void removeToken(String username, String tokenID) {
		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", username))
				.setKind("AuthToken")
				.newKey(tokenID);

		datastore.delete(tokenKey);
	}

	private class ShowUsersResponse {
		public List<UsersData> users;

		public ShowUsersResponse(List<UsersData> users) {
			this.users = users;
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////
	// checks if token is valid first and then if user has permission as it
	// is in the exercise
	private Response checkToken(Entity tokenEntity, Role... allowedRoles) {
		if (tokenEntity == null) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.INVALID_TOKEN)))
					.build();
		}

		long expiresAt = tokenEntity.getLong("expiresAt");
		if (System.currentTimeMillis() > expiresAt) {
			removeToken(tokenEntity.getString("username"), tokenEntity.getString("tokenId"));
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.TOKEN_EXPIRED)))
					.build();
		}

		Role role = Role.valueOf(tokenEntity.getString("role"));
		for (Role allowedRole : allowedRoles) {
			if (role == allowedRole) {
				return null; // token is valid and has permission
			}
		}
		return Response.status(Status.OK)
				.entity(g.toJson(new FailedResponse(AppError.UNAUTHORIZED)))
				.build();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////

	@POST
	@Path("/deleteaccount")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteAccount(DeleteUserData data) {
		if (data == null || data.token == null || data.token.tokenID == null) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.INVALID_TOKEN)))
					.build();
		}
		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.token.username))
				.setKind("AuthToken")
				.newKey(data.token.tokenID);

		Entity tokenEntity = datastore.get(tokenKey);
		Response tokenCheck = checkToken(tokenEntity, Role.ADMIN);
		if (tokenCheck != null) {
			return tokenCheck; // stops here if token is invalid
		}

		Key userKey = userKeyFactory.newKey(data.input.username);
		Entity user = datastore.get(userKey);
		if (user == null) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.USER_NOT_FOUND)))
					.build();
		}

		// Admin cannot delete themselves
		if (data.input.username.equals(tokenEntity.getString("username"))) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.FORBIDDEN)))
					.build();
		}

		// Delete all tokens from this user that will be deleted
		Query<Key> tokensQuery = Query.newKeyQueryBuilder()
				.setKind("AuthToken")
				.setFilter(PropertyFilter.hasAncestor(userKey))
				.build();
		QueryResults<Key> tokens = datastore.run(tokensQuery);
		tokens.forEachRemaining(key -> datastore.delete(key));

		datastore.delete(userKey);
		return Response.ok(g.toJson(new SuccessResponse<>(new DeleteUserResponse()))).build();
	}

	private class DeleteUserResponse {
		public String message;

		public DeleteUserResponse() {
			this.message = "Account deleted successfully";
		}
	}

	@POST
	@Path("/modaccount")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response modAccount(ModUserData data) {
		if (data == null || data.token == null || data.token.tokenID == null) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.INVALID_TOKEN)))
					.build();
		}
		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.token.username))
				.setKind("AuthToken")
				.newKey(data.token.tokenID);

		Entity tokenEntity = datastore.get(tokenKey);

		Response tokenCheck = checkToken(tokenEntity, Role.ADMIN, Role.BOFFICER, Role.USER);
		if (tokenCheck != null) {
			return tokenCheck; // stops here if token is expired
		}

		Entity user = datastore.get(userKeyFactory.newKey(data.input.username));
		if (user == null) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.USER_NOT_FOUND)))
					.build();
		}
		Role targetRole = Role.valueOf(user.getString("role"));

		Role tokenRole = Role.valueOf(tokenEntity.getString("role"));
		if (tokenEntity.getString("role").equals(Role.USER.name())
				&& !data.input.username.equals(tokenEntity.getString("username"))) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.UNAUTHORIZED)))
					.build();
		}

		// users can only modify themselves
		if (tokenRole == Role.USER && !data.input.username.equals(tokenEntity.getString("username"))) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.UNAUTHORIZED)))
					.build();
		}

		// b officers can only modify users and themselfs
		if (tokenRole == Role.BOFFICER && targetRole != Role.USER
				&& !data.input.username.equals(tokenEntity.getString("username"))) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.UNAUTHORIZED)))
					.build();
		}

		Entity.Builder userBuilder = Entity.newBuilder(user);

		if (data.input.attributes.phone != null && !data.input.attributes.phone.isBlank()) {
			userBuilder.set("phone", data.input.attributes.phone);
		} else {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.INVALID_INPUT)))
					.build();
		}

		if (data.input.attributes.address != null && !data.input.attributes.address.isBlank()) {
			userBuilder.set("address", data.input.attributes.address);
		} else {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.INVALID_INPUT)))
					.build();
		}

		datastore.update(userBuilder.build());
		return Response.ok(g.toJson(new SuccessResponse<>(new ModUserResponse()))).build();
	}

	private class ModUserResponse {
		public String message;

		public ModUserResponse() {
			this.message = "Updated successfully";
		}
	}

	@POST
	@Path("/showauthsessions")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response showAuthenticatedSessions(ShowData data) {
		if (data == null || data.token == null || data.token.tokenID == null) {
			return Response.status(Status.OK)
					.entity(g.toJson(new FailedResponse(AppError.INVALID_TOKEN)))
					.build();
		}
		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.token.username))
				.setKind("AuthToken")
				.newKey(data.token.tokenID);

		Entity tokenEntity = datastore.get(tokenKey);
		Response tokenCheck = checkToken(tokenEntity, Role.ADMIN);
		if (tokenCheck != null) {
			return tokenCheck; // stops here if token is invalid
		}

		Query<Entity> query = Query.newEntityQueryBuilder()
				.setKind("AuthToken")
				.build();
		QueryResults<Entity> tokens = datastore.run(query);

		// ==================only valid tokens===========================//
		long now = System.currentTimeMillis();
		List<SessionsData> activeSessions = new ArrayList<>();
		while (tokens.hasNext()) {
			Entity token = tokens.next();
			if (now <= token.getLong("expiresAt")) {
				activeSessions.add(new SessionsData(token));
			} else {
				datastore.delete(token.getKey());
			}
		}

		return Response.ok(g.toJson(new SuccessResponse<>(new ShowSessionsResponse(activeSessions)))).build();

	}

	private class ShowSessionsResponse {
		public List<SessionsData> sessions;

		public ShowSessionsResponse(List<SessionsData> activeSessions) {
			this.sessions = activeSessions;
		}
	}
}

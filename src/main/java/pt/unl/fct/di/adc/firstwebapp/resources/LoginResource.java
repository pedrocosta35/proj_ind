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
import pt.unl.fct.di.adc.firstwebapp.util.FailedResponse;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;
import pt.unl.fct.di.adc.firstwebapp.util.Role;
import pt.unl.fct.di.adc.firstwebapp.util.ShowUsersData;
import pt.unl.fct.di.adc.firstwebapp.util.SuccessResponse;
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
public class LoginResource {

	private static final String MESSAGE_INVALID_CREDENTIALS = "Incorrect username or password.";
	private static final String MESSAGE_NEXT_PARAMETER_INVALID = "Request parameter 'next' must be greater or equal to 0.";

	private static final String LOG_MESSAGE_LOGIN_ATTEMP = "Login attempt by user: ";
	private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful by user: ";
	private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password for: ";
	private static final String USER_PWD = "user_pwd";
	private static final String USER_LOGIN_TIME = "user_login_time";

	/**
	 * Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	private final Gson g = new Gson();

	public LoginResource() {
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
				return Response.status(Status.NOT_FOUND)
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
						.set("token_id", token.tokenID)
						.set("username", token.username)
						.set("role", token.role.name())
						.set("issuedAt", Timestamp.of(new java.util.Date(token.issuedAt)))
						.set("expiresAt", Timestamp.of(new java.util.Date(token.expiresAt)))
						.build();

				// Batch operation
				txn.put(tokenEntity);
				txn.commit();

				// Return token
				LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.input.username);
				return Response
						.ok(g.toJson(new SuccessResponse<>(new loginResponse(token))))
						.build();
			} else {
				// Wrong password
				LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.input.username);
				return Response.status(Status.FORBIDDEN)
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

	private class loginResponse {
		public AuthToken token;

		public loginResponse(AuthToken token) {
			this.token = token;
		}
	}

	@POST
	@Path("/showusers")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response showUsers(ShowUsersData data) {
		if (data == null || data.token == null || data.token.tokenID == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(g.toJson(new FailedResponse(AppError.INVALID_TOKEN)))
					.build();
		}
		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.token.username))
				.setKind("AuthToken")
				.newKey(data.token.tokenID);

		Entity tokenEntity = datastore.get(tokenKey);
		if (tokenEntity == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(g.toJson(new FailedResponse(AppError.INVALID_TOKEN)))
					.build();
		}

		if (data.hasTokenExpired()) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(g.toJson(new FailedResponse(AppError.TOKEN_EXPIRED)))
					.build();
		}
		if (!data.hasPermission(Role.ADMIN, Role.BOFFICER)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(g.toJson(new FailedResponse(AppError.UNAUTHORIZED)))
					.build();
		}
		Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
		QueryResults<Entity> users = datastore.run(query);

		List<Entity> userList = new ArrayList<>();
		while (users.hasNext()) {
			Entity user = users.next();
			userList.add(user);
		}
		return Response.ok(g.toJson(new SuccessResponse<>(new showUsersResponse(userList)))).build();
	}

	private class showUsersResponse {
		public List<Entity> users;

		public showUsersResponse(List<Entity> users) {
			this.users = users;
		}
	}
}
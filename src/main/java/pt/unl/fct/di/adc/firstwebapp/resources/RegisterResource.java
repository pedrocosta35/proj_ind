package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;

import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;

@Path("/createaccount")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public RegisterResource() {
	} // Default constructor, nothing to do

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createAccount(RegisterData data) {
		LOG.fine("Attempt to register user: " + data.username);

		if (!data.validRegistration())
			return Response.status(9906).entity("Invalid input.").build();

		try {
			Transaction txn = datastore.newTransaction();
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);

			if (user != null) {
				txn.rollback();
				return Response.status(9901).entity("User already exists.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("username", data.username)
						.set("password", DigestUtils.sha512Hex(data.password))
						.set("adress", data.adress)
						.set("role", data.role.name())
						.set("user_creation_time", Timestamp.now())
						.build();
				txn.put(user);
				txn.commit();
				LOG.info("User registered " + data.username);
				return Response.ok(g.toJson(data.username + data.role)).build();
			}
		} catch (Exception e) {
			LOG.severe("Error registering user: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error registering user.").build();
		} finally {
			// No need to rollback here, as we only have one transaction and it will be
			// automatically rolled back if not committed.
		}
	}
}
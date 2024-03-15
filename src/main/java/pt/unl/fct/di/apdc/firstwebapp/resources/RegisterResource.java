package pt.unl.fct.di.apdc.firstwebapp.resources;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.logging.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Response;

//import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;

import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;



@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource{

    //logger object
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public RegisterResource() { } //nothing to be done here

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doRegister(RegisterData data) {
        LOG.fine("Attempt to register user: " + data.username);

        //Check ipnut data
        if(!data.isRegValid())
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing or wrong parameter").build();

        Key userKey = datastore.newKeyFactory().setKind("username").newKey(data.username);
        
        Entity person = Entity.newBuilder(userKey)
            .set("user_pwd", DigestUtils.sha512Hex(data.password))
            .set("user_creation_timestamp", Timestamp.now())
            .build();

        datastore.put(person);
        LOG.info("User registered: " + data.username);
        return Response.ok("{}").build(); //.entity(g.toJson("User registered")).build();
    }

    
}
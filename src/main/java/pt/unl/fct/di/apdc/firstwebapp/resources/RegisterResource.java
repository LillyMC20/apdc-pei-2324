package pt.unl.fct.di.apdc.firstwebapp.resources;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.logging.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;



@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource{

    //logger object
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public RegisterResource() { } //nothing to be done here

    @POST
    @Path("/rest/register/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doRegister(LoginData data) {
        
        Key userKey = datastore.newKeyFactory().setKind("Person").newKey("Lilly");
        
        Entity person = Entity.newBuilder(userKey)
        .set("email", "lilly@fct.unl.pt")
        .build();

        datastore.put(person);
        return Response.ok().entity(g.toJson("User registered")).build();
    }

    @GET
    @Path("/{username}")
    public Response checkUsernameAvailble(@PathParam("username") String username) {
        
        if (username.equals("lmc"))
            return Response.ok().entity(g.toJson(false)).build();
        else
            return Response.ok().entity(g.toJson(true)).build();
    }

    
}
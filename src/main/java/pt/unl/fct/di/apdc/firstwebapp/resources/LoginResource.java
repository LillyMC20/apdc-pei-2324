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

import com.google.gson.Gson;


@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource{

    //logger object
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    private final Gson g = new Gson();

    public LoginResource() { } //nothing to be done here

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data) {
        LOG.fine("Attempt to login user: " + data.username);

        if (data.username.equals("lmc") && data.password.equals("password"))
        {
            AuthToken at = new AuthToken(data.username);;
            return Response.ok(g.toJson(at)).build();
        }

        return Response.status(Response.Status.FORBIDDEN).entity("Invalid username or password").build();

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
package pt.unl.fct.di.apdc.firstwebapp.resources;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.util.logging.Logger;

import javax.ws.rs.POST;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

import com.google.gson.Gson;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;


@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource{

    //logger object
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    private final Gson g = new Gson();

    public LoginResource() { } //nothing to be done here

    @POST
    @Path("/v1")
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

    @POST
    @Path("/v2")
    @Consumes(MediaType.APPLICATION_JSON )
    @Produces (MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response doLogin2(LoginData data, @Context HttpServletRequest request, @Context HttpHeaders headers) {
        LOG.fine("Attempt to login user: " + data.username);

        //Keys should be genrated outside transaction
        //*Construct the key from the user
        Key userKey = userKeyFactory.newKey(data.username);
        Key crtsKey = datastore.newKeyFactory()
            .addAncestors(PathElement.of("username", data.username))
            .setKind("UserStats").newKey("counters");
        
        Key logKey = datastore.allocateId(
            datastore.newKeyFactory()
                .addAncestors(PathElement.of("username", data.username))
                .setKind("UserLog").newKey()
        );
        
        Transaction txn = datastore.newTransaction();

        try{
            Entity user = txn.get(userKey);

            if(user == null)
            {
                //*Username does not exist
                LOG.warning("Failed login attempt for username: " + data.username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            //*We get the user stats from the storage
            Entity userStats = txn.get(crtsKey);
            if(userStats == null)
            {
                userStats = Entity.newBuilder(crtsKey)
                    .set("user_stats_login", 0L)
                    .set("user_stats_failed", 0L)
                    .set("user_first_login",Timestamp.now())
                    .set("user_last_login", Timestamp.now())
                    .build();
            }

            String hashedPWD = user.getString("password");
            
            //*Pwd is correct
            if(hashedPWD.equals(DigestUtils.sha512Hex( data.password)))
            {
                //*Construct logs
                Entity log = Entity.newBuilder(logKey)
                    .set("user_log_ip", request.getRemoteAddr())
                    .set("user_log_host", request.getRemoteHost())
                    .set("user_log_latLon", StringValue.newBuilder(headers.getHeaderString("X-AppEngine-CityLatLong"))
                            .setExcludeFromIndexes(true).build())
                    .set("user_log_city", headers.getHeaderString("X-AppEngine-City"))
                    .set("user_log_country", headers.getHeaderString("X-AppEngine-Country"))
                    .set("user_log_time", Timestamp.now())
                    .build();

                //*Update user stats
                Entity uStats = Entity.newBuilder(crtsKey)
                    .set("user_stats_login", 01L + userStats.getLong("user_stats_login"))
                    .set("user_stats_failed", 0L)
                    .set("user_first_login",userStats.getTimestamp("user_first_login"))
                    .set("user_last_login", Timestamp.now())
                    .build();

                txn.put(log, uStats);
                txn.commit();

                //*Return the token
                AuthToken at = new AuthToken(data.username);
                LOG.info("User: " + data.username + " logged in successfully");
                return Response.ok(g.toJson(at)).build();
            }
            else{

            //*Incorrect Password
            Entity uStats = Entity.newBuilder(crtsKey)
                .set("user_stats_login", userStats.getLong("user_stats_login"))
                .set("user_stats_failed", 1L + userStats.getLong("user_stats_failed"))
                .set("user_first_login",userStats.getTimestamp("user_first_login"))
                .set("user_last_login", userStats.getTimestamp("user_last_login"))
                .set("user_last_attempt", Timestamp.now())
                .build();

                txn.put(uStats);
                txn.commit();
                LOG.warning("Failed login attempt for username: " + data.username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }catch(Exception e){

            txn.rollback();
            LOG.severe(e.getMessage()); 
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }finally{
            if(txn.isActive())
                txn.rollback();

        }
    }

    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces (MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response seeLoginTime(LoginData data) {
        LOG.fine("Attempt to see user login timestamp: " + data.username);

        //*Userkey to find user */
        Key userKey = userKeyFactory.newKey(data.username);
       
        //*Userkey to find proprieties */
        Key crtsKey = datastore.newKeyFactory()
        .addAncestors(PathElement.of("username", data.username))
        .setKind("UserStats").newKey("counters");
    

        Transaction txn = datastore.newTransaction();

        try{
            Entity user = txn.get(userKey);

            if(user == null)
            {
                //*Username does not exist
                LOG.warning("Username does not exits: " + data.username);
                return Response.status(Response.Status.FORBIDDEN).build() ;
            }

            Entity userStats = txn.get(crtsKey);

            String hashedPWD = user.getString("password");
            
            //*Pwd is correct
            if(hashedPWD.equals(DigestUtils.sha512Hex( data.password)))
            {
                
                //!We get the user stats from the storage?
                String uTime = userStats.getProperties().get("user_stats_login").toString();
                return Response.ok(g.toJson(uTime)).build();
            }
            else{
                //*Incorrect Password
                Entity uStats = Entity.newBuilder(crtsKey)
                .set("user_stats_login", userStats.getLong("user_stats_login"))
                .set("user_stats_failed", 1L + userStats.getLong("user_stats_failed"))
                .set("user_first_login",userStats.getTimestamp("user_first_login"))
                .set("user_last_login", userStats.getTimestamp("user_last_login"))
                .set("user_last_attempt", Timestamp.now())
                .build();

                txn.put(uStats);
                txn.commit();
                LOG.warning("Failed login attempt for username: " + data.username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }

        }catch(Exception e){

            txn.rollback();
            LOG.severe(e.getMessage()); 
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }finally{
            if(txn.isActive())
                txn.rollback();

        }
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
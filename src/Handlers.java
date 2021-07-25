package net.redpwn;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.InternalServerErrorResponse;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONObject;

public class Handlers {
  public static void createUser(Context ctx) {
    String username = (String)ctx.queryParam("username", String.class).get();
    String password = (String)ctx.queryParam("password", String.class).get();
    try {
      Main.db.createDatabase(username);
      Main.db.createUser(username, password);
      Main.db.addUserToDatabase(username, username);
      JSONObject flagDoc = new JSONObject();
      flagDoc.put("flag", Main.flag);
      Main.db.insertDocumentToDatabase(username, flagDoc.toString());
      ctx.result("success");
    } catch (Exception e) {
      throw new InternalServerErrorResponse("Something went wrong");
    } 
  }
  
  public static void testAPI(Context ctx) {
    String url = (String)ctx.queryParam("url", String.class).get();
    String method = (String)ctx.queryParam("method", String.class).get();
    String data = ctx.queryParam("data");
    try {
      URL urlURI = new URL(url);
      if (urlURI.getHost().contains("couchdb"))
        throw new ForbiddenResponse("Illegal!"); 
    } catch (MalformedURLException e) {
      throw new BadRequestResponse("Input URL is malformed");
    } 
    try {
      if (method.equals("GET")) {
        JSONObject jsonObj = HttpClient.getAPI(url);
        String str = jsonObj.toString();
      } else if (method.equals("POST")) {
        JSONObject jsonObj = HttpClient.postAPI(url, data);
        String stringJsonObj = jsonObj.toString();
        if (Utils.containsFlag(stringJsonObj))
          throw new ForbiddenResponse("Illegal!"); 
      } else {
        throw new BadRequestResponse("Request method is not accepted");
      } 
    } catch (Exception e) {
      throw new InternalServerErrorResponse("Something went wrong");
    } 
    ctx.result("success");
  }
}

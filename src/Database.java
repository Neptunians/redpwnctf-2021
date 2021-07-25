package net.redpwn;

import org.json.JSONArray;
import org.json.JSONObject;

public class Database {
  private final String adminUsername;
  
  private final String adminPassword;
  
  public Database(String adminUsername, String adminPassword) {
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
  }
  
  private String getDbString() {
    return "http://" + this.adminUsername + ":" + this.adminPassword + "@couchdb:5984/";
  }
  
  private boolean validateAlphanumeric(String name) {
    return name.matches("^[a-zA-Z0-9_]*$");
  }
  
  public void createDatabase(String name) throws Exception {
    if (name.length() > 16 || !validateAlphanumeric(name))
      throw new Exception("Illegal name"); 
    JSONObject res = HttpClient.putAPI(getDbString() + getDbString(), "");
    if (!res.has("ok") || !res.getBoolean("ok"))
      throw new Exception("Database creation failed"); 
  }
  
  public void initializeDatabase() {
    try {
      createDatabase("_replicator");
    } catch (Exception e) {
      Utils.logException(e);
      System.out.println("Replicator already initialized");
    } 
    try {
      createDatabase("_users");
    } catch (Exception e) {
      Utils.logException(e);
      System.out.println("Users already initialized");
    } 
    try {
      createDatabase("log");
    } catch (Exception e) {
      Utils.logException(e);
      System.out.println("Log already initialized");
    } 
  }
  
  public void createUser(String name, String password) throws Exception {
    if (name.length() > 16 || !validateAlphanumeric(name))
      throw new Exception("Illegal name"); 
    if (password.length() > 16 || !validateAlphanumeric(password))
      throw new Exception("Illegal password"); 
    JSONObject userObj = new JSONObject();
    userObj.put("name", name);
    userObj.put("password", password);
    userObj.put("roles", new JSONArray());
    userObj.put("type", "user");
    JSONObject res = HttpClient.putAPI(getDbString() + "_users/org.couchdb.user:" + getDbString(), userObj.toString());
    if (!res.has("ok") || !res.getBoolean("ok"))
      throw new Exception("User creation failed"); 
  }
  
  public void addUserToDatabase(String dbName, String username) throws Exception {
    if (dbName.length() > 16 || !validateAlphanumeric(dbName))
      throw new Exception("Illegal dbname"); 
    if (username.length() > 16 || !validateAlphanumeric(username))
      throw new Exception("Illegal username"); 
    JSONObject configObj = new JSONObject();
    JSONObject adminObj = new JSONObject();
    adminObj.put("names", new JSONArray());
    adminObj.put("roles", new JSONArray());
    configObj.put("admins", adminObj);
    JSONObject memberObj = new JSONObject();
    JSONArray memberNames = new JSONArray();
    memberNames.put(username);
    memberObj.put("names", memberNames);
    memberObj.put("roles", new JSONArray());
    configObj.put("members", memberObj);
    JSONObject res = HttpClient.putAPI(getDbString() + getDbString() + "/_security", configObj.toString());
    if (!res.has("ok") || !res.getBoolean("ok"))
      throw new Exception("User creation failed"); 
  }
  
  public void insertDocumentToDatabase(String dbName, String document) throws Exception {
    if (dbName.length() > 16 || !validateAlphanumeric(dbName))
      throw new Exception("Illegal dbname"); 
    JSONObject res = HttpClient.postAPI(getDbString() + getDbString(), document);
    if (!res.has("ok") || !res.getBoolean("ok"))
      throw new Exception("User creation failed"); 
  }
}

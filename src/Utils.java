package net.redpwn;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.json.JSONObject;

public class Utils {
  public static boolean containsFlag(String text) {
    return text.contains(Main.flag);
  }
  
  private static void remoteLog(String message) {
    JSONObject logObj = new JSONObject();
    logObj.put("message", message);
    try {
      Main.db.insertDocumentToDatabase("log", logObj.toString());
    } catch (Exception e) {
      System.out.println("Failed to add message to remote log");
    } 
  }
  
  public static void logException(Exception e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    String exceptionAsString = sw.toString();
    System.out.println(exceptionAsString);
  }
  
  public static void logException(String e) {
    System.out.println(e);
  }
}

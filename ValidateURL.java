import java.net.URL;
import java.net.MalformedURLException;

public class ValidateURL {

  public static void main(String[] args) {
    String url = args[0];
    System.out.println("Input: '" + url + "'");
    try {
      URL urlURI = new URL(url);
      String urlHost = urlURI.getHost();
      System.out.println("====> How java.net.URL understood it:");
      System.out.println("Host: '" + urlHost + "'");
      System.out.println("Protocol: '" + urlURI.getProtocol() + "'");
      System.out.println("Authority: '" + urlURI.getAuthority() + "'");
      System.out.println("Path: '" + urlURI.getPath() + "'");
      System.out.println("Port: '" + Integer.toString(urlURI.getPort()) + "'");
      System.out.println("Query: '" + urlURI.getQuery() + "'");
      System.out.println("Ref/Anchor: '" + urlURI.getRef() + "'");
      System.out.println("User Info: '" + urlURI.getUserInfo() + "'");
      
      if (urlURI.getHost().contains("couchdb"))
        System.out.println("Illegal!"); 
    } catch (MalformedURLException e) {
      System.out.println("Input URL is malformed");
    } 
  }
}
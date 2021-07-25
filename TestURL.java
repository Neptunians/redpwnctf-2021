import java.net.URL;
import java.net.MalformedURLException;

public class TestURL {

    public static void main(String[] args) throws Exception {
        String url = args[0];

        System.out.println(url);

        try {
            URL urlURI = new URL(url);

            System.out.println("Host: " + urlURI.getHost());

            if (urlURI.getHost().contains("couchdb"))
                throw new Exception("Illegal!"); 
        } catch (MalformedURLException e) {
            String message = e.getMessage();
            System.out.println("Input URL is malformed: " + message);
        } 
    }

    
    
}
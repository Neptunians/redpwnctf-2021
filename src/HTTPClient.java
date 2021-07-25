package net.redpwn;

import java.io.IOException;
import java.util.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class HttpClient {
  public static CloseableHttpClient httpclient = HttpClients.custom().disableRedirectHandling().build();
  
  private static String extractBasicAuth(String url) {
    if (url.startsWith("http://")) {
      url = url.substring(7);
    } else if (url.startsWith("https://")) {
      url = url.substring(8);
    } 
    int atIdx = url.indexOf('@');
    if (atIdx == -1)
      return null; 
    String authStr = url.substring(0, atIdx);
    int colonIdx = authStr.indexOf(':');
    if (colonIdx == -1)
      return null; 
    String username = authStr.substring(0, colonIdx);
    String password = authStr.substring(colonIdx + 1, atIdx);
    return basicAuth(username, password);
  }
  
  private static String basicAuth(String username, String password) {
    return "Basic " + Base64.getEncoder().encodeToString((username + ":" + username).getBytes());
  }
  
  public static JSONObject getAPI(String url) throws Exception {
    HttpGet httpGet = new HttpGet(url);
    String basicAuth = extractBasicAuth(url);
    if (basicAuth != null)
      httpGet.setHeader("Authorization", basicAuth); 
    try {
      CloseableHttpResponse response = httpclient.execute((HttpUriRequest)httpGet);
      int responseCode = response.getStatusLine().getStatusCode();
      HttpEntity responseEntity = response.getEntity();
      String stringResponseEntity = EntityUtils.toString(responseEntity);
      EntityUtils.consume(responseEntity);
      response.close();
      if (responseCode / 100 != 2)
        throw new IOException("Request was not successful URL: " + url + " Status: " + responseCode); 
      return new JSONObject(stringResponseEntity);
    } catch (IOException e) {
      Utils.logException(e);
      throw new Exception("Something went wrong");
    } catch (JSONException e) {
      Utils.logException((Exception)e);
      throw new Exception("API must have JSON response");
    } 
  }
  
  public static JSONObject postAPI(String url, String data) throws Exception {
    HttpPost httpPost = new HttpPost(url);
    String basicAuth = extractBasicAuth(url);
    if (basicAuth != null)
      httpPost.setHeader("Authorization", basicAuth); 
    try {
      StringEntity requestEntity = new StringEntity(data, ContentType.APPLICATION_JSON);
      httpPost.setEntity((HttpEntity)requestEntity);
      CloseableHttpResponse response = httpclient.execute((HttpUriRequest)httpPost);
      int responseCode = response.getStatusLine().getStatusCode();
      HttpEntity responseEntity = response.getEntity();
      String stringResponseEntity = EntityUtils.toString(responseEntity);
      EntityUtils.consume(responseEntity);
      response.close();
      if (responseCode / 100 != 2)
        throw new IOException("Request was not successful URL: " + url + " Status: " + responseCode); 
      return new JSONObject(stringResponseEntity);
    } catch (IOException e) {
      Utils.logException(e);
      throw new Exception("Something went wrong");
    } catch (JSONException e) {
      Utils.logException((Exception)e);
      throw new Exception("API must have JSON response");
    } 
  }
  
  public static JSONObject putAPI(String url, String data) throws Exception {
    HttpPut httpPut = new HttpPut(url);
    String basicAuth = extractBasicAuth(url);
    if (basicAuth != null)
      httpPut.setHeader("Authorization", basicAuth); 
    try {
      StringEntity requestEntity = new StringEntity(data, ContentType.APPLICATION_JSON);
      httpPut.setEntity((HttpEntity)requestEntity);
      CloseableHttpResponse response = httpclient.execute((HttpUriRequest)httpPut);
      int responseCode = response.getStatusLine().getStatusCode();
      HttpEntity responseEntity = response.getEntity();
      String stringResponseEntity = EntityUtils.toString(responseEntity);
      EntityUtils.consume(responseEntity);
      response.close();
      if (responseCode / 100 != 2)
        throw new IOException("Request was not successful URL: " + url + " Status: " + responseCode); 
      return new JSONObject(stringResponseEntity);
    } catch (IOException e) {
      Utils.logException(e);
      throw new Exception("Something went wrong");
    } catch (JSONException e) {
      Utils.logException((Exception)e);
      throw new Exception("API must have JSON response");
    } 
  }
}

package fb.notify;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.InetSocketAddress;

import java.util.Iterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class FBNotificationServer {

    private static final int SERVER_PORT = 8766;

    public static void main(String[] args) throws IOException {

        InetSocketAddress addr = new InetSocketAddress(SERVER_PORT);
        HttpServer server = HttpServer.create(addr, 0);

        server.createContext("/", new MyHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Server is listening on port " + SERVER_PORT);
    }
}

class MyHandler implements HttpHandler {

    private static final int HTTP_OK = 200;
    
    public void handle(HttpExchange exchange) throws IOException {

        String requestMethod = exchange.getRequestMethod();

        if (requestMethod.equalsIgnoreCase("GET")) {
            System.out.println("Received Http Get request");
            handleGet(exchange);
        } else if(requestMethod.equalsIgnoreCase("POST")) {
            System.out.println("Received  Http post request");
            handlePost(exchange);
        }
    }

    private void handleGet(HttpExchange exchange) {

        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/plain");
     
        try {

            Headers requestHeaders     = exchange.getRequestHeaders();
            OutputStream responseBody  = exchange.getResponseBody();
            Map<String, Object> params = null;

            params = parseGetParameters(exchange);
            printParams(params);

            // Parse verification request
            String mode      = (String) params.get("hub.mode");
            String token     = (String) params.get("hub.verify_token");
            String challenge = (String) params.get("hub.challenge");

            exchange.sendResponseHeaders(HTTP_OK, challenge.length());

            if(mode.equals("subscribe") && token.equals("readypulse")) {
                System.out.println("Setting response with body: " + challenge);
                responseBody.write(challenge.getBytes());
            }

            responseBody.close();

        } catch(Exception e) {
            System.out.println("Exception occurred while processing GET request" + e);
            return;
        } finally {
            exchange.close();
        }
  }

  private void handlePost(HttpExchange exchange) throws IOException {
      try {

          InputStreamReader streamReader = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
          BufferedReader reader          = new BufferedReader(streamReader);

          String query = reader.readLine();

          exchange.sendResponseHeaders(HTTP_OK, 0);

          handleNotification(query);
        
      } catch(Exception e) {
          System.out.println("Error occurred while handling post " + e);
      }
  }

  private Map<String, Object> 
          parseGetParameters(HttpExchange exchange) throws Exception {

      Map<String, Object> parameters = new HashMap<String, Object>();

      URI requestedUri = exchange.getRequestURI();
      String query     = requestedUri.getRawQuery();

      parseQuery(query, parameters);

      return parameters;
  }
  
  private void parseQuery(
          String query, Map<String, Object> parameters) throws Exception {

      if (query != null) {

          String pairs[] = query.split("[&]");

          for (String pair : pairs) {

              String param[] = pair.split("[=]");
              String key     = null;
              String value   = null;

              if (param.length > 0) {
                  key = URLDecoder.decode(param[0],"UTF-8");
              }

              if (param.length > 1) {
                  value = URLDecoder.decode(param[1],"UTF-8");
              }

              if (parameters.containsKey(key)) {

                  Object obj = parameters.get(key);
                  if(obj instanceof List<?>) {
                      List<String> values = (List<String>)obj;
                      values.add(value);
                  } else if(obj instanceof String) {
                      List<String> values = new ArrayList<String>();
                      values.add((String)obj);
                      values.add(value);
                      parameters.put(key, values);
                  }

             } else {
                  parameters.put(key, value);
             }
          }
      }
  }

  private void handleNotification(String notification) {
      try {

          JSONObject jsonObj = new JSONObject(notification);
          System.out.println("Notification:" + jsonObj);

      } catch(Exception e) {
          System.out.println("Exception while parsing JSON object " + notification);
      }
  }

  private void printParams(Map<String, Object> params) {
      for(Map.Entry<String, Object> entry : params.entrySet()) {
          System.out.println(entry.getKey() + ": " + (String) entry.getValue());
      }
  }
}

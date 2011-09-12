package fb.notify;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.apache.http.client.ResponseHandler;

import org.apache.http.client.entity.UrlEncodedFormEntity;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.util.EntityUtils;

import org.apache.http.protocol.HTTP;

import org.json.JSONObject;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author venkat
 */
public class SubscriptionManager {

    private static final String VERIFY_TOKEN = "readypulse";
    private static final String BASE_URI     = "https://graph.facebook.com";
    private static final String API_NAME     = "subscriptions";
    private static final String ACCESS_TOKEN =
            "180138355391952|l5www_hkYa-ofwnLlm0DjgUFZTE";

    private final DefaultHttpClient       httpClient;
    private final ResponseHandler<String> responseHandler;

    public SubscriptionManager() {
        httpClient      = new DefaultHttpClient();
        responseHandler = new BasicResponseHandler();
    }

    public boolean add(
            String appId,
            String object,
            String fields,
            String callbackUrl) {

        String postUri = BASE_URI + "/" + appId + "/" + API_NAME;

        try {

            HttpPost postReq          = new HttpPost(postUri);
            List <NameValuePair> nvps = new ArrayList <NameValuePair>();

            nvps.add(new BasicNameValuePair("access_token", ACCESS_TOKEN));
            nvps.add(new BasicNameValuePair("object",       object));
            nvps.add(new BasicNameValuePair("fields",       fields));
            nvps.add(new BasicNameValuePair("callback_url", callbackUrl));
            nvps.add(new BasicNameValuePair("verify_token", VERIFY_TOKEN));

            postReq.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            System.out.println("Sending subscription request for object " + object);

            HttpResponse response = httpClient.execute(postReq);

            System.out.println("Got response for subscription:" + response);

            EntityUtils.consume(response.getEntity());

        } catch(Exception e) {
            throw new RuntimeException("Error adding subscription:", e);
        }

        return true;
    }

    public String list(String appId) {

        String responseBody = null;

        try {

            String getUri = BASE_URI + "/" + appId + "/" + API_NAME + "?";

            getUri = appendHttpParameter(getUri, "access_token", URLEncoder.encode(ACCESS_TOKEN, "UTF-8"));

            System.out.println("Sending request " + getUri);
            
            HttpGet getRequest  = new HttpGet(getUri);
            responseBody        = httpClient.execute(getRequest, responseHandler);

        } catch(Exception ex) {
            throw new RuntimeException("Error getting subscription list for app " + appId,  ex);
        }

        return responseBody;
    }

    public int delete(String appId, String object) {

        String responseBody = null;

        try {

            String deleteUri = BASE_URI + "/" + appId + "/" + API_NAME ;                                      
            
            System.out.println("Sending delete request " + deleteUri);

            HttpPost postReq          = new HttpPost(deleteUri);
            List <NameValuePair> nvps = new ArrayList <NameValuePair>();

            nvps.add(new BasicNameValuePair("method",       "delete"));
            nvps.add(new BasicNameValuePair("access_token", ACCESS_TOKEN));

            if(object != null) {
                nvps.add(new BasicNameValuePair("object", object));
            }

            postReq.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            
            HttpResponse response = httpClient.execute(postReq);

            System.out.println("Response for delete:" + response.getStatusLine());

            return response.getStatusLine().getStatusCode();

        } catch(Exception ex) {
            throw new RuntimeException("Error sending deletion request for " + appId,  ex);
        }
    }

    public String appendHttpParameter(String uri, String key, String value) {
        return uri + key + "=" + value;
    }

    public static void main(String[] args) throws Exception {

        SubscriptionManager manager = new SubscriptionManager();

        System.out.println("Subscribing...");
        manager.add("100002817666087", "user", "feed,likes", "http://vsrinivasan.dyndns-home.com:8766");

        String response = manager.list("100002817666087");

        if(response != null) {
            JSONObject jsonObj = new JSONObject(response);
            System.out.println(jsonObj);
        }

        manager.delete("100002817666087", null);        
    }
}

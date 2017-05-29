package nl.wotuu.jwebrequest;



import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.Header;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.naming.Context;

/**
 * Created by Wouter on 07/06/13.
 */
public abstract class WebRequest {

    /**
     * Counter containing the request count.
     */
    private static long REQUEST_COUNT = 1L;
    /**
     * The base URL that should be called.
     */
    private String baseUrl;
    /**
     * The mapping containing all headers that should be set.
     */
    protected Map<String, String> headers = new HashMap<>();
    /**
     * The mapping containing all normal parameters.
     */
    protected Map<String, String> parameters = new HashMap<>();
    /**
     * The mapping containing all body parameters.
     */
    protected Map<String, Object> body = new HashMap<>();
    /**
     * The HTTP method to use. Default is GET.
     */
    public HttpMethod method = HttpMethod.GET;
    /**
     * The post entity of the request.
     */
    public PostEntity postEntity = PostEntity.UrlEncoded;
    /**
     * The context that spawned the request (Toast messages).
     */
    protected Context context;
    /**
     * Flag indicating if the request is allowed to login again if there was an
     * error message that said so.
     */
    public Boolean mayRelogin;
    /**
     * Flag to indicate if any error message by the server should be popped up.
     */
    public Boolean mayPopupError;
    /**
     * The amount of failed attempts.
     */
    private int failedAttempts;

    /**
     * Enum indicating the post entity.
     */
    public enum PostEntity {

        /**
         * URLEncoded will URL encode all post parameters before adding them.
         */
        UrlEncoded,
        /**
         * String will NOT encode post parameters before adding them.
         */
        String
    }

    public WebRequest(String baseUrl) {
        this.baseUrl = baseUrl;

        this.initialize();
    }

    /**
     * Initializes the request with default headers, and sets default values.
     */
    private void initialize() {
        this.parameters = new LinkedHashMap<String, String>();
        this.body = new LinkedHashMap<String, Object>();
        this.mayRelogin = true;
        this.mayPopupError = false;
    }

    /**
     * Add a parameter to the mPrioritizedRequest.
     *
     * @param method The HttpMethod to use.
     * @param key The key to use.
     * @param value The value to set.
     */
    protected void addParameter(HttpMethod method, String key, Object value) {
        if (key.equals("Content-Length")) {
            throw new IllegalArgumentException("Content-Length is set upon the execution of the mPrioritizedRequest. Don't set it here.");
        }

        switch (method) {
            case GET:
                this.parameters.put(key, String.valueOf(value));
                break;
            default:
                this.body.put(key, value);
                break;
        }
    }

    /**
     * Adds a header to the mPrioritizedRequest.
     *
     * @param key The key to add.
     * @param value The value to set.
     */
    protected void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    /**
     * Removes a header, preventing it from being sent to the server.
     *
     * @param header The header that shouldn't be sent.
     */
    protected void removeHeader(String header) {
        this.headers.remove(header);
    }

    /**
     * Executes this mPrioritizedRequest.
     *
     * @param resource The second part of the URL containing the resource you'd
     * like to get.
     * @returns The response object, containing headers and the response string
     * (body).
     */
    protected WebResponse execute(String resource) {        
        try {
            System.out.println("Executing " + resource);
            // Create an instance of HttpClient.
            HttpClient client = this.createHttpClient();


            REQUEST_COUNT++;

            // Get the URL string
            StringBuilder parametersStringBuilder = new StringBuilder();
            int count = 0;
            for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
                parametersStringBuilder.append(entry.getKey() + "=" + entry.getValue());
                if (count != this.parameters.size() - 1) {
                    parametersStringBuilder.append("&");
                }

                count++;
            }

            // Create connection
            String parametersString = ((parametersStringBuilder.length() > 0) ? "?" : "") + parametersStringBuilder.toString();

            HttpRequestBase method;
            String bodyParameterString = "";
            if (this.method == HttpMethod.GET) {
                // Create a method instance.
                method = new HttpGet(this.baseUrl + resource + parametersString);
            } else {
                method = new HttpPost(this.baseUrl + resource + parametersString);

                try {
                    HttpPost postMethod = (HttpPost) method;
                    if (postEntity == PostEntity.UrlEncoded) {
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                        List<String> stringEntityList = new ArrayList<String>();

                        for (Map.Entry<String, Object> entry : this.body.entrySet()) {
                            nameValuePairs.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
                            stringEntityList.add(URLEncoder.encode(entry.getKey()) + "=" + URLEncoder.encode(String.valueOf(entry.getValue())));
                        }

                        postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        bodyParameterString = String.join("&", stringEntityList);
                    } else if (postEntity == PostEntity.String) {
                        List<String> stringEntityList = new ArrayList<String>();

                        for (Map.Entry<String, Object> entry : this.body.entrySet()) {
                            stringEntityList.add(entry.getKey() + "=" + entry.getValue());
                        }

                        postMethod.setEntity(new StringEntity(bodyParameterString = String.join("&", stringEntityList)));
                    }
                } catch (IOException e) {
                }
            }

            // Add the version of this request
            // this.addHeader("clientv", "1.5.0");

            for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                method.addHeader(entry.getKey(), entry.getValue());
            }

            //Get Response
            HttpResponse response = client.execute(method);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[16];
            int read = 0;
            InputStream is = response.getEntity().getContent();
            // Check if it is a GZIP response or not
            if (this.isGZIPResponse(response.getAllHeaders())) {
                // It is, use a GZIPInputStream to parse the data
                GZIPInputStream gzipIS = new GZIPInputStream(is);
                while ((read = gzipIS.read(buffer)) > 0) {
                    if (read < 16) {
                        buffer = Arrays.copyOf(buffer, read);
                    }
                    outputStream.write(buffer);
                }
                gzipIS.close();
            } else {
                // Use a buffered input stream instead
                BufferedInputStream bufferedIS = new BufferedInputStream(is);
                while ((read = bufferedIS.read(buffer)) > 0) {
                    if (read < 16) {
                        buffer = Arrays.copyOf(buffer, read);
                    }
                    outputStream.write(buffer);
                }
                bufferedIS.close();
            }

            byte[] responseBytes = outputStream.toByteArray();
            String responseString = new String(responseBytes);

            int httpStatusCode = response.getStatusLine().getStatusCode();
            WebResponse.Result result = WebResponse.Result.Success;
            // If we should log in again ..
            if (httpStatusCode != 200) {
                System.out.println("Returned status is not success but " + httpStatusCode);

                result = WebResponse.Result.Error;

                // If this request may popup an error about the response
                System.out.println("Executing " + resource + "  exception. Status code was not 200 but " + httpStatusCode + ".");
            }

            // Create the response object
            WebResponse responseObj = new WebResponse(result, response.getAllHeaders(), httpStatusCode, responseBytes, responseString);

            // Cleanup
            is.close();
            outputStream.close();
            System.out.println("Executing " + resource + " success");

            return responseObj;
        } catch (ConnectTimeoutException e) {
            REQUEST_COUNT--;
            System.err.println("Executing " + resource + " exception, connection timed out.");

            return new WebResponse(WebResponse.Result.NoInternetConnection, null, 998, null, null);
        } catch (Exception e) {
            REQUEST_COUNT--;

            System.err.println("Executing " + resource + " exception, unknown exception.");

            return new WebResponse(WebResponse.Result.Error, null, 998, null, null);
        }
    }

    /**
     * Check if a response is GZIP encoded or not.
     *
     * @param responseHeaders The responses headers.
     * @return True if GZIP, false otherwise.
     */
    private Boolean isGZIPResponse(Header[] responseHeaders) {
        for (Header header : responseHeaders) {
            if (header.getName().equals("Content-Encoding") && header.getValue().equals("gzip")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an HTTP client with some default parameters set.
     *
     * @return The setup HTTP client.
     */
    private HttpClient createHttpClient() {
        BasicHttpParams localBasicHttpParams = new BasicHttpParams();
        HttpProtocolParams.setVersion(localBasicHttpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(localBasicHttpParams, "ISO-8859-1");
        HttpProtocolParams.setUseExpectContinue(localBasicHttpParams, true);
        HttpConnectionParams.setConnectionTimeout(localBasicHttpParams, 20000);
        ConnManagerParams.setMaxTotalConnections(localBasicHttpParams, 50);
        ConnManagerParams.setMaxConnectionsPerRoute(localBasicHttpParams, new ConnPerRouteBean(20));
        localBasicHttpParams.setIntParameter("http.connection.timeout", 20000);
        localBasicHttpParams.setIntParameter("http.socket.timeout", 20000);
        localBasicHttpParams.setParameter("http.protocol.cookie-policy", "best-match");
        //localBasicHttpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost("127.0.0.1", 8888, "http"));
        //localBasicHttpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost("wofje.8s.nl", 3128, "http"));
        
        return new DefaultHttpClient(localBasicHttpParams);
    }

    /**
     * Execute this request for a response (WARNING: blocks the current thread!)
     *
     * @return The reponse.
     */
    public abstract WebResponse execute();

    /**
     * Get a description of this request.
     *
     * @return The description.
     */
    public abstract String getDescription();
}
package nl.wotuu.jwebrequest;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;

/**
 * Created by Wouter on 07/06/13.
 */
public class WebResponse {

    public enum Result {
        Success,
        NoInternetConnection,
        Error,
        UnknownError,
        LoginAgain
    }

    /**
     * The result of the response.
     */
    public Result result;

    /**
     * The headers that were sent from the response.
     */
    public Header[] headers;

    /**
     * The HTTP status code that was received from the server.
     */
    public int httpStatusCode;

    /**
     * The raw bytes of the response.
     */
    public byte[] responseBytes;

    /**
     * The string response.
     */
    public String response;

    public WebResponse(Result result, Header[] headers, int httpStatusCode, byte[] responseBytes, String response) {
        this.result = result;

        this.headers = headers;
        this.httpStatusCode = httpStatusCode;
        this.responseBytes = responseBytes;
        this.response = response;
    }

    /**
     * Converts the received headers to headers
     * @param headerArr
     * @return
     */
    private Map<String, List<String>> convertHeaders(Header[] headerArr){
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        for( Header h : headerArr){
            List<String> stringList = new ArrayList<String>();
            stringList.add(h.getValue());

            result.put(h.getName(), stringList);
        }

        return result;
    }
}
package nl.wotuu.jwebrequest;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author wouter.koppenol
 */
public class CustomWebRequest extends WebRequest {

    public CustomWebRequest(String fullURL) {
        super(fullURL);
    }

    @Override
    public WebResponse execute() {
        return super.execute("");
    }

    @Override
    public String getDescription() {
        return "A custom request to any web address";
    }
    
}

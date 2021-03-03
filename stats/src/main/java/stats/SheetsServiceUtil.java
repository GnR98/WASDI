//original code from this tutorial:
//https://www.baeldung.com/google-sheets-java-client

/**
* @author c.nattero
*
*/

package stats;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;



public class SheetsServiceUtil {
	
	private SheetsServiceUtil() {
		throw new IllegalStateException("SheetsServiceUtil is just a utility class, do not construct it, please");
	}

    private static final String APPLICATION_NAME = "Google Sheets Example";

    public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
    	return getSheetsService(null);
    }
    
    public static Sheets getSheetsService(String sKeyFile) throws IOException, GeneralSecurityException {
        Credential credential = GoogleAuthorizeUtil.authorize(sKeyFile);
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential).setApplicationName(APPLICATION_NAME).build();
    }

}

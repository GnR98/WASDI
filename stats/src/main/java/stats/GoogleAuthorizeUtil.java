//original code from this tutorial:
//https://www.baeldung.com/google-sheets-java-client

/**
 * @author c.nattero
 *
 */

package stats;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;


public class GoogleAuthorizeUtil {
	public static Credential authorize() throws IOException, GeneralSecurityException {
		Path oWorkingDirectory = Paths.get(System.getProperty("user.dir")); 
		String sOAuthPath = oWorkingDirectory.resolve("resources").resolve("client_secret.json").toAbsolutePath().normalize().toString();
		InputStream in = new FileInputStream(sOAuthPath);
		//InputStream in = GoogleAuthorizeUtil.class.getResourceAsStream("/google-sheets-client-secret.json");
		//InputStream in = GoogleAuthorizeUtil.class.getResourceAsStream("/My First Project-ca3e6bdef60d.json");

		NetHttpTransport oTransport = GoogleNetHttpTransport.newTrustedTransport();
		JacksonFactory oJacksonFactory = JacksonFactory.getDefaultInstance();
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				oJacksonFactory,
				new InputStreamReader(in)
				);

		List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);		
		GoogleAuthorizationCodeFlow flow = null;
		Credential credential = null;
		try {
			flow = new GoogleAuthorizationCodeFlow.Builder(
					oTransport,
					oJacksonFactory,
					clientSecrets,
					scopes
					).setDataStoreFactory(new MemoryDataStoreFactory())
					.setAccessType("offline").build();
			
			credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		

		return credential;
	}

}

/**
 * Created by Cristiano Nattero on 2019-02-07
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

/**
 * @author c.nattero
 *
 */
public class AuthenticationCredentials {
	String sUser;
	String sPassword;
	
	public AuthenticationCredentials(String sUser, String sPassword) {
		if(!Utils.isNullOrEmpty(sUser) && !Utils.isNullOrEmpty(sPassword)) {
			this.sUser = sUser;
			this.sPassword = sPassword;
		} else {
			throw new NullPointerException("AuthenticationCredentials.AuthenticationCredential(String sUser, String sPassword): null argument(s)");
		}
	}
	
	public String getUser() {
		return sUser;
	}
	public String getPassword() {
		return sPassword;
	}
	
}

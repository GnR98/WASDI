package wasdi.shared.utils;

import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.validator.routines.EmailValidator;

import wasdi.shared.business.User;
import wasdi.shared.viewmodels.ChangeUserPasswordViewModel;
import wasdi.shared.viewmodels.LoginInfo;
import wasdi.shared.viewmodels.RegistrationInfoViewModel;
import wasdi.shared.viewmodels.UserViewModel;

public class CredentialPolicy {

	//TODO read constants from config file
	//UUID are 36 characters long (32 alphanumeric + 4 hyphens "-" )

	private static int MINUSERIDLENGTH =2;
	private static int MINPASSWORDLENGTH = 8;
	private static int MINGUIDLENGTH = 31;
	private static int MINGOOGLEIDLENGTH = MINGUIDLENGTH;
	private static int MINSESSIONIDLENGTH = MINGUIDLENGTH;


	//probably we may raise these values to 2 but no more: some (sur)names can be very short (e.g. "Li")
	private static int MINNAMELENGTH = 1;
	private static int MINSURNAMELENGTH = 1;

	//TODO check other private methods, from utils

	private boolean isNullOrEmpty(String sString) {
		if (sString == null) {
			return true;
		}
		if (sString.isEmpty()) {
			return  true;
		}
		return  false;
	}

	public Boolean validPassword(String sPassword ) {
		if(isNullOrEmpty(sPassword)) {
			return false;
		}
		if(sPassword.length() < MINPASSWORDLENGTH ) {
			return false;
		} else {
			return true;
		}
	}

	private Boolean validGoogleIdToken(String sGoogleId) {
		if(isNullOrEmpty(sGoogleId)) {
			return false;
		}
		if(sGoogleId.length() < MINGOOGLEIDLENGTH ) {
			return false;
		} else {
			return true;
		}
	}

	public Boolean validEmail(String sEmail ) {
		if(isNullOrEmpty(sEmail)) {
			return false;
		} else if(!EmailValidator.getInstance().isValid(sEmail) ){
			return false;
		} else {
			return true;
		}
	}

	//TODO check it is not used as an email validator
	public Boolean validUserId(String sUserId) {
		if(isNullOrEmpty(sUserId)) {
			return false;
		}
		if(sUserId.length() < MINUSERIDLENGTH) {
			return false;
		} /*
		//commented out because previously inserted users didn't have a valid email address as userId
		else if(!EmailValidator.getInstance().isValid(sUserId) ){
			return false;
		} */else {
			return true;
		}
	}

	public Boolean validName(String sName) {
		if(isNullOrEmpty(sName)) {
			return false;
		}
		if(sName.length() < MINNAMELENGTH) {
			return false;
		} else {
			return true;
		}
	}

	public Boolean validSurname(String sSurname) {
		if(isNullOrEmpty(sSurname)) {
			return false;
		}
		if(sSurname.length() < MINSURNAMELENGTH ) {
			return false;
		} else {
			return true;
		}
	}

	public Boolean validSessionId(String sSessionId) {
		if(isNullOrEmpty(sSessionId)) {
			return false;
		}
		if( sSessionId.length() < MINSESSIONIDLENGTH) {
			return false;
		} else {
			return true;
		}
	}

	private boolean validAuthServiceProvider(String sAuthServiceProvider) {
		if(isNullOrEmpty(sAuthServiceProvider)) {
			return false;
		} else {
			String sGoogle = new String("google");
			if(sAuthServiceProvider.toLowerCase().equals(sGoogle.toLowerCase())) {
				return true;
			} else {
				String sWasdi = new String("wasdi");
				if(sAuthServiceProvider.toLowerCase().equals(sWasdi.toLowerCase())) {
					return true;
				} else {
					return false;
				}
			}
		}
	}

	private Boolean validValidAfterFirstAccess(Boolean bValid ) {
		if (null == bValid) {
			return false;
		} else {
			//the flag is valid whether it is both true or false
			return true;
		}
	}

	public Boolean validFirstAccessUUID(String sUUID) {
		if(isNullOrEmpty(sUUID)) {
			return false;
		} else if(sUUID.length() < MINGUIDLENGTH ) {
			return false;
		} else {
			return true;
		}
	}


	// begin satisfaction checks 


	public Boolean satisfies( LoginInfo oLoginInfo ) {
		if(null == oLoginInfo) {
			throw new NullArgumentException();
		}
		//TODO check after refactoring: due to googleId modifications these conditions may change		
		if( validGoogleIdToken(oLoginInfo.getGoogleIdToken()) ) {
			return true;
		} else if(!validUserId(oLoginInfo.getUserId())) {
			return false;
		} else if(!validPassword(oLoginInfo.getUserPassword())) {
			return false;
		} else {
			return true;
		}
	}

	public Boolean satisfies( UserViewModel oUserVM ) {
		if( null == oUserVM) {
			throw new NullArgumentException();
		}
		if(!validUserId(oUserVM.getUserId())) {
			return false;
		} else if(!validName(oUserVM.getName() )) {
			return false;
		} else if(!validSurname(oUserVM.getSurname())) {
			return false;
		} else if(!validSessionId(oUserVM.getSessionId() )) {
			return false;
		} else {
			return true;
		}
	}

	public boolean satisfies(User oUser) {
		if(null==oUser) {
			throw new NullArgumentException();
		}
		//TODO check after refactoring: due to googleId modifications these conditions may change
		if(validAuthServiceProvider(oUser.getAuthServiceProvider())) {
			return true;
		} else if(!validUserId(oUser.getUserId())) {
			return false;
		} else if( !validName(oUser.getName())) {
			return false;
		} else if( !validSurname(oUser.getSurname())) {
			return false;
		} else if( !validPassword(oUser.getPassword())) {
			return false;
		} else if( !validValidAfterFirstAccess(oUser.getValidAfterFirstAccess())) {
			return false;
		} else if( !validFirstAccessUUID(oUser.getFirstAccessUUID())) {
			return false;
		} else {
			return true;
		}
	}

	public Boolean satisfies(RegistrationInfoViewModel oRInfo) {
		if(null == oRInfo) {
			throw new NullArgumentException();
		}
		//TODO check after refactoring: due to googleId modifications these conditions may change
		if(validGoogleIdToken(oRInfo.getGoogleIdToken())) {
			return true;
		} else if(!validUserId(oRInfo.getUserId())) {
			return false;
		} else if(!validName(oRInfo.getName())) {
			return false;
		} else if(!validSurname(oRInfo.getSurname())) {
			return false;
		} else if(!validPassword(oRInfo.getPassword())) {
			return false;
		} else {
			return true;
		}
	}

	public Boolean satisfies(ChangeUserPasswordViewModel oChangeUserPassword) {
		if(null == oChangeUserPassword) {
			throw new NullArgumentException();
		}
		if(validPassword(oChangeUserPassword.getCurrentPassword()) && validPassword(oChangeUserPassword.getNewPassword())) {
			return true;
		} else {
			return false;
		}
	}

}

package it.fadeout.rest.resources;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.business.PasswordAuthentication;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.sftp.SFTPManager;

import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ChangeUserPasswordViewModel;
import wasdi.shared.viewmodels.LoginInfo;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.RegistrationInfoViewModel;
import wasdi.shared.viewmodels.UserViewModel;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;


@Path("/auth")
public class AuthResource {
	
	//TODO use dependency injection
	PasswordAuthentication m_oPasswordAuthentication = new PasswordAuthentication();
	//TODO use dependency injection
	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();
	
	@Context
	ServletConfig m_oServletConfig;
	
	@POST
	@Path("/login")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel Login(LoginInfo oLoginInfo) {
		Wasdi.DebugLog("AuthResource.Login");
		//TODO captcha

		UserViewModel oUserVM = UserViewModel.getInvalid();
		try {
			if (oLoginInfo == null) {
				return UserViewModel.getInvalid();
			}
			if(!m_oCredentialPolicy.satisfies(oLoginInfo)) {
				return UserViewModel.getInvalid();
			}
			
			//TODO log instead
			System.out.println("AuthResource.Login: requested access from " + oLoginInfo.getUserId());
			
			UserRepository oUserRepository = new UserRepository();

			User oWasdiUser = oUserRepository.GetUser(oLoginInfo.getUserId());
			if( oWasdiUser == null ) {
				return UserViewModel.getInvalid();
			}
			
			if(null != oWasdiUser.getValidAfterFirstAccess()) {
				if(oWasdiUser.getValidAfterFirstAccess() ) {
					Boolean bLoginSuccess = m_oPasswordAuthentication.authenticate(
											oLoginInfo.getUserPassword().toCharArray(),
											oWasdiUser.getPassword()
										);
					if ( bLoginSuccess ) {
						//get all expired sessions
						clearUserExpiredSessions(oWasdiUser);
						oUserVM = new UserViewModel();
						oUserVM.setName(oWasdiUser.getName());
						oUserVM.setSurname(oWasdiUser.getSurname());
						oUserVM.setUserId(oWasdiUser.getUserId());
						
						UserSession oSession = new UserSession();
						oSession.setUserId(oWasdiUser.getUserId());
						
						//TODO check: two users must not have the same sessionId (to avoid ambiguity when getting user from sessionId)
						//can it really happen? Should we really read from DB to check for this possibility?
						//Actual risk of collision is very low (~10^-10 over a year)
						//https://stackoverflow.com/questions/20999792/does-randomuuid-give-a-unique-id
						String sSessionId = UUID.randomUUID().toString();
						oSession.setSessionId(sSessionId);
						oSession.setLoginDate((double) new Date().getTime());
						oSession.setLastTouch((double) new Date().getTime());
						
						SessionRepository oSessionRepo = new SessionRepository();
						Boolean bRet = oSessionRepo.InsertSession(oSession);
						if (!bRet) {
							return oUserVM;
						}
						oUserVM.setSessionId(sSessionId);
						//TODO log instead
						System.out.println("AuthService.Login: access succeeded");
					} else {
						//TODO log instead
						System.out.println("AuthService.Login: access failed");
					}	
				} else {
					//TODO log instead
					System.err.println("AuthService.Login: registration not validated yet");
				}
			} else {
				//TODO log instead
				System.err.println("AuthService.Login: registration flag is null");
			}
				
		}
		catch (Exception oEx) {
			//TODO log instead
			System.out.println("AuthService.Login: Error");
			oEx.printStackTrace();
			
		}
		
		return oUserVM;
	}

	private void clearUserExpiredSessions(User oUser) {
		//TODO allow checking for User policy satisfaction 
		SessionRepository oSessionRepository = new SessionRepository();
		List<UserSession> aoEspiredSessions = oSessionRepository.GetAllExpiredSessions(oUser.getUserId());
		for (UserSession oUserSession : aoEspiredSessions) {
			//delete data base session
			if (!oSessionRepository.DeleteSession(oUserSession)) {
				//TODO log instead
				System.err.println("AuthService.Login: Error deleting session.");
			}
		}
	}
	
	@GET
	@Path("/checksession")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel CheckSession(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("AuthResource.CheckSession");

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			//TODO switch to the version below: @sergin13 + @kr1zz
			return null;
			//return UserViewModel.getInvalid();
			
		}
		//TODO implement code to use version below
		if(!m_oCredentialPolicy.validUserId(oUser.getUserId())) {
		//if(!m_oCredentialPolicy.satisfies(oUser)) {
			//TODO switch to the version below
			return null;
			//return UserViewModel.getInvalid();
		}
		
		UserViewModel oUserVM = new UserViewModel();
		oUserVM.setName(oUser.getName());
		oUserVM.setSurname(oUser.getSurname());
		oUserVM.setUserId(oUser.getUserId());
		
		return oUserVM;
	}	
	

	@GET
	@Path("/logout")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult Logout(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("AuthResource.Logout");
		
		PrimitiveResult oResult = new PrimitiveResult();
		//TODO refactor oRsult in order to use null object @sergin13 + @kr1zz
		oResult.setBoolValue(false);
		
		
		if(!m_oCredentialPolicy.validSessionId(sSessionId)) {
			return oResult;
		}
		
		SessionRepository oSessionRepository = new SessionRepository();
		UserSession oSession = oSessionRepository.GetSession(sSessionId);
		if(oSession != null) {
			if(oSessionRepository.DeleteSession(oSession)) {
				//TODO log instead
				System.out.println("AuthService.Logout: Session data base deleted.");
				oResult.setBoolValue(true);
			}
			else {
				//TODO log instead
				System.out.println("AuthService.Logout: Error deleting session data base.");
			}
			
		}
		
		return oResult;
	}	

	
	
	@POST
	@Path("/upload/createaccount")
	@Produces({"application/json", "text/xml"})
	public Response CreateSftpAccount(@HeaderParam("x-session-token") String sSessionId, String sEmail) {
		Wasdi.DebugLog("AuthService.CreateSftpAccount: Called for Mail " + sEmail);
		
		if(! m_oCredentialPolicy.validSessionId(sSessionId) || m_oCredentialPolicy.validEmail(sEmail)) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		//TODO implement code to use version below
		if (oUser == null || m_oCredentialPolicy.validUserId(oUser.getUserId())) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		//if(!m_oCredentialPolicy.satisfies(oUser)) {return Response.status(Status.UNAUTHORIZED).build();}
		String sAccount = oUser.getUserId();
		//TODO read from servlet config file
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) {
			wsAddress = "ws://localhost:6703";
		}
		SFTPManager oManager = new SFTPManager(wsAddress);
		String sPassword = UUID.randomUUID().toString().split("-")[0];
		
		if (!oManager.createAccount(sAccount, sPassword)) {
			//TODO log instead
			System.out.println("AuthService.CreateSftpAccount: error creating sftp account");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		sendPasswordEmail(sEmail, sAccount, sPassword);
	    
		return Response.ok().build();
	}
	
	@GET
	@Path("/upload/existsaccount")
	@Produces({"application/json", "text/xml"})
	public boolean ExixtsSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		//TODO check input: what shall we return if sSessionId is null/invalid? false? @sergin13 + @kr1zz
		Wasdi.DebugLog("AuthService.ExistsSftpAccount");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		//TODO implement code to use version below
		if (oUser == null || m_oCredentialPolicy.validUserId(oUser.getUserId())) { return false; }
		//if(!m_oCredentialPolicy.satisfies(oUser)) {return false;}
		String sAccount = oUser.getUserId();		
		
		//TODO read param from servlet config file
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		return oManager.checkUser(sAccount);
	}


	@GET
	@Path("/upload/list")
	@Produces({"application/json", "text/xml"})
	public String[] ListSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("AuthService.ListSftpAccount");
		if(! m_oCredentialPolicy.validSessionId(sSessionId) ) {
			return null;
		}
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		//TODO implement to use version below
		if (oUser == null || m_oCredentialPolicy.validUserId(oUser.getUserId())) { return null; }
		//if(!m_oCredentialPolicy.satisfies(oUser)) {return null;}	
		String sAccount = oUser.getUserId();		
		
		//TODO read param from servlet config file
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		return oManager.list(sAccount);
	}
	

	@DELETE
	@Path("/upload/removeaccount")
	@Produces({"application/json", "text/xml"})
	public Response RemoveSftpAccount(@HeaderParam("x-session-token") String sSessionId) {
		//TODO check input: what shall we return if sSessionId is null/invalid? @sergin13 + @kr1zz
		Wasdi.DebugLog("AuthService.RemoveSftpAccount");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		//TODO implement to use below version
		if (oUser == null || m_oCredentialPolicy.validUserId(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		//if(!m_oCredentialPolicy.satisfies(oUser)) {return Response.status(Status.UNAUTHORIZED).build();}
		String sAccount = oUser.getUserId();
		
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);

		return oManager.removeAccount(sAccount) ? Response.ok().build() : Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}


	@POST
	@Path("/upload/updatepassword")
	@Produces({"application/json", "text/xml"})
	public Response UpdateSftpPassword(@HeaderParam("x-session-token") String sSessionId, String sEmail) {
		
		Wasdi.DebugLog("AuthService.UpdateSftpPassword");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		//TODO implement to use below version
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		//if(!m_oCredentialPolicy.satisfies(oUser)) {return Response.status(Status.UNAUTHORIZED).build();}
		
		String sAccount = oUser.getUserId();
		
		//TODO read param from servlet config file
		String wsAddress = m_oServletConfig.getInitParameter("sftpManagementWSServiceAddress");
		if (wsAddress==null) wsAddress = "ws://localhost:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);
		
		String sPassword = UUID.randomUUID().toString().split("-")[0];
		
		if (!oManager.updatePassword(sAccount, sPassword)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		sendPasswordEmail(sEmail, sAccount, sPassword);

		return Response.ok().build();
	}
	
	//CHECK USER ID TOKEN BY GOOGLE 
	@POST
	@Path("/logingoogleuser")
	@Produces({"application/xml", "application/json", "text/xml"})
	public UserViewModel LoginGoogleUser(LoginInfo oLoginInfo) {
		Wasdi.DebugLog("AuthResource.CheckGoogleUserId");
		//TODO captcha
		//TODO refactor to use null object @sergin13 + @kr1zz
		//TODO check policy in order to be coherent
		UserViewModel oUserVM = new UserViewModel();
		oUserVM.setUserId("");
		
		try 
		{
			if (oLoginInfo == null) {
				return oUserVM;
			}
			if(!m_oCredentialPolicy.satisfies(oLoginInfo)) {
				return oUserVM;
			}
			
			final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
			final JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
				    // Specify the CLIENT_ID of the app that accesses the backend:
				    .setAudience(Collections.singletonList(oLoginInfo.getUserId()))
				    // Or, if multiple clients access the backend:
				    //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
				    .build();
			
			// (Receive idTokenString by HTTPS POST)
			GoogleIdToken oIdToken = verifier.verify(oLoginInfo.getGoogleIdToken());
			
			//check id token
			if (oIdToken != null) 
			{
			  Payload oPayload = oIdToken.getPayload();

			  // Print user identifier
			  String userId = oPayload.getSubject();
			 
			  // Get profile information from payload
			  String sEmail = oPayload.getEmail();
			  
			 /* boolean bEmailVerified = Boolean.valueOf(oPayload.getEmailVerified());
			  String sName = (String) oPayload.get("name");
			  String sPictureUrl = (String) oPayload.get("picture");
			  String sLocale = (String) oPayload.get("locale");
			  String sGivenName = (String) oPayload.get("given_name");
			  String sFamilyName = (String) oPayload.get("family_name");*/
			  
			  // store profile information and create session
			  //TODO log instead
			  System.out.println("AuthResource.LoginGoogleUser: requested access from " + userId);
			

			  UserRepository oUserRepository = new UserRepository();
			  String sAuthProvider = "google";
			  User oWasdiUser = oUserRepository.GoogleLogin(userId , sEmail, sAuthProvider);
			  //save new user 
			  if(oWasdiUser == null)
			  {
				  User oUser = new User();
				  oUser.setAuthServiceProvider(sAuthProvider);
				  oUser.setUserId(userId);
				  
				  if(oUserRepository.InsertUser(oUser) == true)
				  {
					  //the user is stored in DB
					  //get user from database (i do it only for consistency)
					  oWasdiUser = oUserRepository.GoogleLogin(userId , sEmail, sAuthProvider);
				  }
			  }
			  
			  if (oWasdiUser != null) 
			  {

				  //get all expired sessions
				  SessionRepository oSessionRepository = new SessionRepository();
				  List<UserSession> aoEspiredSessions = oSessionRepository.GetAllExpiredSessions(oWasdiUser.getUserId());
				  for (UserSession oUserSession : aoEspiredSessions) {
					  //delete data base session
					  if (!oSessionRepository.DeleteSession(oUserSession)) {
						  //TODO log instead
						  System.out.println("AuthService.LoginGoogleUser: Error deleting session.");
					  }
				  }

				  oUserVM.setName(oWasdiUser.getName());
				  oUserVM.setSurname(oWasdiUser.getSurname());
				  oUserVM.setUserId(oWasdiUser.getUserId());
				  
				  UserSession oSession = new UserSession();
				  oSession.setUserId(oWasdiUser.getUserId());
				  String sSessionId = UUID.randomUUID().toString();
				  oSession.setSessionId(sSessionId);
				  oSession.setLoginDate((double) new Date().getTime());
				  oSession.setLastTouch((double) new Date().getTime());

				  Boolean bRet = oSessionRepository.InsertSession(oSession);
				  if (!bRet)
					  return oUserVM;

				  oUserVM.setSessionId(sSessionId);
				  //TODO log instead
				  System.out.println("AuthService.LoginGoogleUser: access succeeded");
			  }
			  else {
				  //TODO log instead
				  System.out.println("AuthService.LoginGoogleUser: access failed");
			  }

			} 
			else {
				//TODO log instead
				System.out.println("Invalid ID token.");
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return oUserVM;

	}
		
	@POST
	@Path("/register")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult userRegistration(RegistrationInfoViewModel oUserViewModel) 
	{	
		Wasdi.DebugLog("AuthService.UserRegistration"  );
		//TODO captcha
		
		PrimitiveResult oResult = new PrimitiveResult();
		//TODO refactor w/ null object
		oResult.setBoolValue(false);
		
		if(null == oUserViewModel) {
			return oResult;
		}
		
		if(oUserViewModel != null)
		{
			try
			{
				//TODO credentialPolicy for RegistrationInfoViewModel
				//Check User properties
				if(Utils.isNullOrEmpty(oUserViewModel.getUserId()) || !Utils.isValidEmail(oUserViewModel.getUserId()) ) {
					return oResult;
				}
				if(Utils.isNullOrEmpty(oUserViewModel.getName())) {
					return oResult;
				}
				if(Utils.isNullOrEmpty(oUserViewModel.getSurname())) {
					return oResult;
				}
				if(!Utils.passwordIsGoodEnough(oUserViewModel.getPassword())){
					return oResult;
				}
				
				UserRepository oUserRepository = new UserRepository();
				User oWasdiUser = oUserRepository.GetUser(oUserViewModel.getUserId());
				
				//if oWasdiUser is a new user -> oWasdiUser == null
				if(oWasdiUser == null) {
					//save new user 
					String sAuthProvider = "wasdi";
					User oNewUser = new User();
					oNewUser.setAuthServiceProvider(sAuthProvider);
					oNewUser.setUserId(oUserViewModel.getUserId());
					oNewUser.setName(oUserViewModel.getName());
					oNewUser.setSurname(oUserViewModel.getSurname());
					oNewUser.setPassword(m_oPasswordAuthentication.hash(oUserViewModel.getPassword().toCharArray()));
					oNewUser.setValidAfterFirstAccess(false);
					String sToken = UUID.randomUUID().toString();
					oNewUser.setFirstAccessUUID(sToken);
					
					if(oUserRepository.InsertUser(oNewUser) == true) {
						//the user is stored in DB
						oResult.setBoolValue(true);
					}
					//build confirmation link
					String sLink = buildRegistrationLink(oNewUser);
					//TODO log instead
					System.out.println(sLink);
					//send it via email to the user
					sendRegistrationEmail(oNewUser, sLink);
					
					//uncomment only if email sending service does not work
					//oResult = validateNewUser(oUserViewModel.getUserId(), sToken);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				oResult.setBoolValue(false);
			}

		}
		
		return oResult;
	}
	
	
	@GET
	@Path("/validateNewUser")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult validateNewUser(@QueryParam("email") String sUserId, @QueryParam("validationCode") String sToken  ) {
		//TODO log
		
		//TODO refactor to use null object @sergin13 @kr1zz
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		//TODO check input w/ CredentialPolicy
		if(!Utils.userIdIsGoodEnough(sUserId)) {
			return oResult;
		}
		if(Utils.isNullOrEmpty(sToken)) {
			return oResult;
		}
		
		UserRepository oUserRepo = new UserRepository();
		User oUser = oUserRepo.GetUser(sUserId);
		if( null == oUser.getValidAfterFirstAccess()) {
			//TODO log instead
			System.err.println("AuthResources.validateNewUser: unexpected null first access validation flag");
			return oResult;
		} else if( oUser.getValidAfterFirstAccess() ) {
			//TODO log instead
			System.err.println("AuthResources.validateNewUser: unexpected true first access validation flag");
			return oResult;
		} else if( !oUser.getValidAfterFirstAccess() ) {
			String sDBToken = oUser.getFirstAccessUUID();
			if(Utils.guidIsGoodEnough(sToken) ){
				if(sDBToken.equals(sToken)) {
					oUser.setValidAfterFirstAccess(true);
					oUserRepo.UpdateUser(oUser);
					oResult.setBoolValue(true);
				} else {
					//TODO log instead
					System.err.println("AuthResources.validateNewUser: registration token mismatch");
					return oResult;
				}
			}
		}
		
		return oResult;
	}
	

@POST
	@Path("/editUserDetails")
	@Produces({"application/json", "text/xml"})
	public UserViewModel editUserDetails(@HeaderParam("x-session-token") String sSessionId, UserViewModel oInputUserVM ) {
		Wasdi.DebugLog("AuthService.signin"  );
		//note: sSessionId validity is automatically checked later
		//note: only name and surname can be changed, so far. Other fields are ignored

		//TODO refactor to use null object @sergin13 + @kr1zz
		//TODO check w/ CredentialPolicy
		//check name
		if(Utils.isNullOrEmpty(oInputUserVM.getName())) {
			//TODO log instead
			System.err.println("AuthResource.EditUserDetails: oUserVM.getName() null or empty");
			return null;
		}
		
		//check surname
		if(Utils.isNullOrEmpty(oInputUserVM.getSurname())) {
			//TODO log instead
			System.err.println("AuthResource.EditUserDetails: oUserVM.getSurname() null or empty");
			return null;
		}
		
		UserViewModel oOutputUserVM = null;
		
		try {
			//note: session validity is automatically checked		
			User oUserId = Wasdi.GetUserFromSession(sSessionId);
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				System.err.print("Null user from session id (does the user exist?)");
				return null;
			}
	
			oUserId.setName(oInputUserVM.getName());
			oUserId.setSurname(oInputUserVM.getSurname());
			
			oOutputUserVM = new UserViewModel();
			oOutputUserVM.setUserId(oUserId.getUserId());
			oOutputUserVM.setName(oUserId.getName());
			oOutputUserVM.setSurname(oUserId.getSurname());
			oOutputUserVM.setSessionId(sSessionId);
			

			UserRepository oUR = new UserRepository();
			oUR.UpdateUser(oUserId);
			
		} catch(Exception e) {
			//TODO log instead
			System.err.println("AuthService.ChangeUserPassword: Exception");
			e.printStackTrace();
		}		
		return oOutputUserVM;
	}

	
	
	@POST
	@Path("/changePassword")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ChangeUserPassword(@HeaderParam("x-session-token") String sSessionId,
			ChangeUserPasswordViewModel oChPasswViewModel) {
		
		Wasdi.DebugLog("AuthService.ChangeUserPassword"  );
		
		//TODO refactor to use null object @sergin13 + @kr1zz
		//TODO check w/ CredentialPolicy
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		//input validation
		//(just oChPasswViewModel, sSessionId validity is automatically checked later on)
		if(null == oChPasswViewModel) {
			oResult.setStringValue("AuthService.ChangeUserPassword: null ChangeUserPasswordViewModel");
			//TODO log instead
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		if(null == oChPasswViewModel.getNewPassword() ) {
			oResult.setStringValue("AuthService.ChangeUserPassword: null new password!");
			//TODO log instead
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		if(null == oChPasswViewModel.getCurrentPassword() ) {
			oResult.setStringValue("AuthService.ChangeUserPassword: null current password!");
			//TODO log instead
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		if( Utils.passwordIsGoodEnough(oChPasswViewModel.getNewPassword()) == false) {
			oResult.setStringValue("AuthService.ChangeUserPassword: password is too short");
			//TODO log instead
			System.err.println(oResult.getStringValue());
			return oResult;
		}
		
		
		try {
			//validity is automatically checked		
			User oUserId = Wasdi.GetUserFromSession(sSessionId);
			if(null == oUserId) {
				//Maybe the user didn't exist, or failed for some other reasons
				//TODO log instead
				System.err.print("Null user from session id (does the user exist?)");
				return oResult;
			}
	
			String sOldPassword = oUserId.getPassword();
			Boolean bPasswordCorrect = m_oPasswordAuthentication.authenticate(oChPasswViewModel.getCurrentPassword().toCharArray(), sOldPassword);
			
			if( !bPasswordCorrect ) {
				//TODO log instead
				System.err.println("Wrong current password for user " + oUserId);
				return oResult;
			} else {
				oUserId.setPassword(m_oPasswordAuthentication.hash(oChPasswViewModel.getNewPassword().toCharArray()));
				UserRepository oUR = new UserRepository();
				oUR.UpdateUser(oUserId);
				oResult.setBoolValue(true);
			}
		} catch(Exception e) {
			//TODO log instead
			System.err.println("AuthService.ChangeUserPassword: Exception");
			e.printStackTrace();
		}
		
		return oResult;
		
	} 	
	
	private void sendRegistrationEmail(User oUser, String sLink) {
		//TODO log
		//TODO check w/ CredentialPolicy
		try {
			
			String sMercuriusAPIAddress = m_oServletConfig.getInitParameter("mercuriusAPIAddress");
			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				//TODO log instead
				System.err.println("AuthResource.sendRegistrationEmail: sMercuriusAPIAddress is null");
				return;
			}
			MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
			Message oMessage = new Message();
			
			//TODO let the servlet config handle the message subject title
			//e.g.
			//String sTitle = m_oServletConfig.getInitParameter("sftpMailTitle");
			String sTitle = "Welcome to WASDI";
			oMessage.setTilte(sTitle);
			
			//TODO use the appropriate sender config
			String sSender = m_oServletConfig.getInitParameter("sftpManagementMailSenser");
			if (sSender==null) {
				sSender = "adminwasdi@wasdi.org";
			}
			oMessage.setSender(sSender);
			
			//String sMessage = m_oServletConfig.getInitParameter("sftpMailText");
			//TODO let the servlet config handle the message body
			String sMessage = "Dear " + oUser.getName() + " " + oUser.getSurname() + ",\n welcome to WASDI.\n\n"+
					"Please click on the link below to activate your account:\n\n" + 
					sLink;
			oMessage.setMessage(sMessage);
			oAPI.sendMailDirect(oUser.getUserId(), oMessage);
		} catch(Exception e) {
			//TODO log instead
			System.err.println(e.getMessage() );
			return;
		}
	}
	
	
	//TODO link to a client's page
	private String buildRegistrationLink(User oUser) {
		//MAYBE check w/ CredentialPolicy 
		String sResult = "";
		
		String sAPIUrl =  m_oServletConfig.getInitParameter("REGISTRATION_API_URL");
		String sUserId = "email=" + oUser.getUserId();
		String sToken = "validationCode=" + oUser.getFirstAccessUUID();
		
		sResult = sAPIUrl + sUserId + "&" + sToken;
		
		return sResult;
	}


	private void sendPasswordEmail(String sRecipientEmail, String sAccount, String sPassword) {
		//TODO log
		//MAYBE refactor (?) to use null object @sergin13 + @kr1zz (?)
		//TODO check w/ CredentialPolicy
		//send email with new password
		String sMercuriusAPIAddress = m_oServletConfig.getInitParameter("mercuriusAPIAddress");
		MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
		Message oMessage = new Message();
		String sTitle = m_oServletConfig.getInitParameter("sftpMailTitle");
		oMessage.setTilte(sTitle);
		String sSenser = m_oServletConfig.getInitParameter("sftpManagementMailSenser");
		if (sSenser==null) sSenser = "adminwasdi@wasdi.org";
		oMessage.setSender(sSenser);
		
		String sMessage = m_oServletConfig.getInitParameter("sftpMailText");
		sMessage += "\n\nUSER: " + sAccount + " - PASSWORD: " + sPassword;
		oMessage.setMessage(sMessage);
		oAPI.sendMailDirect(sRecipientEmail, oMessage);
	}

}

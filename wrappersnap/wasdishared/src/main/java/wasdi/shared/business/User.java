package wasdi.shared.business;

/**
 * WASDI User
 * Created by p.campanella on 21/10/2016.
 */
public class User {
	
	/**
	 * Unique int id
	 */
    private int id;
    /**
     * User ID
     */
    private String userId;
    /**
     * Name
     */
    private String name;
    /**
     * Surname
     */
    private String surname;
    /**
     * Password
     */
    private String password;
    
    /**
     * Flag to check the first Access
     */
    private Boolean validAfterFirstAccess;
    
    /**
     * UUID for the confirmation mail
     */
    private String firstAccessUUID;
    
    /**
     * Internal code of the authentication provider
     */
    private String authServiceProvider;
    
    /**
     * Google Id Token for google users
     */
    private String googleIdToken;

    /**
     * User registration date
     */
    private String m_sRegistrationDate = null;

    /**
     * User confirmation
     */
    private String m_sConfirmationDate = null;

    /**
     * User last login date
     */
    private String m_sLastLogin = null;

    /**
     * Singleton invalid User
     */
    private static User s_oInvalid;
        
    static {
    	s_oInvalid = new User();
    	s_oInvalid.id = -1;
    	s_oInvalid.userId = null;
    	s_oInvalid.name = null;
    	s_oInvalid.surname = null;
    	s_oInvalid.password = null;
    	s_oInvalid.validAfterFirstAccess = null;
    	s_oInvalid.firstAccessUUID = null;
    	s_oInvalid.authServiceProvider = null;
    }

    public static User getInvalid() {
		return s_oInvalid;
	}
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

	public Boolean getValidAfterFirstAccess() {
		return validAfterFirstAccess;
	}

	public void setValidAfterFirstAccess(Boolean validAfterFirstAccess) {
		this.validAfterFirstAccess = validAfterFirstAccess;
	}

	public String getFirstAccessUUID() {
		return firstAccessUUID;
	}

	public void setFirstAccessUUID(String firstAccessUUID) {
		this.firstAccessUUID = firstAccessUUID;
	}

	public String getAuthServiceProvider() {
		return authServiceProvider;
	}

	public void setAuthServiceProvider(String authServiceProvider) {
		this.authServiceProvider = authServiceProvider;
	}

	public String getGoogleIdToken() {
		return googleIdToken;
	}

	public void setGoogleIdToken(String googleIdToken) {
		this.googleIdToken = googleIdToken;
	}

	public String getRegistrationDate() {
		return m_sRegistrationDate;
	}

	public void setRegistrationDate(String sRegistrationDate) {
		this.m_sRegistrationDate = sRegistrationDate;
	}

	public String getLastLogin() {
		return m_sLastLogin;
	}

	public void setLastLogin(String sLastLogin) {
		this.m_sLastLogin = sLastLogin;
	}

	public String getConfirmationDate() {
		return m_sConfirmationDate;
	}

	public void setConfirmationDate(String oConfirmationDate) {
		this.m_sConfirmationDate = oConfirmationDate;
	}

}

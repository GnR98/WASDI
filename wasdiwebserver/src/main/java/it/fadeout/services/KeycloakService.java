package it.fadeout.services;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import it.fadeout.Wasdi;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;

public class KeycloakService implements AuthProviderService {

	@Context
	ServletConfig m_oServletConfig;

	private static final String s_sACCESS_TOKEN = "access_token";

	//just to hide the implicit one
	//private KeycloakService() {}

	public String getToken() {
		try {
			String sAuthUrl = m_oServletConfig.getInitParameter("keycloak_server");
			String sCliSecret = m_oServletConfig.getInitParameter("keycloak_CLI_Secret");
			//URL
			if(!sAuthUrl.endsWith("/")) {
				sAuthUrl += "/";
			}
			sAuthUrl += "realms/wasdi/protocol/openid-connect/token";
			//payload
			String sPayload = "client_id=admin-cli" +
					"&grant_type=client_credentials" +
					"&client_secret=" + sCliSecret;
			//headers
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
			//POST -> authenticate on keycloak 
			Utils.debugLog("KeycloakService.getToken: about to get token: " + sAuthUrl + ", " + sPayload);
			String sAuthResult = Wasdi.httpPost(sAuthUrl, sPayload, asHeaders);
			if(Utils.isNullOrEmpty(sAuthResult)) {
				throw new RuntimeException("could not login into keycloak");
			}
			//read response and get token
			JSONObject oJson = new JSONObject(sAuthResult);
			if( null == oJson||
					!oJson.has(s_sACCESS_TOKEN) ||							
					Utils.isNullOrEmpty(oJson.optString(s_sACCESS_TOKEN, null))
					) {
				throw new NullPointerException("Missing access token");
			}
			String sKcTokenId = "";
			sKcTokenId = oJson.optString(s_sACCESS_TOKEN, null);
			if(Utils.isNullOrEmpty(sKcTokenId)) {
				throw new NullPointerException("Token id null or empty");
			}
			Utils.debugLog("KeycloakService.getKeycloakAdminCliToken: admin token obtained :-)");
			return sKcTokenId;
		} catch (Exception oE) {
			Utils.debugLog("KeycloakService.getKeycloakAdminCliToken: " + oE);
		}
		return null;
	}

	public String getUserData(String sToken, String sUserId) {
		// build keycloak API URL
		String sUrl = m_oServletConfig.getInitParameter("keycloak_server");
		if(!sUrl.endsWith("/")) {
			sUrl += "/";
		}
		sUrl += "admin/realms/wasdi/users?exact=true&username=";
		sUrl += sUserId;
		Utils.debugLog("KeycloakService.userRegistration: about to GET to " + sUrl);
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.clear();
		asHeaders.put("Authorization", "Bearer " + sToken);
		return Wasdi.httpGet(sUrl, asHeaders);
	}

	@Override
	public String login(String sUser, String sPassword) {
		//authenticate against keycloak
		String sUrl = m_oServletConfig.getInitParameter("keycloak_auth");

		String sPayload = "client_id=";
		sPayload += m_oServletConfig.getInitParameter("keycloak_confidentialClient");
		sPayload += "&client_secret=" + m_oServletConfig.getInitParameter("keycloak_clientSecret");
		sPayload += "&grant_type=password&username=" + sUser;
		sPayload += "&password=" + sPassword;
		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
		String sAuthResult = Wasdi.httpPost(sUrl, sPayload, asHeaders);

		return sAuthResult;
	}
	
	@Override
	public String getUserDbId(String sUserId) {
		if(Utils.isNullOrEmpty(sUserId)) {
			Utils.debugLog("KeycloakService.getUserDbId: user id null or empty, aborting");
			return null;
		}
		String sToken = getToken();
		if(Utils.isNullOrEmpty(sToken)) {
			Utils.debugLog("KeycloakService.getUserDbId: token null or empty, aborting");
			return null;
		}
		String sResponse = getUserData(sToken, sUserId);
		if(Utils.isNullOrEmpty(sResponse)) {
			Utils.debugLog("KeycloakService.getUserDbId: response null or empty, aborting");
			return null;
		}
		try {
			JSONArray oResponseArray = new JSONArray(sResponse);
			if(oResponseArray.length() != 1) {
				Utils.debugLog("KeycloakService.getUserDbId: response array has length " + oResponseArray.length() + ", but lenght of exactly 1 was expected, aborting");
				return null;
			}
			JSONObject oEntry = oResponseArray.getJSONObject(0);
			return oEntry.optString("id", null);
		} catch (Exception oE) {
			Utils.debugLog("KeycloakService.getUserDbId: could not parse response due to " + oE + ", aborting");
		}
		return null;
	}
	
	@Override
	public PrimitiveResult requirePasswordUpdateViaEmail(String sUserId) {
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		if(Utils.isNullOrEmpty(sUserId)) {
			Utils.debugLog("KeycloakService.requirePasswordUpdateViaEmail: user id null or empty, aborting");
			return oResult;
		}
		String sUserDbId = getUserDbId(sUserId);
		if(Utils.isNullOrEmpty(sUserDbId)) {
			Utils.debugLog("KeycloakService.requirePasswordUpdateViaEmail: user DB id null or empty, aborting");
			return oResult;
		}
		StringBuilder oUrlBuilder = new StringBuilder()
				.append(m_oServletConfig.getInitParameter("keycloak_server"))
				.append("admin/realms/wasdi/users/")
				.append(sUserDbId)
				.append("/execute-actions-email?redirect_uri=https://www.wasdi.net/&client_id=wasdi_api");
		String sUrl = oUrlBuilder.toString();
		//todo check if it is possible to invalidate current password (password expire?)
		//alternatively: set a temporary password unknown to the user
		//todo decide whether we want to interrupt existing sessions
		String sPayload = "[\"UPDATE_PASSWORD\"]";
		try {
			String sToken = getToken();
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Authorization", "Bearer " + sToken);
			asHeaders.put("Content-Type", "application/json");
			String sResponse = Wasdi.httpPut(sUrl, sPayload, asHeaders);
			System.out.println(sResponse);
			oResult.setIntValue(200);
			return oResult;
		} catch (Exception oE) {
			Utils.debugLog("KeycloakService.requirePasswordUpdateViaEmail: could not establish connection due to: " + oE + ", aborting");
		}
		
				
		return oResult;
	}
}







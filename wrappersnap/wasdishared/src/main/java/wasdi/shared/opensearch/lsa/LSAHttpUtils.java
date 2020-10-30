package wasdi.shared.opensearch.lsa;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.net.io.Util;

import wasdi.shared.utils.Utils;

public class LSAHttpUtils {
	
    public static String s_sLoginBaseUrl = "https://sso.collgs.lu/auth/realms/lucollgs/protocol/openid-connect/auth?";
    public static String s_sClientId = "client_id=account&response_mode=fragment&response_type=code&";
    public static String s_sRedirectUrl = "redirect_uri=https://collgs.lu/geocatalog.html";
    
    public static void authenticate(String sUser, String sPassword) {
    	
    	try {
    		
    		Utils.debugLog("LSAProviderAdapter.authenticate: " + sUser);
    		
    		// Create the cookie manager
    		CookieManager oCookieManager = new CookieManager();
    		CookieHandler.setDefault(oCookieManager);
    		
    		// Login URL
    		String sLoginUrl = s_sLoginBaseUrl+s_sClientId+s_sRedirectUrl;
    		String sLoginPage = httpGetResults(sLoginUrl, oCookieManager);
    		
    		// Get the path of the login link
    		String sActionToSearch = "action=\"";
    		int iActionStart=sLoginPage.indexOf(sActionToSearch);
    		int iActionEnd = iActionStart+sActionToSearch.length();
    		int iLinkEnd= sLoginPage.indexOf("\" method=\"post\">");
    		
    		String sActionLink = sLoginPage.substring(iActionEnd, iLinkEnd);
    		sActionLink = sActionLink.replace("&amp;", "&");
    		
    		// Body with the login data
    		String sLoginData = "username=" + URLEncoder.encode(sUser, java.nio.charset.StandardCharsets.UTF_8.toString()) + "&password=" + URLEncoder.encode(sPassword, java.nio.charset.StandardCharsets.UTF_8.toString());
    		
    		// Log in
    		String sLoginResult = LSAHttpUtils.httpPostResults(sActionLink, sLoginData, oCookieManager);
    	}
    	catch (Exception oEx) {
			Utils.debugLog("LSAProviderAdapter.authenticate: Exception " + oEx.toString());
		}
    }
    
	/**
	 * Internal version of get 
	 * @param sUrl
	 * @param oCookieManager
	 * @return
	 */
	public static String httpGetResults(String sUrl, CookieManager oCookieManager) {
		Utils.debugLog("QueryExecutorLSA.httpGetResults( " + sUrl + " )");
		String sResult = null;
		long lStart = 0l;
		int iResponseSize = -1;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			// optional default is GET
			oConnection.setRequestMethod("GET");
			oConnection.setRequestProperty("Accept", "*/*");
			
			Utils.debugLog("Sending 'GET' request to URL : " + sUrl);

			lStart = System.nanoTime();
			try {
				int responseCode =  oConnection.getResponseCode();
				Utils.debugLog("QueryExecutorLSA.httpGetResults: Response Code : " + responseCode);
				String sResponseExtract = null;
				if(200 == responseCode) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if(null!=oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}					
				} else {
					Utils.debugLog("QueryExecutorLSA.httpGetResults: provider did not return 200 but "+responseCode+ " (1/2) and the following message:\n" + oConnection.getResponseMessage());
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					
					String sMessage = oBytearrayOutputStream.toString();
					if(null!=sMessage) {
						sResponseExtract = sMessage.substring(0,  Math.min(sMessage.length(), 200)) + "...";
						Utils.debugLog("QueryExecutorLSA.httpGetResults: provider did not return 200 but "+responseCode+ " (2/2) and this is the content of the error stream:\n" + sResponseExtract);
						if(iResponseSize <= 0) {
							iResponseSize = sMessage.length();
						}						
					}
				}				
			}
			catch (Exception oEint) {
				Utils.debugLog("QueryExecutorLSA.httpGetResults: Exception " + oEint);
			} 
			finally {
				oConnection.disconnect();
			}
			
			long lEnd = System.nanoTime();
			long lTimeElapsed = lEnd - lStart;
			double dMillis = lTimeElapsed / (1000.0 * 1000.0);
			double dSpeed = 0;
			if(iResponseSize > 0) {
				dSpeed = ( (double) iResponseSize ) / dMillis;
				dSpeed *= 1000.0;
			}
			Utils.debugLog("QueryExecutorLSA.httpGetResults( " + sUrl + " ) performance: " + dMillis + " ms, " + iResponseSize + " B (" + dSpeed + " B/s)");
		}
		catch (Exception oE) {
			Utils.debugLog("QueryExecutorLSA.httpGetResults: Exception " + oE);
		}
		return sResult;
	}

	
	public static String httpPostResults(String sUrl, String sPayload, CookieManager oCookieManager) {
		Utils.debugLog("QueryExecutorLSA.httpPostResults( " + sUrl + " )");
		String sResult = null;
		long lStart = 0l;
		int iResponseSize = -1;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setRequestProperty("Accept", "*/*");
			oConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			oConnection.setRequestProperty("Connection", "keep-alive");
			oConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			oConnection.setRequestProperty("User-Agent", "python-requests/2.22.0");
			
			byte[] ayBytes = sPayload.getBytes();
			oConnection.setFixedLengthStreamingMode(ayBytes.length);
			
			if (oCookieManager.getCookieStore().getCookies().size() > 0) {
				List<HttpCookie> aoCookieList = oCookieManager.getCookieStore().getCookies();
				
				String sCookie = "";
				
				for (HttpCookie oHttpCookie : aoCookieList) {
					sCookie += oHttpCookie.getName() + "=" + oHttpCookie.getValue();
					sCookie += "; ";
				}
				
				if (sCookie.length()>0) {
					sCookie = sCookie.substring(0, sCookie.length()-2);
				}
				
				oConnection.setRequestProperty("Cookie", sCookie);				
	        }
			
			oConnection.setDoOutput(true);
			oConnection.connect();
			try(OutputStream os = oConnection.getOutputStream()) {
			    os.write(ayBytes);
			}
			

			Utils.debugLog("QueryExecutorLSA.httpPostResults: Sending 'POST' request to URL : " + sUrl);

			lStart = System.nanoTime();
			try {
				int iResponseCode =  oConnection.getResponseCode();
				Utils.debugLog("QueryExecutorLSA.httpGetResults: Response Code : " + iResponseCode);
				String sResponseExtract = null;
				if(iResponseCode == 200) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if(null!=oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}
				} else if (iResponseCode == 302) {
					
					String sNewUrl = oConnection.getHeaderField("Location");
					String sCookies = oConnection.getHeaderField("Set-Cookie");
					
					Utils.debugLog("QueryExecutorLSA.httpPostResults: redirect to " + sNewUrl);
					
					oConnection = (HttpURLConnection) new URL(sNewUrl).openConnection();
					
					oConnection.setRequestProperty("Cookie", sCookies);
					
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if(null!=oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}					
				}
				else {
					
					Utils.debugLog("QueryExecutorLSA.httpPostResults: provider did not return 200 but "+iResponseCode+ " (1/2) and the following message:\n" + oConnection.getResponseMessage());
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					String sMessage = oBytearrayOutputStream.toString();
					if(null!=sMessage) {
						sResponseExtract = sMessage.substring(0,  200) + "...";
						Utils.debugLog("QueryExecutorLSA.httpPostResults: provider did not return 200 but "+iResponseCode+ " (2/2) and this is the content of the error stream:\n" + sResponseExtract);
						if(iResponseSize <= 0) {
							iResponseSize = sMessage.length();
						}						
					}
				}
			}catch (Exception oEint) {
				Utils.debugLog("QueryExecutorLSA.httpPostResults: Exception " + oEint);
			} finally {
				oConnection.disconnect();
			}
			
			long lEnd = System.nanoTime();
			long lTimeElapsed = lEnd - lStart;
			double dMillis = lTimeElapsed / (1000.0 * 1000.0);
			double dSpeed = 0;
			if(iResponseSize > 0) {
				dSpeed = ( (double) iResponseSize ) / dMillis;
				dSpeed *= 1000.0;
			}
			Utils.debugLog("QueryExecutorLSA.httpPostResults( " + sUrl + ") performance: " + dMillis + " ms, " + iResponseSize + " B (" + dSpeed + " B/s)");
		}
		catch (Exception oE) {
			Utils.debugLog("QueryExecutorLSA.httpPostResults: Exception " + oE);
		}
		return sResult;
	}
	
}

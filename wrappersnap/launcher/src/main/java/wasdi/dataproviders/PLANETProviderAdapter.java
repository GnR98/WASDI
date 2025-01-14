package wasdi.dataproviders;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.Utils;

public class PLANETProviderAdapter extends ProviderAdapter {
	
	/**
	 * Max number of cycles to do waiting for the activation
	 */
	int m_iMaxActivationCycles = 10;
	
	/**
	 * Sleep period of the activation in Seconds
	 */
	long m_lActivationCyclesSleepSeconds = 60;
	
	/**
	 * Sleep period for retry in Seconds
	 */
	long m_lRetrySleepSeconds = 60;
	
	public PLANETProviderAdapter() {
		super();
		m_sDataProviderCode = "PLANET";
	}
	
	public PLANETProviderAdapter(LoggerWrapper logger) {
		super(logger);
		m_sDataProviderCode = "PLANET";
	}
	
	@Override
	protected void internalReadConfig() {
		
		if (m_oDataProviderConfig != null) {
			
			m_sProviderUser = m_oDataProviderConfig.user;
			
			// We should have a config file with the retry and sleep settings
			try {
				String sFile = m_oDataProviderConfig.adapterConfig;
				File oConfigFile = new File(sFile);
				
				if (oConfigFile.exists()) {
					TypeReference<HashMap<String,Object>> oMapType = new TypeReference<HashMap<String,Object>>() {};					
					HashMap<String,Object> oConfig = MongoRepository.s_oMapper.readValue(oConfigFile, oMapType);
					
					if (oConfig.containsKey("retrySleepSeconds")) {
						m_lRetrySleepSeconds = (long) oConfig.get("retrySleepSeconds");
					}
					
					if (oConfig.containsKey("maxActivationCycles")) {
						m_iMaxActivationCycles = (int) oConfig.get("maxActivationCycles");
					}
					
					if (oConfig.containsKey("activationCyclesSleepSeconds")) {
						m_lActivationCyclesSleepSeconds = (long) oConfig.get("activationCyclesSleepSeconds");
					}
				}
			}
			catch (Exception oEx) {
				Utils.debugLog("PLANETProviderAdapter.internalReadConfig: " + oEx.toString());
			}
		}
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		
		if (Utils.isNullOrEmpty(sFileURL)) {
			return 0;
		}
		
		try {
			String [] asUrlParts = sFileURL.split(";");
			
			return getDownloadFileSizeViaHttp(asUrlParts[1]);
			
		}
		catch (Exception oEx) {
			Utils.debugLog("PLANETProviderAdapter.getDownloadFileSize: " + oEx.toString());
		}
		
		return 0;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		// We need the url
		if (Utils.isNullOrEmpty(sFileURL)) {
			return null;
		}
		
		try {
			
			// The url should have the both the asset and direct file urls
			String [] asUrlParts = sFileURL.split(";");
			
			String sAssetUrl = asUrlParts[0];
			String sDirectFileUrl = asUrlParts[1];
			
			// We support max retry
			int iAttemp = 0;
			
			while (iAttemp<iMaxRetry) {
				
				m_oLogger.debug("PLANETProviderAdapter.executeDownloadFile: start attemp #"+iAttemp);
				
				sDirectFileUrl = isAssetReady(sAssetUrl); 
				// Check if it is ready
				if (Utils.isNullOrEmpty(sDirectFileUrl)) {
					
					m_oLogger.debug("PLANETProviderAdapter.executeDownloadFile: asset not ready, try to activate it");
					
					// try to activate
					activateAsset(sAssetUrl);
					
					// We can wait for the asset to be ready
					for (int iWaitCycles = 0; iWaitCycles<m_iMaxActivationCycles; iWaitCycles++) {
						
						try {
							m_oLogger.debug("PLANETProviderAdapter.executeDownloadFile: waiting for activation, going to sleep for " + m_lActivationCyclesSleepSeconds + " [s]");
							
							Thread.sleep(m_lActivationCyclesSleepSeconds*1000);
							
							sDirectFileUrl = isAssetReady(sAssetUrl);
							
							if (!Utils.isNullOrEmpty(sDirectFileUrl)) {
								// After the sleep is ready, break the for cycle!!
								break;
							}
						}
						catch (Exception oEx) {
						}
						
					}					
				}
				
				sDirectFileUrl = isAssetReady(sAssetUrl);
				
				// here we double check if it is ready
				if (!Utils.isNullOrEmpty(sDirectFileUrl)) {
					
					m_oLogger.debug("PLANETProviderAdapter.executeDownloadFile: ok asset ready try to download file");
					
					// Yeah, we can dowload finally!!
					String sFile = downloadViaHttp(sDirectFileUrl, m_sProviderUser, "", sSaveDirOnServer);
					
					if (!Utils.isNullOrEmpty(sFile)) {
						return sFile;
					}
				}
				else {
					m_oLogger.debug("PLANETProviderAdapter.executeDownloadFile: no, asset still not ready");
				}
				
				// or not ready, or not downlaoded. Let see if we have other try...
				iAttemp ++;
				
				// Take a nap before
				try {
					m_oLogger.debug("PLANETProviderAdapter.executeDownloadFile: retry cycle, going to sleep for " + m_lRetrySleepSeconds + " [s]");
					Thread.sleep(m_lRetrySleepSeconds*1000);	
				}
				catch (Exception oSleepEx) {
				}
				
			}
			
		}
		catch (Exception oEx) {
			Utils.debugLog("PLANETProviderAdapter.getDownloadFileSize: " + oEx.toString());
		}
		
		return null;		
	}
	
	/**
	 * get the headers for calls to planet
	 * @return
	 */
	protected HashMap<String, String> getPlanetHeaders() {
		HashMap<String, String> asHeaders = new HashMap<String, String>();
		
		String sUserCredentials = m_sProviderUser + ":";
		try {
			String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
			asHeaders.put("Authorization", sBasicAuth);
		} catch (Exception oE) {
			Utils.debugLog("PLANETProviderAdapter.getPlanetHeaders: " + oE);
		}
		
		return asHeaders;
	}
	
	/**
	 * Check if an asset is ready
	 * @param sUrl Asset url
	 * @return true if ready, false otherwise
	 */
	protected String isAssetReady(String sUrl) {
		
		try {
			
			//Call the Asset URL
			String sResult = HttpUtils.standardHttpGETQuery(sUrl, getPlanetHeaders());
			
			// Convert the response in the relative JSON Map representation
			TypeReference<HashMap<String,Object>> oMapType = new TypeReference<HashMap<String,Object>>() {};
			HashMap<String,Object> oPlanetResponse = MongoRepository.s_oMapper.readValue(sResult, oMapType);
			
			if (oPlanetResponse.containsKey("analytic")) {
				
				HashMap<String,Object> oAnalytic = (HashMap<String,Object>) oPlanetResponse.get("analytic");
						
				if (oAnalytic.containsKey("status")) {
					String sStatus = oAnalytic.get("status").toString();
					
					if (sStatus.equals("active")) {
						
						if (oAnalytic.containsKey("location")) {
							return oAnalytic.get("location").toString();
						}
					}
				}				
			}
			
		}
		catch (Exception oEx) {
			Utils.debugLog("PLANETProviderAdapter.isAssetReady: " + oEx.toString());
		}	
		
		return null;
		
	}
	
	/**
	 * Put the request to activate the asset
	 * @param sUrl Asset url
	 */
	public void activateAsset(String sUrl) {
		
		try {
			
			// We query the asset
			String sResult = HttpUtils.standardHttpGETQuery(sUrl, getPlanetHeaders());
			
			// Convert the response in the relative JSON Map representation
			TypeReference<HashMap<String,Object>> oMapType = new TypeReference<HashMap<String,Object>>() {};
			HashMap<String,Object> oPlanetResponse = MongoRepository.s_oMapper.readValue(sResult, oMapType);
			
			// We need the link to activate
			if (oPlanetResponse.containsKey("_links")) {
				HashMap<String,Object> oLinks = (HashMap<String,Object>) oPlanetResponse.get("_links");
				
				if (oLinks.containsKey("activate")) {
					
					// Here it is!
					String sActivateLink = oLinks.get("activate").toString();
					
					// Post the request
					HttpUtils.httpPost(sActivateLink, null, getPlanetHeaders());
					
					m_oLogger.debug("PLANETProviderAdapter.activateAsset: activation request sent");
					
					return;
				}
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("PLANETProviderAdapter.activateAsset: " + oEx.toString());
		}
		
		m_oLogger.debug("PLANETProviderAdapter.activateAsset: impossible to send the activation request");
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		
		if (Utils.isNullOrEmpty(sFileURL)) {
			return "";
		}
		
		try {
			String [] asUrlParts = sFileURL.split(";");
			return asUrlParts[2] + ".tif";
		}
		catch (Exception oEx) {
			Utils.debugLog("PLANETProviderAdapter.getDownloadFileSize: " + oEx.toString());
		}
		
		return null;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.PLANET)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return -1;
	}

}

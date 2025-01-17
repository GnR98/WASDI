package wasdi.dataproviders;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;

public class TerrascopeProviderAdapter extends ProviderAdapter {

	/**
	 * Flag to know if we already authenticated to the Terrascope Data Center or no
	 */
	boolean m_bAuthenticated = false;

	/**
	 * Base path of the folder mounted with EO Data
	 */
	private String m_sProviderBasePath = "";    

	/**
	 * URL domain (i.e. https://services.terrascope.be/catalogue/products).
	 */
	private String m_sProviderUrlDomain = "";

	private static final String TERRASCOPE_URL_ZIPPER = "https://services.terrascope.be/package/zip";


	/**
	 * Basic constructor
	 */
	public TerrascopeProviderAdapter() {
		m_sDataProviderCode = "TERRASCOPE";
	}

	/**
	 * @param logger
	 */
	public TerrascopeProviderAdapter(LoggerWrapper logger) {
		super(logger);
		m_sDataProviderCode = "TERRASCOPE";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		//todo fail instead
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("TerrascopeProviderAdapter.GetDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}

		long lSizeInBytes = 0L;

		if (isHttpsProtocol(sFileURL)) {
			String sResult = "";

			if (sFileURL.contains("&size=")) {
				return Long.valueOf(sFileURL.substring(sFileURL.indexOf("&size=") + 6, sFileURL.indexOf(",", sFileURL.indexOf("&size="))));
			}

			try {
				//extract appropriate url
				StringBuilder oUrl = new StringBuilder(getZipperUrl(sFileURL));
				sFileURL = oUrl.toString();

				String sOpenidConnectToken = obtainTerrascopeOpenidConnectToken();
				Map<String, String> asHeaders = HttpUtils.getOpenIdConnectHeaders(sOpenidConnectToken);

				lSizeInBytes = HttpUtils.getDownloadFileSizeViaHttp(sFileURL, asHeaders);

				m_oLogger.debug("TerrascopeProviderAdapter.getDownloadFileSize: file size is: " + sResult);
			} catch (Exception oE) {
				m_oLogger.debug("TerrascopeProviderAdapter.getDownloadFileSize: could not extract file size due to " + oE);
			}
		}

		return lSizeInBytes;
	}

	private String obtainTerrascopeOpenidConnectToken() {
		String sDownloadUser = m_sProviderUser;
		String sDownloadPassword = m_sProviderPassword;
		String sUrl = "https://sso.vgt.vito.be/auth/realms/terrascope/protocol/openid-connect/token";
		String sClientId = "terracatalogueclient";

		return HttpUtils.obtainOpenidConnectToken(sUrl, sDownloadUser, sDownloadPassword, sClientId);
	}

	private String getZipperUrl(String sFileURL) {
		String sResult = "";

		try {
			sResult = sFileURL.split(",")[0];
		} catch (Exception oE) {
			m_oLogger.error("TerrascopeProviderAdapter.getZipperUrl: " + oE);
		}

		return sResult;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		Utils.debugLog("TerrascopeProviderAdapter.executeDownloadFile: try to get " + sFileURL);

		String sResult = "";

//		if (isFileProtocol(m_sDefaultProtocol)) {
//
//			String sPathLinux = null;
//			if (isFileProtocol(sFileURL)) {
//				sPathLinux = removePrefixFile(sFileURL);
//			} else if (isHttpsProtocol(sFileURL)) {
//				sPathLinux = extractFilePathFromHttpsUrl(sFileURL);
//			} else {
//				Utils.debugLog("TerrascopeProviderAdapter.executeDownloadFile: unknown protocol " + sFileURL);
//			}
//
//			if (sPathLinux != null) {
//				File oSourceFile = new File(sPathLinux);
//
//				if (oSourceFile != null && oSourceFile.exists()) {
//					sResult = copyFile("file:" + sPathLinux, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace, iMaxRetry);
//
//					return sResult;
//				}
//			}
//		}

		if (isHttpsProtocol(sFileURL)) {
			if (sFileURL.startsWith(TERRASCOPE_URL_ZIPPER) && sFileURL.contains("compressed_payload")) {

				List<String> aoUrls = extractPayloadFromUrl(sFileURL);
				if (aoUrls != null) {

					String sPayload = JsonUtils.stringify(aoUrls);
					sResult = downloadHttpsPost(sFileURL, sPayload, sSaveDirOnServer, iMaxRetry, sResult);

					if (!Utils.isNullOrEmpty(sResult)) {
						String sDesiredFileName = this.getFileName(sFileURL);

						String renamedFile = WasdiFileUtils.renameFile(sResult, sDesiredFileName);
						ZipFileUtils.fixZipFileInnerSafePath(renamedFile);

						return renamedFile;
					}

				}
			} else {
				sResult = downloadHttps(sFileURL, sSaveDirOnServer, iMaxRetry, sResult);
			}
		}

		return sResult;
	}

	private List<String> extractPayloadFromUrl(String sUrl) {
		String sDecodedUrl = sUrl;

		String sCompressedPayload = null;
		String sPayload = null;
		if (sDecodedUrl != null && sDecodedUrl.startsWith(TERRASCOPE_URL_ZIPPER) && sDecodedUrl.contains("?compressed_payload=")) {
			sCompressedPayload = sDecodedUrl.substring(sDecodedUrl.indexOf("?compressed_payload=") + "?compressed_payload=".length(), sDecodedUrl.indexOf("&size="));
			sPayload = uncompressPayload(sCompressedPayload);
			if (sPayload == null) {
				return null;
			}

			return JsonUtils.jsonToListOfStrings(sPayload);
		}

		return null;
	}

	private static String uncompressPayload(String sCompressedPayload) {
		String sPayload = null;

		try {
			sPayload = StringUtils.uncompressString(sCompressedPayload);
		} catch (IOException e) {
			Utils.debugLog("TerrascopeProviderAdapter.uncompressPayload: the payload cannot be uncompressed: " + e.getMessage());
		}
		
		return sPayload;
	}

	private String downloadHttps(String sFileURL, String sSaveDirOnServer, int iMaxRetry, String sResult) {

		StringBuilder oUrl = new StringBuilder(getZipperUrl(sFileURL));
		sFileURL = oUrl.toString();

		String sOpenidConnectToken = obtainTerrascopeOpenidConnectToken();
		Map<String, String> asHeaders = HttpUtils.getOpenIdConnectHeaders(sOpenidConnectToken);

		for (int iAttempt = 0; iAttempt < iMaxRetry; iAttempt ++) {

			Utils.debugLog("TerrascopeProviderAdapter.downloadHttps: attemp #" + iAttempt);

			try {
				sResult = downloadViaHttp(sFileURL, asHeaders, sSaveDirOnServer);
			} catch (Exception oEx) {
				Utils.debugLog("TerrascopeProviderAdapter.downloadHttps: exception in download via http call: " + oEx.toString());
			}

			if (!Utils.isNullOrEmpty(sResult)) {
				return sResult;
			}

			try {
				int iMsSleep = (int) ((Math.random() * 15_000) + 10_000);
				Thread.sleep(iMsSleep);
			} catch (Exception oEx) {
				Utils.debugLog("TerrascopeProviderAdapter.executeDownloadFile: exception in sleep for retry: " + oEx.toString());
			}
		}

		return sResult;
	}

	private String downloadHttpsPost(String sFileURL, String sPayload, String sSaveDirOnServer, int iMaxRetry, String sResult) {

		String sOpenidConnectToken = obtainTerrascopeOpenidConnectToken();
		Map<String, String> asHeaders = HttpUtils.getOpenIdConnectHeaders(sOpenidConnectToken);

		for (int iAttempt = 0; iAttempt < iMaxRetry; iAttempt ++) {

			Utils.debugLog("TerrascopeProviderAdapter.downloadHttpsPost: attemp #" + iAttempt);

			try {
				sResult = downloadViaHttpPost(sFileURL, asHeaders, sPayload, sSaveDirOnServer);
			} catch (Exception oEx) {
				Utils.debugLog("TerrascopeProviderAdapter.downloadHttpsPost: exception in download via http call: " + oEx.toString());
			}

			if (!Utils.isNullOrEmpty(sResult)) {
				return sResult;
			}

			try {
				int iMsSleep = (int) ((Math.random() * 15_000) + 10_000);
				Thread.sleep(iMsSleep);
			} catch (Exception oEx) {
				Utils.debugLog("TerrascopeProviderAdapter.downloadHttpsPost: exception in sleep for retry: " + oEx.toString());
			}
		}

		return sResult;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) return "";

		String sFileName = "";

		if (sFileURL.startsWith(TERRASCOPE_URL_ZIPPER) && sFileURL.contains("compressed_payload")) {
			String [] asParts = sFileURL.split(",");

			if (asParts != null) {
				sFileName = asParts[1];
			}
		} else {
			String [] asParts = sFileURL.split("/");

			if (asParts != null) {
				sFileName = asParts[asParts.length - 1];
			}
		}

		return sFileName;
	}

	@Override
	protected void internalReadConfig() {

		try {
			m_sDefaultProtocol = m_oDataProviderConfig.defaultProtocol; 
			m_sProviderBasePath = m_oDataProviderConfig.localFilesBasePath;
			m_sProviderUrlDomain = m_oDataProviderConfig.urlDomain;
		} catch (Exception e) {
			m_oLogger.error("TerrascopeProviderAdapter: Config reader is null");
		}
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {

		if (sPlatformType.equals(Platforms.SENTINEL1) 
//				|| sPlatformType.equals(Platforms.SENTINEL2)
				|| sPlatformType.equals(Platforms.DEM)
				|| sPlatformType.equals(Platforms.WORLD_COVER)) {
			if (isWorkspaceOnSameCloud()) {
				return DataProviderScores.SAME_CLOUD_DOWNLOAD.getValue();
			}
			else {
				return DataProviderScores.DOWNLOAD.getValue();
			}
		}

		return 0;
	}

}

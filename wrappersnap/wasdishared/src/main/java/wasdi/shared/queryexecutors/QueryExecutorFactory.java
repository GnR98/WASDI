/**
 * Created by Cristiano Nattero on 2019-02-06
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.cds.QueryExecutorCDS;
import wasdi.shared.queryexecutors.cm.QueryExecutorCM;
import wasdi.shared.queryexecutors.creodias.QueryExecutorCREODIAS;
import wasdi.shared.queryexecutors.eodc.QueryExecutorEODC;
import wasdi.shared.queryexecutors.gpm.QueryExecutorGPM;
import wasdi.shared.queryexecutors.lsa.QueryExecutorLSA;
import wasdi.shared.queryexecutors.onda.QueryExecutorONDA;
import wasdi.shared.queryexecutors.planet.QueryExecutorPLANET;
import wasdi.shared.queryexecutors.probav.QueryExecutorPROBAV;
import wasdi.shared.queryexecutors.sentinelhub.QueryExecutorSENTINEL;
import wasdi.shared.queryexecutors.sobloo.QueryExecutorSOBLOO;
import wasdi.shared.queryexecutors.statics.QueryExecutorSTATICS;
import wasdi.shared.queryexecutors.terrascope.QueryExecutorTerrascope;
import wasdi.shared.queryexecutors.viirs.QueryExecutorVIIRS;
import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorFactory {

	private static final Map<String, Supplier<QueryExecutor>> s_aoExecutors;

	static {
		Utils.debugLog("QueryExecutorFactory");
		final Map<String, Supplier<QueryExecutor>> aoMap = new HashMap<>();

		aoMap.put("ONDA", QueryExecutorONDA::new);
		aoMap.put("SENTINEL", QueryExecutorSENTINEL::new);
		aoMap.put("SOBLOO", QueryExecutorSOBLOO::new);
		aoMap.put("EODC", QueryExecutorEODC::new);
		aoMap.put("CREODIAS", QueryExecutorCREODIAS::new);
		aoMap.put("LSA", QueryExecutorLSA::new);
		aoMap.put("VIIRS", QueryExecutorVIIRS::new);
		aoMap.put("CDS", QueryExecutorCDS::new);
		aoMap.put("PROBAV", QueryExecutorPROBAV::new);
		aoMap.put("PLANET", QueryExecutorPLANET::new);
		aoMap.put("TERRASCOPE", QueryExecutorTerrascope::new);
		aoMap.put("STATICS", QueryExecutorSTATICS::new);
		aoMap.put("GPM", QueryExecutorGPM::new);
		aoMap.put("COPERNICUSMARINE", QueryExecutorCM::new);
		
		s_aoExecutors = Collections.unmodifiableMap(aoMap);
		
		Utils.debugLog("QueryExecutorFactory.static constructor, s_aoExecutors content:");
		for (String sKey : s_aoExecutors.keySet()) {
			Utils.debugLog("QueryExecutorFactory.s_aoExecutors key: " + sKey);
		}
	}

	private static QueryExecutor supply(String sProvider) {
		
		QueryExecutor oExecutor = null;
		if(null!=sProvider) {
			Supplier<QueryExecutor> oSupplier = s_aoExecutors.get(sProvider);
			if(null!=oSupplier) {
				oExecutor = oSupplier.get();
			}
		} else {
			Utils.debugLog("QueryExecutorFactory.QueryExecutor: sProvider is null");
		}
		return oExecutor;	
	}

	private static QueryExecutor getExecutor(String sProvider, AuthenticationCredentials oCredentials, String sParserConfigPath, String sAppConfigPath) {
		
		QueryExecutor oExecutor = null;

		try {
			oExecutor = supply(sProvider);
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorFactory.getExecutor: " + oE );
		}
		if( null != oExecutor) {
			try {
				oExecutor.setCredentials(oCredentials);
				oExecutor.setParserConfigPath(sParserConfigPath);
				oExecutor.setAppconfigPath(sAppConfigPath);
			} catch (Exception oE1) {
				Utils.debugLog("QueryExecutorFactory.getExecutor: " + oE1 );
			}
		} else {
			throw new NullPointerException("QueryExecutorFactory.getExecutor: could not get a non-null QueryExecutor" );
		}

		return oExecutor;
	}
	
	
	/**
	 * Get the Query Executor for a specific provider
	 * @param sProvider Provider code
	 * @return QueryExecutor of the specific provider
	 */
	public static  QueryExecutor getExecutor(String sProvider) {
		Utils.debugLog("QueryExecutorFactory.getExecutor, provider: " + sProvider);
		QueryExecutor oExecutor = null;
		try {
			if(null!=sProvider) {
				AuthenticationCredentials oCredentials = getCredentials(sProvider);
				
				DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(sProvider);
	
				String sParserConfigPath = oDataProviderConfig.parserConfig;
				String sAppConfigPath = WasdiConfig.Current.paths.missionsConfigFilePath;
				
				oExecutor = getExecutor(
						sProvider,
						oCredentials,
						sParserConfigPath, sAppConfigPath);
				
				oExecutor.getSupportedPlatforms().clear();
				
				for (String sSupportedPlatform : oDataProviderConfig.supportedPlatforms) {
					oExecutor.getSupportedPlatforms().add(sSupportedPlatform);
				}
				
				oExecutor.init();
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorFactory.getExecutor( " + sProvider + " ): " + oE);
		}
		return oExecutor;

	}

	/**
	 * Get Auth Credentials for a specific provider
	 * @param sProvider Provider Code
	 * @return AuthenticationCredentials entity
	 */
	private static  AuthenticationCredentials getCredentials(String sProvider) {
		
		AuthenticationCredentials oCredentials = null;
		try {
			DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(sProvider);
			
			oCredentials = new AuthenticationCredentials(oDataProviderConfig.user, oDataProviderConfig.password);
			
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorFactory.getCredentials( " + sProvider + " ): " + oE);
		}
		return oCredentials;
	}	

}

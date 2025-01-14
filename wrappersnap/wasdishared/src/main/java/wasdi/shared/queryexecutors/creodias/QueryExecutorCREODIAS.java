/**
 * Created by Cristiano Nattero on 2019-12-20
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.creodias;

import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.http.QueryExecutorHttpGet;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * Query Executor for the CREODIAS DIAS.
 * 
 * 
 * @author c.nattero
 *
 */
public class QueryExecutorCREODIAS extends QueryExecutorHttpGet {

	public QueryExecutorCREODIAS() {
		m_sProvider="CREODIAS";
		this.m_oQueryTranslator = new QueryTranslatorCREODIAS();
		this.m_oResponseTranslator = new ResponseTranslatorCREODIAS();
	}
	
	/**
	 * Overrided because it looks that CREODIAS can return the Sentinel1 files also named _COG.
	 * In this case we need to select the correct original one
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		try {
			String sClientQuery = "";
			
			sClientQuery = sProduct + " AND ( beginPosition:[1893-09-07T00:00:00.000Z TO 2893-09-07T00:00:00.000Z] AND endPosition:[1893-09-07T23:59:59.999Z TO 2893-09-07T23:59:59.999Z] ) ";
			
			String sPlatform = WasdiFileUtils.getPlatformFromSatelliteImageFileName(sProduct);
			
			if (!Utils.isNullOrEmpty(sPlatform)) {
				sClientQuery = sClientQuery + "AND (platformname:" + sPlatform + " )";
			}
			
			PaginatedQuery oQuery = new PaginatedQuery(sClientQuery, "0", "10", null, null);
			List<QueryResultViewModel> aoResults = executeAndRetrieve(oQuery);
			
			if (aoResults != null) {
				if (aoResults.size()>0) {
					
					QueryResultViewModel oResult = aoResults.get(0);
					
					if (sPlatform.equals(Platforms.SENTINEL1)) {
						// Clean it
						oResult = null;
					
						for (int iResults = 0; iResults<aoResults.size(); iResults++) {
							QueryResultViewModel oTempResult = aoResults.get(iResults);
														
							if (oTempResult.getTitle().equals(sProduct)) {
								oResult = oTempResult;
								break;
							}
						}
					}
					
					if (oResult!=null) {
						return oResult.getLink();
					}
				}
			}
			
			return "";					
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutor.getUriFromProductName: exception " + oEx.toString());
		}
		
		return "";
	}
	
}

/**
 * Created by Cristiano Nattero on 2020-01-21
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.sobloo;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.http.QueryExecutorHttpGet;

/**
 * Query executor for the SOBLOO DIAS.
 * 
 * SOBLOO uses Opensearch
 * 
 * @author c.nattero
 *
 */
public class QueryExecutorSOBLOO extends QueryExecutorHttpGet {

	public QueryExecutorSOBLOO() {
		m_sProvider="SOBLOO";
		this.m_oQueryTranslator = new QueryTranslatorSOBLOO();
		this.m_oResponseTranslator = new ResponseTranslatorSOBLOO();
		
		this.m_sUser = null;
		this.m_sPassword = null;
		
		m_bUseBasicAuthInHttpQuery = false;
		
		m_asSupportedPlatforms.add(Platforms.SENTINEL1);
		m_asSupportedPlatforms.add(Platforms.SENTINEL2);
		m_asSupportedPlatforms.add(Platforms.SENTINEL3);
	}

}
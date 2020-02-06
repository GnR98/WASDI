/**
 * Created by Cristiano Nattero on 2018-11-28
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.HashMap;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public abstract class DiasQueryTranslator {
	
	protected HashMap<String, String> keyMapping;
	protected HashMap<String, String> valueMapping;
	protected String m_sParserConfigPath;

	public String translateAndEncode(String sQueryFromClient) {
		Utils.debugLog("DiasQueryTranslator.translateAndEncode");
		return encode(translate(sQueryFromClient));
	}
	
	//translates from WASDI query (OpenSearch) to <derived class> format
	protected abstract String translate(String sQueryFromClient);

	protected String encode( String sDecoded ) {
		String sResult = sDecoded; 
		sResult = sResult.replace(" ", "%20");
		sResult = sResult.replaceAll("\"", "%22");
		//sResult = java.net.URLEncoder.encode(sDecoded, m_sEnconding);
		return sResult;
	}
	
	protected String prepareQuery(String sInput) {
		String sQuery = new String(sInput);
		//insert space before and after round brackets
		sQuery = sQuery.replaceAll("\\(", " \\( ");
		sQuery = sQuery.replaceAll("\\)", " \\) ");
		//remove space before and after square brackets 
		sQuery = sQuery.replaceAll(" \\[", "\\[");
		sQuery = sQuery.replaceAll("\\[ ", "\\[");
		sQuery = sQuery.replaceAll(" \\]", "\\]");
		sQuery = sQuery.replaceAll("\\] ", "\\]");
		sQuery = sQuery.replaceAll("POLYGON", "POLYGON ");
		sQuery = sQuery.replaceAll("\\: ", "\\:");
		sQuery = sQuery.replaceAll(" \\: ", "\\:");

		sQuery = sQuery.replaceAll("AND", " AND ");
		sQuery = sQuery.trim().replaceAll(" +", " ");
		return sQuery;
	}

	public void setParserConfigPath(String sParserConfigPath) {
		m_sParserConfigPath = sParserConfigPath;
	}
}

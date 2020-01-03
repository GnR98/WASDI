/**
 * Created by Cristiano Nattero on 2018-12-11
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.ArrayList;

import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public interface DiasResponseTranslator {
	
	public QueryResultViewModel translate( Object oResponseViewModel, String sProtocol );
	public ArrayList<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel, String sDownloadProtocol);
	
}

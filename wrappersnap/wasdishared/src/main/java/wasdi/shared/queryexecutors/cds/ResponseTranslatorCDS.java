package wasdi.shared.queryexecutors.cds;

import java.util.List;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * CDS is not a real catalogue. See QueryExecutorCDS for reason.
 * This class is just a placeholder
 * 
 * @author PetruPetrescu
 *
 */
public class ResponseTranslatorCDS extends ResponseTranslator {

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		return 0;
	}

}

/**
 * 
 */
package stats;

import java.util.List;

import com.google.api.services.sheets.v4.Sheets;

import wasdi.shared.business.Processor;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessorRepository;

/**
 * @author c.nattero
 *
 */
public class WasdiStats {
	
	private static Sheets s_oSheetsService;
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//configure
		try {
			//google sheets
			String sSheetId = ConfigReader.getPropValue("Google_sheet_ID");
			//needs OAuth2.0 cert
			s_oSheetsService = SheetsServiceUtil.getSheetsService();
			
			
			//mongo
			MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
			MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
			MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");

			//todo load list of nodes...
			
			
			WasdiStats oWasdiStats = new WasdiStats();
	
			//TODO store result in sheet
			oWasdiStats.getProcessorStats();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void getProcessorStats() {
		//TODO change return type into some form of reusable list
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();
	}

}

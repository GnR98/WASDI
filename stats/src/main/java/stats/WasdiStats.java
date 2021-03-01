/**
 * 
 */
package stats;

import java.util.List;

import wasdi.shared.business.Processor;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessorRepository;

/**
 * @author c.nattero
 *
 */
public class WasdiStats {
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//configure
		try {
			
			//mongo
			MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
			MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
			MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");

			//load list of nodes...
			
			//
			
			WasdiStats oWasdiStats = new WasdiStats();
			
			oWasdiStats.getProcessorStats();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public void getProcessorStats() {
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();
	}

}

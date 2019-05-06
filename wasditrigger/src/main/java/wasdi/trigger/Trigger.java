package wasdi.trigger;

import java.io.File;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import wasdi.jwasdilib.WasdiLib;
import wasdi.shared.business.Schedule;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ScheduleRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;

public class Trigger {
	
	String m_sLibBasePath = "/data/wasdi/";
	
	public Trigger() {
		try {
			MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
			MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
			MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
			MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
			m_sLibBasePath = ConfigReader.getPropValue("BASE_PATH", "/data/wasdi/");
		} 
		catch (Throwable oEx) {
			oEx.printStackTrace();
		} 
	}

	/**
	 * Static Logger that references the "MyApp" logger
	 */
	public static Logger s_oLogger = Logger.getLogger(Trigger.class);
	
	public static void main(String[] args) {
		System.out.println("WASDI TRIGGER START");
		
		try {
			//get jar directory
			File oCurrentFile = new File(Trigger.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			//configure log
			String sThisFilePath = oCurrentFile.getParentFile().getPath();
			DOMConfigurator.configure(sThisFilePath + "/log4j.xml");

		}
		catch(Exception exp)
		{
			//no log4j configuration
			System.err.println( "Trigger - Error loading log.  Reason: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exp) );
			System.exit(-1);
		}

		s_oLogger.debug("Trigger Start");


		// create the parser
		CommandLineParser oParser = new DefaultParser();

		// create Options object
		Options oOptions = new Options();
		
		oOptions.addOption("s","scheduleid", true, "schedule object id");

		String sScheduleId = "";

		try {

			// parse the command line arguments
			CommandLine oLine = oParser.parse( oOptions, args );

			if (oLine.hasOption("scheduleid")) {
				// Get the Operation Code
				sScheduleId  = oLine.getOptionValue("scheduleid");
			}
			else {
				s_oLogger.debug("No Schedule ID No party. Bye");
			}
						
			Trigger oTrigger = new Trigger();

			s_oLogger.debug("Executing Schedule for " + sScheduleId);

			// And Run
			oTrigger.executeTrigger(sScheduleId);

			s_oLogger.debug(new EndMessageProvider().getGood());
		}
		catch( ParseException exp ) {
			s_oLogger.error("Trigger Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exp));
			System.exit(-1);
		}
	}

	public void executeTrigger(String sScheduleId) {
		s_oLogger.debug("executeTrigger start ");
		
		// Create the Session and Schedule Repo
		SessionRepository oSessionRepository = new SessionRepository();
		ScheduleRepository oScheduleRepository = new ScheduleRepository();
		
		// Get the schedule
		Schedule oSchedule = oScheduleRepository.GetSchedule(sScheduleId);
		
		if (oSchedule == null) {
			s_oLogger.debug("schedule does not exists " + sScheduleId);
			return;
		}
		
		s_oLogger.debug("got schedule id " + sScheduleId);

		// Create the session
		UserSession oUserSession = new UserSession();
		oUserSession.setSessionId(Utils.GetRandomName());
		oUserSession.setUserId(oSchedule.getUserId());
		oUserSession.setLoginDate((double) new Date().getTime());
		oUserSession.setLastTouch((double) new Date().getTime());
		
		oSessionRepository.InsertSession(oUserSession);
		
		// Create the wasdi lib
		WasdiLib oWasdiLib = new WasdiLib();
		
		// Set min config to start
		oWasdiLib.setUser(oSchedule.getUserId());
		oWasdiLib.setActiveWorkspace(oSchedule.getWorkspaceId());
		oWasdiLib.setSessionId(oUserSession.getSessionId());
		oWasdiLib.setBasePath(m_sLibBasePath);
		oWasdiLib.setIsOnServer(true);
		oWasdiLib.setDownloadActive(false);
		
		// Init the lib
		if (oWasdiLib.internalInit()) {
			s_oLogger.debug("wasdi lib initialized");
			
			// Trigger the processor
			String sProcId = oWasdiLib.asynchExecuteProcessor(oSchedule.getProcessorName(), oSchedule.getParams());
			
			s_oLogger.debug("PROCESS SCHEDULED: got ProcId " + sProcId);
		}
		else {
			s_oLogger.debug("Error Initializing the WASDI Lib. Exit without scheduling");
		}
	}
}

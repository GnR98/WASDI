/**
 * 
 */
package stats;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.User;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.TypeConversionUtils;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class WasdiStats {


	//sheets
	private static Sheets s_oSheetsService;
	private static Map<String, Integer> s_aiSheetIds = new HashMap<>();
	private static String s_sSheetId = null;
	private static final String s_sWHAT_IF_GID = "what-if.gid";
	private static final String s_sUSERS_GID = "users.gid";
	private static final String s_sCOMMA_DELIMITER = ",";
	private static final long s_lMAX_BYTES = (long)((double)(10485760 * 0.8));

	//USERS
	private Map<String,User> m_aoAllUsers;

	//PROCESSWORKSPACE
	//user -> Map of lists of processors -> one list per tree
	private Map<String, Map<String, Set<ProcessWorkspace>>> m_aoAllProcessWorkspaces;
	//private Map<String, ProcessWorkspace> m_aoRootProcessWorkspaces;

	//WARNING: rows in spreadsheets start at 1, not 0
	private int m_iNextFreeRow = 1;


	//stats
	private long m_lTotalDurationOfRootProcesses = 0l;
	private Map <String, Long> m_alTotalDurationOfRootProcessesPerUser = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//configure
		try {

			//google sheets
			s_sSheetId = ConfigReader.getPropValue("Google_sheet_ID");
			String sWhatIfGid = ConfigReader.getPropValue(s_sWHAT_IF_GID);
			s_aiSheetIds.put(s_sWHAT_IF_GID, Integer.parseInt(sWhatIfGid));

			//needs OAuth2.0 cert (use a service key file to avoid interactive authorization)
			s_oSheetsService = SheetsServiceUtil.getSheetsService(ConfigReader.getPropValue("KeyFile"));

			//mongo
			MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
			MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
			MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");

			WasdiStats oWasdiStats = new WasdiStats();

			oWasdiStats.loadUsers();
			oWasdiStats.loadProcessWorkspacesToMem();

			oWasdiStats.writeUsersStats();
			
			//disabled: we have too many rows for google spreasdheets
			//oWasdiStats.writeProcessWorkspaceStats();
			
			oWasdiStats.getProcessorStats();

			



		} catch (Exception e) {
			e.printStackTrace();
		}

	}





	//------------------------------ USERS -----------------------------------------------


	private void loadUsers() {
		m_aoAllUsers = new HashMap<>();
		UserRepository oUserRepository = new UserRepository();
		List<User> aoUserList = oUserRepository.getAllUsers();
		for (User oUser : aoUserList) {
			if(null!=oUser && !Utils.isNullOrEmpty(oUser.getUserId())) {
				m_aoAllUsers.put(oUser.getUserId(), oUser);
			}
		}
	}


	private void writeUsersStats() {
		String sUsersGid = null;
		try {
			sUsersGid = ConfigReader.getPropValue(s_sUSERS_GID);
			s_aiSheetIds.put(s_sUSERS_GID, Integer.parseInt(sUsersGid));

			UserRepository oUserRepository = new UserRepository();
			int iRow = 1;
			//total users			
			ValueRange oBody = new ValueRange().setValues(
					TypeConversionUtils.getListOfListsOfObjects(
							Arrays.asList(
									Arrays.asList("total users", ""+ oUserRepository.countUsers())
									)
							)
					);
			//UpdateValuesResponse oResult = 
			s_oSheetsService.spreadsheets().values()
			.update(s_sSheetId, "'Users'!A" + iRow, oBody)
			.setValueInputOption("RAW").execute();
			++iRow;

			//users active in the last n days
			int iDays = 60;
			oBody = new ValueRange().setValues(
					TypeConversionUtils.getListOfListsOfObjects(
							Arrays.asList(
									Arrays.asList("users active in the last " + iDays + " days", ""+ oUserRepository.getActiveUsers(iDays).size())
									)
							)
					);

			//oResult = 
			s_oSheetsService.spreadsheets().values()
			.update(s_sSheetId, "'Users'!A" + iRow, oBody)
			.setValueInputOption("RAW").execute();

			++iRow;

			//all users
			List<User> aoUsers = oUserRepository.getAllUsers();
			//add one row for the header
			List<List<Object>> aoObjects = new ArrayList<>(aoUsers.size()+1);
			List<Object> aoUserAsList = new LinkedList<Object>();
			aoUserAsList.add("_id");
			aoUserAsList.add("userId");
			aoUserAsList.add("name");
			aoUserAsList.add("surname");
			aoUserAsList.add("validAfterFirstAccess");
			aoUserAsList.add("firstAccessUUID");
			aoUserAsList.add("authProviderService");
			aoUserAsList.add("googleIdToken");
			aoUserAsList.add("registrationDate");
			aoUserAsList.add("confirmationDate");
			aoUserAsList.add("lastLogin");
			aoUserAsList.add("defaultNode");
			aoUserAsList.add("description");
			aoUserAsList.add("link");
			aoObjects.add(aoUserAsList);
			for (User oUser : aoUsers) {
				aoUserAsList = new LinkedList<Object>();
				aoUserAsList.add(""+oUser.getId());
				aoUserAsList.add(""+oUser.getUserId());
				aoUserAsList.add(""+oUser.getName());
				aoUserAsList.add(""+oUser.getSurname());
				aoUserAsList.add(""+oUser.getPassword());
				aoUserAsList.add(""+oUser.getValidAfterFirstAccess());
				aoUserAsList.add(""+oUser.getFirstAccessUUID());
				aoUserAsList.add(""+oUser.getAuthServiceProvider());
				aoUserAsList.add(""+oUser.getGoogleIdToken());
				aoUserAsList.add(""+oUser.getRegistrationDate());
				aoUserAsList.add(""+oUser.getConfirmationDate());
				aoUserAsList.add(""+oUser.getLastLogin());
				aoUserAsList.add(""+oUser.getDefaultNode());
				aoUserAsList.add(""+oUser.getDescription());
				aoUserAsList.add(""+oUser.getLink());
				aoObjects.add(aoUserAsList);
			}


			List<ValueRange> oData = new ArrayList<>();
			oData.add(new ValueRange().setRange("'Users'!A"+iRow).setValues(aoObjects));
			BatchUpdateValuesRequest oBatchBody = new BatchUpdateValuesRequest()
					.setValueInputOption("USER_ENTERED")
					.setData(oData);
			//BatchUpdateValuesResponse oBatchResult = 
			s_oSheetsService.spreadsheets().values()
			.batchUpdate(s_sSheetId, oBatchBody).execute();

			/*
				oBody = new ValueRange().setValues( Arrays.asList(aoObjects));
				oResult = s_oSheetsService.spreadsheets().values()
						.update(sSheetId, "A5", oBody)
						.setValueInputOption("RAW").execute();

			 */

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}



	//----------------------------- PROCESSORS -----------------------------------

	private List<Processor> getProcessorStats() {
		//TODO change return type into some form of reusable list
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();
		System.out.println("WASDI currently has " + aoProcessors.size() + " processors");
		return aoProcessors;
	}



	//------------------------- PROCESSWORKSPACE ---------------------------


	private void loadProcessWorkspacesToMem() {
		//make room for results
		m_aoAllProcessWorkspaces = new HashMap<>();
		//m_aoRootProcessWorkspaces = new HashMap<>();

		Path oWorkingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		if(!oWorkingDirectory.toFile().isDirectory()) {
			Utils.debugLog("WasdiStats.writeProcessWorkspaceStats: current working directory is not a directory. Wait, what?! :-O");
			return;
		}
		try {
			m_iNextFreeRow = 1;
			Files.list(oWorkingDirectory).forEach(oPath -> loadProcessWorkspaceCsvToMem(oPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void loadProcessWorkspaceCsvToMem(Path oPath) {
		resetTimeCounters();
		try (BufferedReader oBufferedReader = new BufferedReader(new FileReader(oPath.toString()))) {
			String sLine;
			boolean bHeaderSkipped = false;
			while ((sLine = oBufferedReader.readLine()) != null) {
				//skip empty lines
				if(Utils.isNullOrEmpty(sLine) || !sLine.contains(s_sCOMMA_DELIMITER) ) {
					continue;
				}
				//skip first line too
				if(!bHeaderSkipped) {
					bHeaderSkipped = true;
					continue;
				}
				try {
					String[] asValues = sLine.split(s_sCOMMA_DELIMITER);
					if(asValues==null || asValues.length <= 0) {
						continue;
					}
					//instantiate ProcessWorkspace and populate it from string
					ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
					populateSingleProcessWorkspace(asValues, oProcessWorkspace);

					//make room for current user, if needed
					if(!m_aoAllProcessWorkspaces.containsKey(oProcessWorkspace.getUserId())) {
						m_aoAllProcessWorkspaces.put(oProcessWorkspace.getUserId(), new HashMap<>());
					}
					if(!m_alTotalDurationOfRootProcessesPerUser.containsKey(oProcessWorkspace.getUserId())){
						m_alTotalDurationOfRootProcessesPerUser.put(oProcessWorkspace.getUserId(), 0l);
					}

					//we're grouping the processes by parentId
					String sParentId = oProcessWorkspace.getParentId();
					if(Utils.isNullOrEmpty(sParentId)){
						//then this is the parent, i.e., the root node
						sParentId = oProcessWorkspace.getProcessObjId();

						//add duration
						long lCurrDuration = m_alTotalDurationOfRootProcessesPerUser.get(oProcessWorkspace.getUserId());
						lCurrDuration += WasdiStats.calcProcessDuration(oProcessWorkspace);
						m_alTotalDurationOfRootProcessesPerUser.put(oProcessWorkspace.getUserId(), lCurrDuration);
					}
					if(!m_aoAllProcessWorkspaces.get(oProcessWorkspace.getUserId()).containsKey(sParentId)){
						m_aoAllProcessWorkspaces.get(oProcessWorkspace.getUserId()).put(sParentId, new HashSet<>());
					}
					m_aoAllProcessWorkspaces.get(oProcessWorkspace.getUserId()).get(sParentId).add(oProcessWorkspace);					
				} catch (Exception oE) {
					Utils.debugLog("WasdiStats.loadProcessWorkspaceCsv: while loading " + sLine + ": " + oE);
				}
			}
		}catch (Exception oE) {
			Utils.debugLog("WasdiStats.loadProcessWorkspaceCsvDump( " + oPath.toString() +  " ): failed reading file: " + oE);
		}
	}





	private void populateSingleProcessWorkspace(String[] asValues, ProcessWorkspace oProcessWorkspace) {
		/*
		"productName,workspaceId,userId,operationType,"
		"operationSubType,operationDate,operationStartDate,operationEndDate,"
		"lastStateChangeDate,processObjId,fileSize,status,"
		"progressPerc,pid,payload,nodeCode,"
		"parentId,subprocessPid"
		 */
		oProcessWorkspace.setProductName(asValues[0]);
		oProcessWorkspace.setWorkspaceId(asValues[1]);
		oProcessWorkspace.setUserId(asValues[2]);
		oProcessWorkspace.setOperationType(asValues[3]);
		oProcessWorkspace.setOperationSubType(asValues[4]);
		oProcessWorkspace.setOperationDate(asValues[5]);
		oProcessWorkspace.setOperationStartDate(asValues[6]);
		oProcessWorkspace.setOperationEndDate(asValues[7]);
		oProcessWorkspace.setLastStateChangeDate(asValues[8]);
		oProcessWorkspace.setProcessObjId(asValues[9]);
		oProcessWorkspace.setFileSize(asValues[10]);
		oProcessWorkspace.setStatus(asValues[11]);
		oProcessWorkspace.setProgressPerc(Integer.parseInt(asValues[12]));
		oProcessWorkspace.setPid(Integer.parseInt(asValues[13]));
		oProcessWorkspace.setPayload(asValues[14]);
		oProcessWorkspace.setParentId(asValues[15]);
		oProcessWorkspace.setSubprocessPid(Integer.parseInt(asValues[16]));
	}

	public static long calcProcessDuration(ProcessWorkspace oProcessWorkspace) {
		if(null == oProcessWorkspace) {
			return 0l;
		}
		Long lResult = 0l;
		try {
			String sStart = oProcessWorkspace.getOperationStartDate();
			if(Utils.isNullOrEmpty(sStart)) {
				sStart = oProcessWorkspace.getOperationDate();
			}
			String sEnd = oProcessWorkspace.getOperationEndDate();
			if(Utils.isNullOrEmpty(sEnd)) {
				sEnd = oProcessWorkspace.getLastStateChangeDate();
			}
			if(Utils.isNullOrEmpty(sEnd) || Utils.isNullOrEmpty(sStart) || sStart.toLowerCase().equals("null") || sEnd.toLowerCase().equals("null")) {
				return 0l;
			} 
			try {
				String sFormat = "yyyy-MM-dd HH:mm:ss";
				long lEnd = TimeEpochUtils.fromDateStringToEpoch(sEnd, sFormat);
				long lStart = TimeEpochUtils.fromDateStringToEpoch(sStart, sFormat);
				if(lEnd < lStart) {
					Utils.debugLog("WasdiStats.calcProcessDuration: process " + oProcessWorkspace.getProcessObjId() + " has inverted dates: op= " + oProcessWorkspace.getOperationDate() +
							", st= " + oProcessWorkspace.getOperationStartDate() + ", en= " + oProcessWorkspace.getOperationEndDate() + ", la= " + oProcessWorkspace.getLastStateChangeDate());
					return 0l;
				}
				lResult = lEnd - lStart;
			} catch (Exception oE) {
				Utils.debugLog("WasdiStats.calcProcessDuration: could not calculate difference from " + sStart + " to " + sEnd + " due to: " + oE);
				lResult = 0l;
			}
		} catch (Exception oE) {
			Utils.debugLog("WasdiStats.calcProcessDuration: " + oE ); 
		}
		return lResult;
	}



	public void resetTimeCounters() {
		m_lTotalDurationOfRootProcesses = 0l;
		if(null==m_alTotalDurationOfRootProcessesPerUser) {
			m_alTotalDurationOfRootProcessesPerUser = new HashMap<>();
		}
		m_alTotalDurationOfRootProcessesPerUser.clear();
	}
	
	
	public void calcRunTime() {
		resetTimeCounters();
		
		for (Entry<String, Map<String, Set<ProcessWorkspace>>>  aoUserEntry : m_aoAllProcessWorkspaces.entrySet()) {
			//init the count if user does not exist in counter yet
			if(!m_alTotalDurationOfRootProcessesPerUser.containsKey(aoUserEntry.getKey())) {
				m_alTotalDurationOfRootProcessesPerUser.put(aoUserEntry.getKey(), 0l);
			}
			for(Entry<String, Set<ProcessWorkspace>> oProcessesEntry : aoUserEntry.getValue().entrySet()) {	
				//oProcessesEntry.getValue().stream().
			}

		}
	}


	private void loadProcessWorkspaceToSheet() {
		Path oWorkingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		if(!oWorkingDirectory.toFile().isDirectory()) {
			Utils.debugLog("WasdiStats.writeProcessWorkspaceStats: current working directory is not a directory. Wait, what?! :-O");
			return;
		}
		try {
			m_iNextFreeRow = 1;
			Files.list(oWorkingDirectory).forEach(oPath -> readCsvToSheet(oPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readCsvToSheet(Path oPath){
		try {
			//exclude irrelevant entries
			if(oPath.toFile().isDirectory() || !oPath.toString().toLowerCase().endsWith(".csv")) {
				return;
			}
			Utils.debugLog("WasdiStats.readCsv: processing file " + oPath.getFileName());

			//read and upload as soon as possible
			List<List<Object>> aoCsvContent = new LinkedList<>();
			try (BufferedReader oBufferedReader = new BufferedReader(new FileReader(oPath.toString()))) {
				String sLine;
				int iCurrentLine = 0;
				boolean bHeaderSkipped = false;
				long lCumulativeSize = 0l;
				while ((sLine = oBufferedReader.readLine()) != null) {
					//skip empty lines
					if(Utils.isNullOrEmpty(sLine) || !sLine.contains(s_sCOMMA_DELIMITER) ) {
						continue;
					}
					//skip first line too
					if(!bHeaderSkipped) {
						bHeaderSkipped = true;
						continue;
					}

					long lCurLineSize = (long)(sLine.toCharArray().length * Character.BYTES); 
					if(lCurLineSize + lCumulativeSize > s_lMAX_BYTES) {
						//stop here, adding more would violate limit
						//upload block of lines read so far

						Utils.debugLog("WasdiStats.readCsv: uploading " + aoCsvContent.size() + " lines, size: " + lCumulativeSize + ", rows so far: " + iCurrentLine);
						ValueRange oBody = new ValueRange().setValues(aoCsvContent);
						UpdateValuesResponse oResult = s_oSheetsService.spreadsheets().values()
								.update(s_sSheetId, "'ProcessWorkspace'!A"+m_iNextFreeRow, oBody)
								.setValueInputOption("RAW").execute();
						Utils.debugLog("WasdiStats.readCsv: updated " + oResult.getUpdatedCells() + " cells");
						m_iNextFreeRow += iCurrentLine;
						//reset structure and size counter
						aoCsvContent.clear();
						lCumulativeSize = 0l;
					} 

					//buffer not full yet, keep adding
					lCumulativeSize += lCurLineSize;
					Object[] aoValues = sLine.split(s_sCOMMA_DELIMITER);
					aoCsvContent.add(Arrays.asList(aoValues));
					iCurrentLine++;
				}
			}catch (Exception oE) {
				Utils.debugLog("WasdiStats.loadProcessWorkspaceCsvDump( " + oPath.toString() +  " ): failed reading file: " + oE);
			}
			//skip the first line (it's just the header)
			try {
				aoCsvContent = aoCsvContent.subList(1, aoCsvContent.size());
			} catch (Exception oE) {
				Utils.debugLog("WasdiStats.loadProcessWorkspaceCsvDump( " + oPath.toString() +  " ): could not remove first line: " + oE);
			}
		} catch (Exception oE) {
			Utils.debugLog("WasdiStats.loadProcessWorkspaceCsvDump( " + oPath.toString() +  " ): " + oE);
		}
	}


}

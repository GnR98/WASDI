/**
 * 
 */
package stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.User;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.TypeConversionUtils;

/**
 * @author c.nattero
 *
 */
public class WasdiStats {


	private static Sheets s_oSheetsService;
	private static Map<String, Integer> s_aiSheetIds = new HashMap<>();
	private static final String s_sWHAT_IF_GID = "what-if.gid";
	private static final String s_sUSERS_GID = "users.gid";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//configure
		try {

			//google sheets
			String sSheetId = ConfigReader.getPropValue("Google_sheet_ID");
			String sWhatIfGid = ConfigReader.getPropValue(s_sWHAT_IF_GID);
			s_aiSheetIds.put(s_sWHAT_IF_GID, Integer.parseInt(sWhatIfGid));

			//needs OAuth2.0 cert (use a service key file to avoid interactive authorization)
			s_oSheetsService = SheetsServiceUtil.getSheetsService(ConfigReader.getPropValue("KeyFile"));

			//mongo
			MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
			MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
			MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");

			WasdiStats oWasdiStats = new WasdiStats();

			oWasdiStats.writeUsersStats(sSheetId);
			
			oWasdiStats.writeProcessWorkspaceStats(sSheetId);



		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	private void writeProcessWorkspaceStats(String sSheetId) {
		ProcessWorkspaceRepository oProcessWorkspace = new ProcessWorkspaceRepository();
		List<ProcessWorkspace> aoProcessWorkspaces = oProcessWorkspace.getAll();
		//TODO dump list onto sheet 
		
		//TODO do this for every node... how to connect? Rather use a new API?
		
		
	}


	private void writeUsersStats(String sSheetId) {
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
			UpdateValuesResponse oResult = s_oSheetsService.spreadsheets().values()
					.update(sSheetId, "'Users'!A" + iRow, oBody)
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
			
			oResult = s_oSheetsService.spreadsheets().values()
					.update(sSheetId, "'Users'!A" + iRow, oBody)
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
			BatchUpdateValuesResponse oBatchResult = s_oSheetsService.spreadsheets().values()
					.batchUpdate(sSheetId, oBatchBody).execute();

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



	private List<Processor> getProcessorStats() {
		//TODO change return type into some form of reusable list
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();
		System.out.println("WASDI currently has " + aoProcessors.size() + " processors");
		return aoProcessors;
	}

}

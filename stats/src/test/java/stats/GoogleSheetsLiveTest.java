package stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.CopyPasteRequest;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateSpreadsheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import wasdi.shared.utils.TypeConversionUtils;

public class GoogleSheetsLiveTest {

	private static Sheets s_oSheetsService;

	// this id can be replaced with your spreadsheet id
	// otherwise be advised that multiple people may run this test and update the
	// public spreadsheet
	
	//TODO replace with a suitable spreadsheet ID
	private static final String SPREADSHEET_ID = "1sILuxZUnyl_7-MlNThjt765oWshN3Xs-PPLfqYe4DhI";


	@BeforeClass
	public static void setup() throws GeneralSecurityException, IOException {
		s_oSheetsService = SheetsServiceUtil.getSheetsService();
	}

	@Test
	public void whenWriteSheet_thenReadSheetOk() throws IOException {
		//set of rows
		ValueRange oBody = new ValueRange().setValues(
				TypeConversionUtils.getListOfListsOfObjects(
							Arrays.asList(
								Arrays.asList("Expenses January"),
								Arrays.asList("books", "30"),
								Arrays.asList("pens", "10"),
								Arrays.asList("Expenses February"),
								Arrays.asList("clothes", "20"),
								Arrays.asList("shoes", "5")
							)
						)
				);
		UpdateValuesResponse oResult = s_oSheetsService.spreadsheets().values()
				.update(SPREADSHEET_ID, "A1", oBody)
				.setValueInputOption("RAW").execute();

		
		
		List<ValueRange> oData = new ArrayList<>();
		oData.add(new ValueRange().setRange("D1").setValues(
				TypeConversionUtils.getListOfListsOfObjects(Arrays.asList(Arrays.asList("January Total", "=B2+B3"))))
			);
		oData.add(new ValueRange().setRange("D4").setValues(
				TypeConversionUtils.getListOfListsOfObjects(Arrays.asList(Arrays.asList("February Total", "=B5+B6"))))
			);

		BatchUpdateValuesRequest oBatchBody = new BatchUpdateValuesRequest()
				.setValueInputOption("USER_ENTERED")
				.setData(oData);
		BatchUpdateValuesResponse oBatchResult = s_oSheetsService.spreadsheets().values()
				.batchUpdate(SPREADSHEET_ID, oBatchBody).execute();

		List<String> asRanges = Arrays.asList("E1", "E4");
		BatchGetValuesResponse readResult = s_oSheetsService.spreadsheets().values()
				.batchGet(SPREADSHEET_ID).setRanges(asRanges).execute();

		ValueRange oJanuaryTotal = readResult.getValueRanges().get(0);
		assertThat(oJanuaryTotal.getValues().get(0).get(0)).isEqualTo("40");

		ValueRange oFebruaryTotal = readResult.getValueRanges().get(1);
		assertThat(oFebruaryTotal.getValues().get(0).get(0)).isEqualTo("25");

		ValueRange oAppendBody = new ValueRange().setValues(TypeConversionUtils.getListOfListsOfObjects(Arrays.asList(Arrays.asList("Total", "=E1+E4"))));

		AppendValuesResponse oAppendResult = s_oSheetsService.spreadsheets().values()
				.append(SPREADSHEET_ID, "A1", oAppendBody).setValueInputOption("USER_ENTERED")
				.setInsertDataOption("INSERT_ROWS").setIncludeValuesInResponse(true).execute();

		ValueRange oTotal = oAppendResult.getUpdates().getUpdatedData();
		
		assertThat(oTotal.getValues().get(0).get(1)).isEqualTo("65");
	}

	@Test
	public void whenUpdateSpreadSheetTitle_thenOk() throws IOException {

		UpdateSpreadsheetPropertiesRequest oUpdateRequest = new UpdateSpreadsheetPropertiesRequest().setFields("*")
				.setProperties(new SpreadsheetProperties().setTitle("Expenses"));

		GridRange oSource = new GridRange()
				.setSheetId(0)
				.setStartColumnIndex(0).setEndColumnIndex(2)
				.setStartRowIndex(0).setEndRowIndex(1);
		GridRange oDestination = new GridRange()
				//TODO check appropriateness of sheet ID
				.setSheetId(1)
				.setStartColumnIndex(0).setEndColumnIndex(2)
				.setStartRowIndex(0).setEndRowIndex(1);
		CopyPasteRequest oCopyRequest = new CopyPasteRequest()
				.setSource(oSource)
				.setDestination(oDestination)
				.setPasteType("PASTE_VALUES");

		List<Request> aoRequests = new ArrayList<>();
		aoRequests.add(new Request().setCopyPaste(oCopyRequest));
		aoRequests.add(new Request().setUpdateSpreadsheetProperties(oUpdateRequest));

		BatchUpdateSpreadsheetRequest oBody = new BatchUpdateSpreadsheetRequest().setRequests(aoRequests);

		BatchUpdateSpreadsheetResponse oResponse = null;
		try {
			oResponse = s_oSheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, oBody).execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(oResponse.toPrettyString());

	}

	@Test
	public void whenCreateSpreadSheet_thenIdOk() throws IOException {
		Spreadsheet oSpreadSheet = new Spreadsheet()
				.setProperties(new SpreadsheetProperties().setTitle("My Spreadsheet"));
		Spreadsheet oResult = s_oSheetsService.spreadsheets().create(oSpreadSheet).execute();

		assertThat(oResult.getSpreadsheetId()).isNotNull();
	}

}
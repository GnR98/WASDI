package wasdi.shared.queryexecutors.gpm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * GPM Query Executor.
 * This class queries the GPM Catalogue from https://cds.climate.copernicus.eu/api/v2/
 * 
 * The query in this case is local: the catalogue does not have API.
 * GPM are regular (half-hourly, monthly) maps in a full world format.
 * 
 * The query is obtained with a local shape file that reproduces the GPM grid
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorGPM extends QueryExecutor {

	private static final String URL_TEXT_LATE = "https://jsimpsonhttps.pps.eosdis.nasa.gov/text/imerg/gis/";
	private static final String URL_TEXT_EARLY = "https://jsimpsonhttps.pps.eosdis.nasa.gov/text/imerg/gis/early/";
	private static final String URL_LATE = "https://jsimpsonhttps.pps.eosdis.nasa.gov/imerg/gis/";
	private static final String URL_EARLY = "https://jsimpsonhttps.pps.eosdis.nasa.gov/imerg/gis/early/";

	private static final String EARLY = "Early";
	private static final String LATE = "Late";

	private static final String DURATION_HHR = "HHR";
	private static final String DURATION_DAY = "DAY";
	private static final String DURATION_MO = "MO";

	private static final String ACCUMULATION_3DAY = "3day";
	private static final String ACCUMULATION_3DAY_ONLY_FOR_LATE = "3day - only for Late";
	private static final String ACCUMULATION_7DAY = "7day";
	private static final String ACCUMULATION_7DAY_ONLY_FOR_LATE = "7day - only for Late";
	private static final String ACCUMULATION_ALL = "All";

	private static final String EXTENSION_TIF = ".tif";

	private static DataProviderConfig s_oDataProviderConfig;

	public QueryExecutorGPM() {
		m_sProvider = "GPM";
		s_oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sProvider);

		this.m_oQueryTranslator = new QueryTranslatorGPM();
		this.m_oResponseTranslator = new ResponseTranslatorGPM();
	}

	/**
	 * Overload of the get URI from Product Name method.
	 * For Terrascope, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		if (sProduct.toUpperCase().startsWith("3B-")
				|| sProduct.toUpperCase().contains("IMERG")) {
			return sOriginalUrl;
		}
		return null;
	}

	@Override
	public int executeCount(String sQuery) {
		Utils.debugLog("QueryExecutorGPM.executeCount | sQuery: " + sQuery);

		int iCount = 0;

		// Parse the query
		QueryViewModel oGPMQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oGPMQuery.platformName)) {
			return iCount;
		}


		final String sQueryStartFromDate = oGPMQuery.startFromDate;
		final String sQueryEndToDate = oGPMQuery.endToDate;

		final Date oStartFromDate;
		final Date oEndToDate;
		
		Date oStartFromDateProvided = null;
		Date oEndToDateProvided = null;

		if (!Utils.isNullOrEmpty(sQueryStartFromDate) && !Utils.isNullOrEmpty(sQueryEndToDate)) {
			oStartFromDateProvided = Utils.getYyyyMMddTZDate(sQueryStartFromDate);
			oEndToDateProvided = Utils.getYyyyMMddTZDate(sQueryEndToDate);
		}

		if (oStartFromDateProvided == null || oEndToDateProvided == null) {
			oStartFromDate = getDefaultStartDate();
			oEndToDate = getDefaultEndDate();
		} else {
			if (oEndToDateProvided.before(oStartFromDateProvided)) {
				Utils.debugLog("QueryExecutorGPM.executeCount | the end date preceedes the start date. oStartFromDate = " + oStartFromDateProvided + "; " + "oEndToDate = " + oEndToDateProvided + "; Inverting the dates.");

				oStartFromDate = oEndToDateProvided;
				oEndToDate = oStartFromDateProvided;
			} else {
				oStartFromDate = oStartFromDateProvided;
				oEndToDate = oEndToDateProvided;
			}
		}

		String sBaseUrl;

		final String sDuration = oGPMQuery.productType;
		final String sAccumulation;
		final String sExtension;

		if (EARLY.equalsIgnoreCase(oGPMQuery.productName)) {
			sBaseUrl = URL_TEXT_EARLY;

			if (DURATION_DAY.equalsIgnoreCase(oGPMQuery.productType)
					|| DURATION_MO.equalsIgnoreCase(oGPMQuery.productType)) {
				sAccumulation = null;
				sExtension = ".zip";
			} else if (ACCUMULATION_ALL.equalsIgnoreCase(oGPMQuery.productLevel)
					|| ACCUMULATION_3DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)
					|| ACCUMULATION_7DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
				sAccumulation = null;
				sExtension = EXTENSION_TIF;
			} else {
				sAccumulation = oGPMQuery.productLevel;
				sExtension = EXTENSION_TIF;
			}
		} else {
			sBaseUrl = URL_TEXT_LATE;

			if (DURATION_DAY.equalsIgnoreCase(oGPMQuery.productType)
					|| DURATION_MO.equalsIgnoreCase(oGPMQuery.productType)) {
				sAccumulation = null;
				sExtension = ".zip";
			} else if (ACCUMULATION_ALL.equalsIgnoreCase(oGPMQuery.productLevel)) {
				sAccumulation = null;
				sExtension = EXTENSION_TIF;
			} else if (ACCUMULATION_3DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
				sAccumulation = ACCUMULATION_3DAY;
				sExtension = EXTENSION_TIF;
			} else if (ACCUMULATION_7DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
				sAccumulation = ACCUMULATION_7DAY;
				sExtension = EXTENSION_TIF;
			} else {
				sAccumulation = oGPMQuery.productLevel;
				sExtension = EXTENSION_TIF;
			}
		}


		List<String> aoMonthsList = getMonthsBetweenDatesIncluding(oStartFromDate, oEndToDate);

		for (String sMonth : aoMonthsList) {
			if (Utils.isNullOrEmpty(sMonth)) {
				continue;
			}

			String sUrl = sBaseUrl + sMonth;
			String sHtmlPageSource = performRequest(sUrl);

			if (Utils.isNullOrEmpty(sHtmlPageSource)) {
				continue;
			}

			int indexOfText = sUrl.indexOf("/text/");
			String relativePath = sUrl.substring(indexOfText + 5);
			List<QueryCountResponseEntry> list = parseCountResponse(sHtmlPageSource, relativePath);

			List<QueryCountResponseEntry> filteredList = list.stream()
					.filter(t -> sDuration.equalsIgnoreCase(t.getDuration()))
					.filter(t -> sExtension.equalsIgnoreCase(t.getExtension()))
					.filter(t -> sAccumulation == null || sAccumulation.equalsIgnoreCase(t.getAccumulation()))
					.filter(t -> DURATION_MO.equalsIgnoreCase(t.getDuration()) || !(oStartFromDate.after(t.getDate())))
					.filter(t -> DURATION_MO.equalsIgnoreCase(t.getDuration()) || !(oEndToDate.before(t.getDate())))
					.collect(Collectors.toList());


			iCount += filteredList.size();
		}

		return iCount;
	}

	private static List<String> getMonthsBetweenDatesIncluding(Date oStartDate, Date oEndDate) {
		List<String> monthsList = new ArrayList<>();

		Calendar oStartDateCalendar = Calendar.getInstance();
		oStartDateCalendar.setTimeInMillis(oStartDate.getTime());

		Calendar oEndDateCalendar = Calendar.getInstance();
		oEndDateCalendar.setTimeInMillis(oEndDate.getTime());

		int iStartDateYear = oStartDateCalendar.get(Calendar.YEAR);
		int iStartDateMonth = oStartDateCalendar.get(Calendar.MONTH);

		int iEndDateYear = oEndDateCalendar.get(Calendar.YEAR);
		int iEndDateMonth = oEndDateCalendar.get(Calendar.MONTH);

		int iStartingMonth = iStartDateYear * 12 + iStartDateMonth;
		int iEndingMonth = iEndDateYear * 12 + iEndDateMonth;
		for (int i = iStartingMonth; i <= iEndingMonth; i++) {
			String sEntry = "" + (i / 12) + "/" + String.format("%02d", ((i % 12) + 1)) + "/";
			monthsList.add(sEntry);
		}

		return monthsList;
	}

	private static Date getDefaultStartDate() {
		Date now = new Date();

		Calendar oStartDateCalendar = Calendar.getInstance();
		oStartDateCalendar.setTimeInMillis(now.getTime());
		oStartDateCalendar.set(Calendar.HOUR, 0);
		oStartDateCalendar.set(Calendar.MINUTE, 0);
		oStartDateCalendar.set(Calendar.SECOND, 0);
		oStartDateCalendar.set(Calendar.MILLISECOND, 0);

		oStartDateCalendar.add(Calendar.DAY_OF_MONTH, -7);

		return oStartDateCalendar.getTime();
	}

	private static Date getDefaultEndDate() {
		Date now = new Date();

		Calendar oEndDateCalendar = Calendar.getInstance();
		oEndDateCalendar.setTimeInMillis(now.getTime());
		oEndDateCalendar.set(Calendar.HOUR, 23);
		oEndDateCalendar.set(Calendar.MINUTE, 59);
		oEndDateCalendar.set(Calendar.SECOND, 59);
		oEndDateCalendar.set(Calendar.MILLISECOND, 999);

		return oEndDateCalendar.getTime();
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		Utils.debugLog("QueryExecutorGPM.executeAndRetrieve | sQuery: " + oQuery.getQuery());

		try {
			List<QueryResultViewModel> aoResults = new ArrayList<>();
 
			// Parse the query
			QueryViewModel oGPMQuery = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

			if (!m_asSupportedPlatforms.contains(oGPMQuery.platformName)) {
				return aoResults;
			}

			String sOffset = oQuery.getOffset();
			String sLimit = oQuery.getLimit();

			int iOffset = 0;
			int iLimit = 10;

			try {
				iOffset = Integer.parseInt(sOffset);
			} catch (Exception oE) {
				Utils.debugLog("QueryExecutorGPM.executeAndRetrieve: " + oE.toString());
			}

			try {
				iLimit = Integer.parseInt(sLimit);
			} catch (Exception oE) {
				Utils.debugLog("QueryExecutorGPM.executeAndRetrieve: " + oE.toString());
			}

			
			final String sStartFromDate = oGPMQuery.startFromDate;
			final String sEndToDate = oGPMQuery.endToDate;

			final Date oStartFromDate;
			final Date oEndToDate;
			
			Date oStartFromDateProvided = null;
			Date oEndToDateProvided = null;

			if (!Utils.isNullOrEmpty(sStartFromDate) && !Utils.isNullOrEmpty(sEndToDate)) {
				oStartFromDateProvided = Utils.getYyyyMMddTZDate(sStartFromDate);
				oEndToDateProvided = Utils.getYyyyMMddTZDate(sEndToDate);
			}

			if (oStartFromDateProvided == null || oEndToDateProvided == null) {
				oStartFromDate = getDefaultStartDate();
				oEndToDate = getDefaultEndDate();
			} else {
				if (oEndToDateProvided.before(oStartFromDateProvided)) {
					Utils.debugLog("QueryExecutorGPM.executeCount | the end date preceedes the start date. oStartFromDate = " + oStartFromDateProvided + "; " + "oEndToDate = " + oEndToDateProvided + "; Inverting the dates.");

					oStartFromDate = oEndToDateProvided;
					oEndToDate = oStartFromDateProvided;
				} else {
					oStartFromDate = oStartFromDateProvided;
					oEndToDate = oEndToDateProvided;
				}
			}

			String sBaseUrl;

			final String sDuration = oGPMQuery.productType;
			final String sAccumulation;
			final String sExtension;

			if (EARLY.equalsIgnoreCase(oGPMQuery.productName)) {
				sBaseUrl = URL_EARLY;

				if (DURATION_DAY.equalsIgnoreCase(oGPMQuery.productType)
						|| DURATION_MO.equalsIgnoreCase(oGPMQuery.productType)) {
					sAccumulation = null;
					sExtension = ".zip";
				} else if (ACCUMULATION_ALL.equalsIgnoreCase(oGPMQuery.productLevel)
						|| ACCUMULATION_3DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)
						|| ACCUMULATION_7DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
					sAccumulation = null;
					sExtension = EXTENSION_TIF;
				} else {
					sAccumulation = oGPMQuery.productLevel;
					sExtension = EXTENSION_TIF;
				}
			} else {
				sBaseUrl = URL_LATE;

				if (DURATION_DAY.equalsIgnoreCase(oGPMQuery.productType)
						|| DURATION_MO.equalsIgnoreCase(oGPMQuery.productType)) {
					sAccumulation = null;
					sExtension = ".zip";
				} else if (ACCUMULATION_ALL.equalsIgnoreCase(oGPMQuery.productLevel)) {
					sAccumulation = null;
					sExtension = EXTENSION_TIF;
				} else if (ACCUMULATION_3DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
					sAccumulation = ACCUMULATION_3DAY;
					sExtension = EXTENSION_TIF;
				} else if (ACCUMULATION_7DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
					sAccumulation = ACCUMULATION_7DAY;
					sExtension = EXTENSION_TIF;
				} else {
					sAccumulation = oGPMQuery.productLevel;
					sExtension = EXTENSION_TIF;
				}
			}

			List<String> aoMonthsList = getMonthsBetweenDatesIncluding(oStartFromDate, oEndToDate);


			for (String sMonth : aoMonthsList) {
				if (Utils.isNullOrEmpty(sMonth)) {
					continue;
				}

				String sUrl = sBaseUrl + sMonth;
				String sHtmlPageSource = performRequest(sUrl);

				if (Utils.isNullOrEmpty(sHtmlPageSource)) {
					continue;
				}

				List<QueryRetrieveResponseEntry> list = parseRetrieveResponse(sHtmlPageSource);

				List<QueryRetrieveResponseEntry> filteredList = list.stream()
						.filter(t -> sDuration.equalsIgnoreCase(t.getDuration()))
						.filter(t -> sExtension.equalsIgnoreCase(t.getExtension()))
						.filter(t -> sAccumulation == null || sAccumulation.equalsIgnoreCase(t.getAccumulation()))
						.filter(t -> DURATION_MO.equalsIgnoreCase(t.getDuration()) || !(oStartFromDate.after(t.getDate())))
						.filter(t -> DURATION_MO.equalsIgnoreCase(t.getDuration()) || !(oEndToDate.before(t.getDate())))
						.collect(Collectors.toList());


				for (QueryRetrieveResponseEntry qre : filteredList) {
					QueryResultViewModel qrvm = new QueryResultViewModel();

					// for the list of results, force the extension to tif
					String sTitle;
					if (qre.getExtension().equalsIgnoreCase(EXTENSION_TIF)) {
						sTitle = qre.getName();
					} else {
						sTitle = qre.getName().replace(sExtension, EXTENSION_TIF);
					}

					qrvm.setId(qre.getName());
					qrvm.setTitle(sTitle);
					qrvm.setLink(sUrl + qre.getName());
					qrvm.setProvider("GPM");
					qrvm.setSummary("No summary, yet!");

					Map<String, String> properties = qrvm.getProperties();
					properties.put("platformname", oGPMQuery.platformName);
					properties.put("satellite", "MS");
					properties.put("instrument", "MRG");
					properties.put("algorithm", "3IMERG");
					properties.put("title", sTitle);
					properties.put("date", qre.getLastModified());
					properties.put("size", qre.getSize());
					properties.put("link", sUrl + qre.getName());
					properties.put("duration", qre.getDuration());
					properties.put("accumulation", qre.getAccumulation());
					properties.put("type", "tif");

					if (iOffset > 0) {
						iOffset--;
						continue;
					}

					aoResults.add(qrvm);

					if (aoResults.size() >= iLimit) {
						return aoResults;
					}
				}
				

			}

			return aoResults;
		} catch (Exception oEx) {
			Utils.debugLog("QueryExecutorGPM.executeAndRetrieve: error " + oEx.toString());
		}

		return null;
	}

	private String performRequest(String sUrl) {
		String sResult = "";

		int iMaxRetry = 5;
		int iAttemp = 0;

		while (Utils.isNullOrEmpty(sResult) && iAttemp < iMaxRetry) {

			if (iAttemp > 0) {
				Utils.debugLog("QueryExecutorGPM.performRequest.httpGetResults: attemp #" + iAttemp);
			}

			try {
				sResult = HttpUtils.httpGetResults(sUrl, s_oDataProviderConfig.user, s_oDataProviderConfig.password);
			} catch (Exception oEx) {
				Utils.debugLog("QueryExecutorGPM.performRequest: exception in http get call: " + oEx.toString());
			}

			iAttemp ++;
		}

		return sResult;
	}

	public static List<QueryCountResponseEntry> parseCountResponse(String source, String relativePath) {
		List<QueryCountResponseEntry> list = new ArrayList<>();

		if (Utils.isNullOrEmpty(source)) {
			return list;
		}

		String[] lines = source.split("\n");

		for (String line : lines) {
			QueryCountResponseEntry qcre = parseLine(line, relativePath);

			if (qcre == null) {
				continue;
			}

			list.add(qcre);
		}

		return list;
	}

	private static QueryCountResponseEntry parseLine(String line, String relativePath) {
		QueryCountResponseEntry qcre = null;

		if (Utils.isNullOrEmpty(line)) {
			return qcre;
		}

		if (!line.contains(relativePath)) {
			return qcre;
		}

		String name = line.replace(relativePath, "");


		int indexOf3B = name.indexOf("3B-");

		if (indexOf3B == -1) {
			return qcre;
		}

		int indexOfFirstDash = name.indexOf("-", indexOf3B + 3);

		if (indexOfFirstDash == -1) {
			return qcre;
		}

		String sDuration = name.substring(indexOf3B + 3, indexOfFirstDash);


		int indexOfImerg = name.indexOf("IMERG");

		if (indexOfImerg == -1) {
			return qcre;
		}

		int indexOfFirstDot = name.indexOf(".", indexOfImerg);

		if (indexOfFirstDot == -1) {
			return qcre;
		}

		String sDate = name.substring(indexOfFirstDot + 1, indexOfFirstDot + 9);

		if (Utils.isNullOrEmpty(sDate)) {
			return qcre;
		}

		Date oDate = Utils.getYyyyMMddDate(sDate);

		int indexOfExtensionDot = name.lastIndexOf(".");

		int indexOfAccumulationDot = name.substring(0, indexOfExtensionDot).lastIndexOf(".");

		String sAccumulation = name.substring(indexOfAccumulationDot + 1, indexOfExtensionDot);

		String sExtension = name.substring(indexOfExtensionDot);

		qcre = new QueryCountResponseEntry();
		qcre.setName(name);
		qcre.setDuration(sDuration);
		qcre.setAccumulation(sAccumulation);
		qcre.setExtension(sExtension);
		qcre.setDate(oDate);

		return qcre;
	}

	private static List<QueryRetrieveResponseEntry> parseRetrieveResponse(String source) {
		List<QueryRetrieveResponseEntry> list = new ArrayList<>();

		if (Utils.isNullOrEmpty(source)) {
			return list;
		}

		Document doc = Jsoup.parse(source);

		for (Element table : doc.select("table")) {
			for (Element row : table.select("tr")) {
				Elements tds = row.select("td");

				if (tds.isEmpty()) {
					continue;
				}

				if (tds.get(1).text().equalsIgnoreCase("Parent Directory")) {
					continue;
				}

				Element td1Element = tds.get(1);
				Element aElement = td1Element.selectFirst("a");
				String sName = aElement.attr("href");

				String lastModified = tds.get(2).text().trim();
				String sSize = tds.get(3).text();

				QueryRetrieveResponseEntry qreObject = new QueryRetrieveResponseEntry();

				qreObject.setName(sName);

				int indexOf3B = sName.indexOf("3B-");
				int indexOfFirstDash = sName.indexOf("-", indexOf3B + 3);
				String sDuration = sName.substring(indexOf3B + 3, indexOfFirstDash);
				qreObject.setDuration(sDuration);

				int indexOfExtensionDot = sName.lastIndexOf(".");

				int indexOfAccumulationDot = sName.substring(0, indexOfExtensionDot).lastIndexOf(".");

				String sAccumulation = sName.substring(indexOfAccumulationDot + 1, indexOfExtensionDot);
				qreObject.setAccumulation(sAccumulation);

				String sExtension = sName.substring(indexOfExtensionDot);
				qreObject.setExtension(sExtension);

				qreObject.setLastModified(lastModified);


				int indexOfImerg = sName.indexOf("IMERG");

				if (indexOfImerg > -1) {
					int indexOfFirstDot = sName.indexOf(".", indexOfImerg);

					if (indexOfFirstDot > -1) {
						String sDate = sName.substring(indexOfFirstDot + 1, indexOfFirstDot + 9);

						if (!Utils.isNullOrEmpty(sDate)) {
							qreObject.setDate(Utils.getYyyyMMddDate(sDate));
						}
					}
				}

				qreObject.setSize(sSize);

				list.add(qreObject);
			}
		}

		return list;
	}

}

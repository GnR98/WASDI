package it.fadeout.business;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.nfs.orbits.CoverageTool.CoverageRequest;
import org.nfs.orbits.CoverageTool.InterestArea;
import org.nfs.orbits.CoverageTool.Polygon;
import org.nfs.orbits.CoverageTool.apoint;
import org.nfs.orbits.sat.CoverageSwathResult;
import org.nfs.orbits.sat.ISatellite;
import org.nfs.orbits.sat.LookingType;
import org.nfs.orbits.sat.SatFactory;
import org.nfs.orbits.sat.SatSensor;
import org.nfs.orbits.sat.Satellite;
import org.nfs.orbits.sat.SensorMode;
import org.nfs.orbits.sat.SwathArea;
import org.nfs.orbits.sat.ViewAngle;
import org.nfs.orbits.sat.swathSize;

import it.fadeout.viewmodels.OpportunitiesSearchViewModel;
import it.fadeout.viewmodels.SatelliteFilterViewModel;
import it.fadeout.viewmodels.SensorModeViewModel;
import it.fadeout.viewmodels.SensorViewModel;
import satLib.astro.time.Time;

public class InstanceFinder {

	/**
	 * List of Orbit's CosmoSkyMed satellites references
	 */
	public static final String[] s_sOrbitSats = new String[] {
			"/org/nfs/orbits/sat/resource/cosmosky1.xml",
			"/org/nfs/orbits/sat/resource/cosmosky2.xml",
			"/org/nfs/orbits/sat/resource/cosmosky3.xml",
			"/org/nfs/orbits/sat/resource/cosmosky4.xml",
			"/org/nfs/orbits/sat/resource/sentinel_1a.xml",
			"/org/nfs/orbits/sat/resource/sentinel_1b.xml",
			"/org/nfs/orbits/sat/resource/landsat8.xml",
		    "/org/nfs/orbits/sat/resource/sentinel_2a.xml",
		    "/org/nfs/orbits/sat/resource/sentinel_2b.xml",
		    "/org/nfs/orbits/sat/resource/probav.xml",
		    "/org/nfs/orbits/sat/resource/geoeye.xml",
		    "/org/nfs/orbits/sat/resource/worldview2.xml"
	};

	public static final HashMap<String, String> s_sOrbitSatsMap = new HashMap<String, String>(){
		{
			put("COSMOSKY1", "/org/nfs/orbits/sat/resource/cosmosky1.xml");
			put("COSMOSKY2", "/org/nfs/orbits/sat/resource/cosmosky2.xml");
			put("COSMOSKY3", "/org/nfs/orbits/sat/resource/cosmosky3.xml");
			put("COSMOSKY4", "/org/nfs/orbits/sat/resource/cosmosky4.xml");
			put("SENTINEL1A", "/org/nfs/orbits/sat/resource/sentinel_1a.xml");
			put("SENTINEL1B", "/org/nfs/orbits/sat/resource/sentinel_1b.xml");
			put("LANDSAT8", "/org/nfs/orbits/sat/resource/landsat8.xml");
			put("SENTINEL2A", "/org/nfs/orbits/sat/resource/sentinel_2a.xml");
			put("SENTINEL2B", "/org/nfs/orbits/sat/resource/sentinel_2b.xml");			
			put("PROBAV", "/org/nfs/orbits/sat/resource/probav.xml");
			put("GEOEYE", "/org/nfs/orbits/sat/resource/geoeye.xml");
			put("WORLDVIEW2", "/org/nfs/orbits/sat/resource/worldview2.xml");
			
			put("COSMO-SKYMED 1", "/org/nfs/orbits/sat/resource/cosmosky1.xml");
			put("COSMO-SKYMED 2", "/org/nfs/orbits/sat/resource/cosmosky2.xml");
			put("COSMO-SKYMED 3", "/org/nfs/orbits/sat/resource/cosmosky3.xml");
			put("COSMO-SKYMED 4", "/org/nfs/orbits/sat/resource/cosmosky4.xml");
			put("SENTINEL-1A", "/org/nfs/orbits/sat/resource/sentinel_1a.xml");
			put("SENTINEL-1B", "/org/nfs/orbits/sat/resource/sentinel_1b.xml");
			put("LANDSAT 8", "/org/nfs/orbits/sat/resource/landsat8.xml");
			put("SENTINEL-2A", "/org/nfs/orbits/sat/resource/sentinel_2a.xml");
			put("SENTINEL-2B", "/org/nfs/orbits/sat/resource/sentinel_2b.xml");			
			put("PROBA-V", "/org/nfs/orbits/sat/resource/probav.xml");
			put("GEOEYE 1", "/org/nfs/orbits/sat/resource/geoeye.xml");
			put("WORLDVIEW-2 (WV-2)", "/org/nfs/orbits/sat/resource/worldview2.xml");
		}
	};

	static ArrayList<ISatellite> m_aoSatellites = null;		
	/**
	 * Constant to convert from wgs84 to radiant.
	 */
	static final double s_dConversionFactor = Math.PI / 180.0;

	/**
	 * Finds eo data instances by mean of Orbit library.
	 * 
	 * @param sArea
	 *            A string defining the area of interest.
	 * @param dtDate
	 *            Instances date.
	 * @param sMissionName
	 *            Name of satellite mission instances should made by. At the
	 *            moment this field is ignored, looking all cosmo skymed satellites.
	 * @return
	 */
	public static ArrayList<CoverageSwathResult> OLDfindSwats(String sArea, Date dtDate, String sMissionName) {

		// inizializzo i satelliti
		ArrayList<ISatellite> aoSatellites = new ArrayList<ISatellite>();



		// use all cosmo skymed satellites
		for (int i = 0; i < s_sOrbitSats.length; i++) {
			//InputStream oInputStream = TestSat.class.getResourceAsStream(s_sOrbitSats[i]);

			Satellite oSatellite;
			try {
				//oSatellite = new Satellite(oInputStream);
				oSatellite=SatFactory.buildSat(s_sOrbitSats[i]);
			} catch (Throwable oEx) {
				oEx.printStackTrace();
				System.out.println("InstanceFinder::findSwats: unable to instantiate satellite " + s_sOrbitSats[i] + " - " + oEx);
				return null;
			}

			// di ogni satellite devo specificare quali sensori attivare e quali
			// angoli considerare
			// (di Default nessun sensore è abilitato)

			// ottengo l'elenco dei sensori disponibili sul satellite
			ArrayList<SatSensor> oSatSensors = oSatellite.getSensors();

			for (SatSensor oSensor : oSatSensors) {

				System.out.println("SENSORE ORBIT: " + oSensor.getSName());
				// activate all sensors
				oSensor.setEnabled(true);
				// ottengo l'elenco di tutti i fasci (angoli) di
				// acquisizione disponibili per questo sensore
				ArrayList<SensorMode> oSensorModes = oSensor.getSensorModes();
				// per questo sensore attivo tutti i possibili fasci
				for (SensorMode oSensorMode : oSensorModes) {
					System.out.println("\tMODE: " + oSensorMode.getName());
					oSensorMode.setEnabled(true);
				}
			}

			// add the current satellite to the find list
			aoSatellites.add(oSatellite);
		}

		// preparo l'area di interesse
		InterestArea oAreaOfInterest = new InterestArea("required area");

		Polygon oPoligon = new Polygon();
		String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
		String[] asAreaPoints = sCleanedArea.split(",");
		int iPointsCount = asAreaPoints.length;
		apoint[] aoPoints = new apoint[iPointsCount];

		// process each polygon point
		if (asAreaPoints != null) {
			for (int iCount = 0; iCount < iPointsCount; iCount++) {
				String[] asPoint = asAreaPoints[iCount].split(" ");
				double dX;
				try {
					dX = Double.valueOf(asPoint[0]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto x dell'area ");
					dX = 0;
				}
				double dY;
				try {
					dY = Double.valueOf(asPoint[1]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto y dell'area ");
					dY = 0;
				}
				aoPoints[iCount] = new apoint(dX * s_dConversionFactor, dY * s_dConversionFactor, 0);

			}
		}

		//double k = Math.PI / 180.0d;
		// setto i punti dell'area di interesse
		oPoligon.setVertex(aoPoints);
		oAreaOfInterest.setArea(oPoligon);

		String sStartDate = "" + (dtDate.getYear() + 1900);
		sStartDate += "" + ((dtDate.getMonth() + 1) < 10 ? "0" + (dtDate.getMonth() + 1) : (dtDate.getMonth() + 1));
		sStartDate += "" + (dtDate.getDate() < 10 ? "0" + dtDate.getDate() : dtDate.getDate());

		String sEndDate = "" + (dtDate.getYear() + 1900);
		sEndDate += "" + ((dtDate.getMonth() + 1) < 10 ? "0" + (dtDate.getMonth() + 1) : (dtDate.getMonth() + 1));
		sEndDate += "" + ((dtDate.getDate() + 1) < 10 ? "0" + (dtDate.getDate() + 1) : (dtDate.getDate() + 1));

		// scelgo il periodo di osservazione
		// DAL 31/07/2013 15:00:00
		Time datetime_start = new Time(sStartDate + "000000");

		// AL 01/08/2013 17:00:00
		Time datetime_end = new Time(sEndDate + "000000");

		// preparo la richiesta di copertura
		CoverageRequest coverageRequest = new CoverageRequest();

		// aggiungo l'area di interesse (posso aggiungerne anche più di una)
		coverageRequest.addInterestArea(oAreaOfInterest);

		// imposto la lista di satelliti da utilizzare per la ricerca
		coverageRequest.setISatellite(aoSatellites);

		// imposto le date di inizio e fine osservazione
		coverageRequest.setFirstDate(datetime_start);
		coverageRequest.setSecondDate(datetime_end);

		// Eseguo la ricerca
		// se a solveRequest passo false ottengo soltanto la potenziale
		// copertura, i
		// fasci non vengono considerati.
		// se passo true per ogni potenziale copertura viene calcolata anche la
		// copertura
		// dei fasci attivati precedentemente
		ArrayList<CoverageSwathResult> oResults = coverageRequest.solveRequest(true);

		// ris contiente l'elenco di tutte le potenziali coperture

		return oResults;
	}

	/**
	 * 
	 * @param sArea
	 * @param dtDate
	 * @param sSensorResolution
	 * @param sSensorType
	 * @return
	 */
	public static ArrayList<CoverageSwathResult> findSwats(String sArea, Date dtDate, String sSensorResolution, String sSensorType) {

		// inizializzo i satelliti
		if (m_aoSatellites == null) {			

			System.out.println("findSwats: CREO I SATELLITI");

			m_aoSatellites = new ArrayList<ISatellite>();

			// use all cosmo skymed satellites
			for (int i = 0; i < s_sOrbitSats.length; i++) {

				System.out.println("findSwats: cerco satellite: " + s_sOrbitSats[i]);
				//InputStream oInputStream = TestSat.class.getResourceAsStream(s_sOrbitSats[i]);

				Satellite oSatellite;
				try {
					//oSatellite = new Satellite(oInputStream);
					oSatellite=SatFactory.buildSat(s_sOrbitSats[i]);
					System.out.println("costruito");
				} catch (Throwable oEx) {
					oEx.printStackTrace();
					System.out.println("InstanceFinder::findSwats: unable to instantiate satellite " + s_sOrbitSats[i] + " - " + oEx);
					return null;
				}

				// add the current satellite to the find list
				m_aoSatellites.add(oSatellite);
			}

		}

		if (m_aoSatellites != null) {
			System.out.println("findSwats: Satelliti Disponibili " + m_aoSatellites.size());
		}
		else {
			System.out.println("findSwats: m_aoSatellites NULL ");
		}

		ArrayList<ISatellite> aoSatellites = new ArrayList<ISatellite>();

		for (ISatellite oSatellite : m_aoSatellites) {

			if (oSatellite.getType().toString().toUpperCase().equals(sSensorType.toUpperCase()) == false) continue;

			// di ogni satellite devo specificare quali sensori attivare e quali
			// angoli considerare
			// (di Default nessun sensore è abilitato)

			// ottengo l'elenco dei sensori disponibili sul satellite
			ArrayList<SatSensor> oSatSensors = oSatellite.getSensors();

			for (SatSensor oSensor : oSatSensors) {

				boolean bEnabled = false;
				if (oSensor.getResolution().toString().toUpperCase().substring(0, 1).equals(sSensorResolution.toUpperCase().substring(0, 1) )==true) {
					bEnabled = true;
				}

				//System.out.println("SENSORE ORBIT: " + oSensor.getSName());

				// activate all sensors
				oSensor.setEnabled(bEnabled);
				// ottengo l'elenco di tutti i fasci (angoli) di
				// acquisizione disponibili per questo sensore
				ArrayList<SensorMode> oSensorModes = oSensor.getSensorModes();
				// per questo sensore attivo tutti i possibili fasci
				for (SensorMode oSensorMode : oSensorModes) {
					//System.out.println("\tMODE: " + oSensorMode.getName());
					oSensorMode.setEnabled(bEnabled);
				}
			}

			aoSatellites.add(oSatellite);	
		}

		// preparo l'area di interesse
		InterestArea oAreaOfInterest = new InterestArea("required area");

		Polygon oPoligon = new Polygon();
		String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
		String[] asAreaPoints = sCleanedArea.split(",");
		int iPointsCount = asAreaPoints.length;
		apoint[] aoPoints = new apoint[iPointsCount];

		// process each polygon point
		if (asAreaPoints != null) {
			for (int iCount = 0; iCount < iPointsCount; iCount++) {
				String[] asPoint = asAreaPoints[iCount].split(" ");
				double dX;
				try {
					dX = Double.valueOf(asPoint[0]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto x dell'area ");
					dX = 0;
				}
				double dY;
				try {
					dY = Double.valueOf(asPoint[1]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto y dell'area ");
					dY = 0;
				}
				aoPoints[iCount] = new apoint(dX * s_dConversionFactor, dY * s_dConversionFactor, 0);

			}
		}

		//double k = Math.PI / 180.0d;
		// setto i punti dell'area di interesse
		oPoligon.setVertex(aoPoints);
		oAreaOfInterest.setArea(oPoligon);

		SimpleDateFormat oFormat = new SimpleDateFormat("yyyyMMdd");
		String sDate = oFormat.format(dtDate);
		sDate += "062100";

		// scelgo il periodo di osservazione
		Time oDateTimeStart = new Time(sDate);
		Time oDateTimeEnd = new Time(sDate);

		// Set starting and ending time according the time of the request
		Calendar oCalendar = GregorianCalendar.getInstance();

		oCalendar.setTime(dtDate);
		if (oCalendar.get(Calendar.HOUR_OF_DAY)<=12 && oCalendar.get(Calendar.MINUTE)<30) {
			// Ok for tomorrow
			oDateTimeStart.add(Time.HOUR,24);
			oDateTimeEnd.add(Time.HOUR,48);
		}
		else {
			// Impossible, go to the day after
			oDateTimeStart.add(Time.HOUR,48);
			oDateTimeEnd.add(Time.HOUR,72);			
		}

		// preparo la richiesta di copertura
		CoverageRequest coverageRequest = new CoverageRequest();

		// aggiungo l'area di interesse (posso aggiungerne anche più di una)
		coverageRequest.addInterestArea(oAreaOfInterest);

		// imposto la lista di satelliti da utilizzare per la ricerca
		coverageRequest.setISatellite(aoSatellites);

		// imposto le date di inizio e fine osservazione
		coverageRequest.setFirstDate(oDateTimeStart);
		coverageRequest.setSecondDate(oDateTimeEnd);


		System.out.println("findSwats CHIAMO SOLVE REQUEST");
		// Eseguo la ricerca
		// se a solveRequest passo false ottengo soltanto la potenziale
		// copertura, i fasci non vengono considerati.
		// se passo true per ogni potenziale copertura viene calcolata anche la copertura dei fasci attivati precedentemente
		ArrayList<CoverageSwathResult> oResults = coverageRequest.solveRequest(true);

		System.out.println("findSwats TORNO");
		// ris contiente l'elenco di tutte le potenziali coperture

		return oResults;
	}

	
//	/**
//	 * 
//	 * @param sArea
//	 * @param dtDate
//	 * @param sSensorResolution
//	 * @param sSensorType
//	 * @return
//	 * @throws ParseException 
//	 */
//	public static ArrayList<CoverageSwathResult> findSwatsByFilters(String sArea, 
//			String sAquisitionStartTime, String sAquisitionEndTime,
//			ArrayList<String> asSatelliteNames,
//			String sSensorResolution, String sSensorType,String sLookingType,String sViewAngle,String sSwathSize) throws ParseException {
//
//
//		System.out.println("findSwats: CREO I SATELLITI");
//
//		m_aoSatellites = new ArrayList<ISatellite>();
//
//		// use all cosmo skymed satellites
//		for (int i = 0; i < asSatelliteNames.size(); i++) {
//
//			System.out.println("InstanceFinder::findSwatsByFilters: cerco satellite: " + asSatelliteNames.get(i));
//			//InputStream oInputStream = TestSat.class.getResourceAsStream(s_sOrbitSatsMap.get(asSatelliteNames.get(i)));
//
//			Satellite oSatellite;
//			try {
//				//oSatellite = new Satellite(oInputStream);
//				oSatellite=SatFactory.buildSat(s_sOrbitSatsMap.get(asSatelliteNames.get(i)));
//				System.out.println("costruito");
//			} catch (Throwable oEx) {
//				oEx.printStackTrace();
//				System.out.println("InstanceFinder::findSwatsByFilters: unable to instantiate satellite " + s_sOrbitSats[i] + " - " + oEx);
//				return null;
//			}
//
//			// add the current satellite to the find list
//			m_aoSatellites.add(oSatellite);
//		}
//
//		if (m_aoSatellites != null) {
//			System.out.println("InstanceFinder::findSwatsByFilters: Satelliti Disponibili " + m_aoSatellites.size());
//		}
//		else {
//			System.out.println("InstanceFinder::findSwatsByFilters: m_aoSatellites NULL ");
//		}
//
//		ArrayList<ISatellite> aoSatellites = new ArrayList<ISatellite>();
//		LookingType oLookingType = convertLookingTypeString(sLookingType);
//		ViewAngle oViewAngle = convertViewAngleString(sViewAngle);
//		swathSize oSwathSize = convertSwathSizeString(sSwathSize);
//		for (ISatellite oSatellite : m_aoSatellites) {
//
//			if (oSatellite.getType().toString().toUpperCase().equals(sSensorType.toUpperCase()) == false) continue;
//
//			// di ogni satellite devo specificare quali sensori attivare e quali
//			// angoli considerare
//			// (di Default nessun sensore è abilitato)
//
//			// ottengo l'elenco dei sensori disponibili sul satellite
//			ArrayList<SatSensor> oSatSensors = oSatellite.getSensors();
//
//			for (SatSensor oSensor : oSatSensors) {
//
//				boolean bEnabled = false;
//				if (oSensor.getResolution().toString().toUpperCase().substring(0, 1).equals(sSensorResolution.toUpperCase().substring(0, 1) )==true) {
//					bEnabled = true;
//				}
//
//				//System.out.println("SENSORE ORBIT: " + oSensor.getSName());
//
//				// activate all sensors
//				oSensor.setEnabled(bEnabled);
//				// ottengo l'elenco di tutti i fasci (angoli) di
//				// acquisizione disponibili per questo sensore
//				ArrayList<SensorMode> oSensorModes = oSensor.getSensorModes();
//				// per questo sensore attivo tutti i possibili fasci
//				for (SensorMode oSensorMode : oSensorModes) {
//					//System.out.println("\tMODE: " + oSensorMode.getName());
//					oSensorMode.setEnabled(bEnabled);
//				}
//				
//				if(oLookingType != null)
//				{
//					oSensor.setLooking(oLookingType);
//				}
//				if(oViewAngle != null)
//				{
//					oSensor.setViewAngle(oViewAngle);
//				}
//				if(oSwathSize != null)
//				{
//					oSensor.setswathSize(oSwathSize);
//				}
//			}
//
//			aoSatellites.add(oSatellite);	
//		}
//
//		// preparo l'area di interesse
//		InterestArea oAreaOfInterest = new InterestArea("required area");
//
//		Polygon oPoligon = new Polygon();
//		String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
//		String[] asAreaPoints = sCleanedArea.split(",");
//		int iPointsCount = asAreaPoints.length;
//		apoint[] aoPoints = new apoint[iPointsCount];
//
//		// process each polygon point
//		if (asAreaPoints != null) {
//			for (int iCount = 0; iCount < iPointsCount; iCount++) {
//				String[] asPoint = asAreaPoints[iCount].split(" ");
//				double dX;
//				try {
//					dX = Double.valueOf(asPoint[0]);
//				} catch (Exception oEx) {
//					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto x dell'area ");
//					dX = 0;
//				}
//				double dY;
//				try {
//					dY = Double.valueOf(asPoint[1]);
//				} catch (Exception oEx) {
//					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto y dell'area ");
//					dY = 0;
//				}
//				aoPoints[iCount] = new apoint(dX * s_dConversionFactor, dY * s_dConversionFactor, 0);
//
//			}
//		}
//
//		//double k = Math.PI / 180.0d;
//		// setto i punti dell'area di interesse
//		oPoligon.setVertex(aoPoints);
//		oAreaOfInterest.setArea(oPoligon);
//
//		SimpleDateFormat oFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		Date dtAquisitionStartTime = oFormat.parse(sAquisitionStartTime);
//		Date dtAquisitionEndTime = oFormat.parse(sAquisitionEndTime);
//
//		SimpleDateFormat oFormat2 = new SimpleDateFormat("yyyyMMddHHmmss");
//		sAquisitionStartTime = oFormat2.format(dtAquisitionStartTime);
//		sAquisitionEndTime = oFormat2.format(dtAquisitionEndTime);
//
//		// scelgo il periodo di osservazione
//		Time oDateTimeStart = new Time(sAquisitionStartTime);
//		Time oDateTimeEnd = new Time(sAquisitionEndTime);
//
//		// preparo la richiesta di copertura
//		CoverageRequest coverageRequest = new CoverageRequest();
//
//		// aggiungo l'area di interesse (posso aggiungerne anche più di una)
//		coverageRequest.addInterestArea(oAreaOfInterest);
//
//		// imposto la lista di satelliti da utilizzare per la ricerca
//		coverageRequest.setISatellite(aoSatellites);
//
//		// imposto le date di inizio e fine osservazione
//		coverageRequest.setFirstDate(oDateTimeStart);
//		coverageRequest.setSecondDate(oDateTimeEnd);
//		//String sLookingType,String sViewAngle,String sSwathSize
//		System.out.println("findSwats CHIAMO SOLVE REQUEST");
//		// Eseguo la ricerca
//		// se a solveRequest passo false ottengo soltanto la potenziale
//		// copertura, i fasci non vengono considerati.
//		// se passo true per ogni potenziale copertura viene calcolata anche la copertura dei fasci attivati precedentemente
//		ArrayList<CoverageSwathResult> oResults = coverageRequest.solveRequest(true);
//
//		System.out.println("findSwats TORNO");
//		// ris contiente l'elenco di tutte le potenziali coperture
//
//		return oResults;
//	}
	
	
	public static  ArrayList<CoverageSwathResult> findSwatsByFilters(OpportunitiesSearchViewModel oOpportunitiesSearch)
	{
		System.out.println("findSwats: CREO I SATELLITI");

		m_aoSatellites = new ArrayList<ISatellite>();
		ArrayList<SatelliteFilterViewModel> aoSatelliteFilters;
		aoSatelliteFilters = oOpportunitiesSearch.getSatelliteFilters();
		
		// use all cosmo skymed satellites
		for (int iIndexSatelliteFitler = 0; iIndexSatelliteFitler < aoSatelliteFilters.size() ; iIndexSatelliteFitler++) {
			String sSatelliteName = aoSatelliteFilters.get(iIndexSatelliteFitler).getSatelliteName();
			System.out.println("InstanceFinder::findSwatsByFilters: cerco satellite: " + sSatelliteName);
			//InputStream oInputStream = TestSat.class.getResourceAsStream(s_sOrbitSatsMap.get(asSatelliteNames.get(i)));

			Satellite oSatellite;
			try {
				//oSatellite = new Satellite(oInputStream);
				oSatellite=SatFactory.buildSat(s_sOrbitSatsMap.get(sSatelliteName));
				System.out.println("costruito");
			} catch (Throwable oEx) {
				oEx.printStackTrace();
				System.out.println("InstanceFinder::findSwatsByFilters: unable to instantiate satellite " + s_sOrbitSats[iIndexSatelliteFitler] + " - " + oEx);
				return null;
			}

			// add the current satellite to the find list
			m_aoSatellites.add(oSatellite);
		}

		if (m_aoSatellites != null) {
			System.out.println("InstanceFinder::findSwatsByFilters: Satelliti Disponibili " + m_aoSatellites.size());
		}
		else {
			System.out.println("InstanceFinder::findSwatsByFilters: m_aoSatellites NULL ");
		}
		
		for (int iIndexSatelliteFitler = 0; iIndexSatelliteFitler < aoSatelliteFilters.size() ; iIndexSatelliteFitler++) 
		{
			aoSatelliteFilters.get(iIndexSatelliteFitler);
		}
//		ArrayList<ISatellite> aoSatellites = new ArrayList<ISatellite>();
		
		//vedo quali sensori sono stati selezionati 
		for (ISatellite oSatellite : m_aoSatellites) 
		{
			for(int iIndexSatelliteFilter = 0; iIndexSatelliteFilter < aoSatelliteFilters.size() ; iIndexSatelliteFilter++)
			{
				String sSatelliteName =  aoSatelliteFilters.get(iIndexSatelliteFilter).getSatelliteName();
				if(oSatellite.getName().equals(sSatelliteName))
				{
					ArrayList<SensorViewModel> aoSatelliteSensorsEnabled = aoSatelliteFilters.get(iIndexSatelliteFilter).getSatelliteSensors();
					ArrayList<SatSensor> aoSatSensors = oSatellite.getSensors();
					setEnableSensorsAndSensorModes(aoSatSensors,aoSatelliteSensorsEnabled);
					
				}
				
			}
		}
		
		// preparo l'area di interesse
		InterestArea oAreaOfInterest = new InterestArea("required area");
		String sArea = oOpportunitiesSearch.getPolygon();
		Polygon oPoligon = prepareAreaOfInterest(sArea);
		oAreaOfInterest.setArea(oPoligon);
		
		//preparo le date
		String sAquisitionStartTime = oOpportunitiesSearch.getAcquisitionStartTime();
		String sAquisitionEndTime = oOpportunitiesSearch.getAcquisitionEndTime();
		sAquisitionStartTime = dateFormat(sAquisitionStartTime);
		sAquisitionEndTime = dateFormat(sAquisitionEndTime);
		
		// scelgo il periodo di osservazione
		Time oDateTimeStart = new Time(sAquisitionStartTime);
		Time oDateTimeEnd = new Time(sAquisitionEndTime);
		
		// preparo la richiesta di copertura
		CoverageRequest coverageRequest = new CoverageRequest();
		coverageRequest = getCoverageRequest(oAreaOfInterest,m_aoSatellites,oDateTimeStart,oDateTimeEnd);
		//String sLookingType,String sViewAngle,String sSwathSize
		System.out.println("findSwats CHIAMO SOLVE REQUEST");
		// Eseguo la ricerca
		// se a solveRequest passo false ottengo soltanto la potenziale
		// copertura, i fasci non vengono considerati.
		// se passo true per ogni potenziale copertura viene calcolata anche la copertura dei fasci attivati precedentemente
		ArrayList<CoverageSwathResult> oResults = coverageRequest.solveRequest(true);

		System.out.println("findSwats TORNO");
		// ris contiente l'elenco di tutte le potenziali coperture
		
		return oResults;
	}
	
	private static CoverageRequest getCoverageRequest(InterestArea oAreaOfInterest,ArrayList<ISatellite> aoSatellites,Time oDateTimeStart,Time oDateTimeEnd)
	{
		CoverageRequest oCoverageRequest = new CoverageRequest();
		
		// aggiungo l'area di interesse (posso aggiungerne anche più di una)
		oCoverageRequest.addInterestArea(oAreaOfInterest);

		// imposto la lista di satelliti da utilizzare per la ricerca
		oCoverageRequest.setISatellite(aoSatellites);

		// imposto le date di inizio e fine osservazione
		oCoverageRequest.setFirstDate(oDateTimeStart);
		oCoverageRequest.setSecondDate(oDateTimeEnd);
		
		return oCoverageRequest;
	}
	
	private static String dateFormat(String sTime)
	{
		SimpleDateFormat oFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dtTime;
		try {
			dtTime = oFormat.parse(sTime);
			SimpleDateFormat oFormat2 = new SimpleDateFormat("yyyyMMddHHmmss");
			sTime = oFormat2.format(dtTime);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sTime;
	}
	
	private static Polygon prepareAreaOfInterest(String sArea){
//		String sArea = oOpportunitiesSearch.getPolygon();
		
		Polygon oPoligon = new Polygon();
		String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
		String[] asAreaPoints = sCleanedArea.split(",");
		int iPointsCount = asAreaPoints.length;
		apoint[] aoPoints = new apoint[iPointsCount];

		// process each polygon point
		if (asAreaPoints != null) {
			for (int iCount = 0; iCount < iPointsCount; iCount++) {
				String[] asPoint = asAreaPoints[iCount].split(" ");
				double dX;
				try {
					dX = Double.valueOf(asPoint[0]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto x dell'area ");
					dX = 0;
				}
				double dY;
				try {
					dY = Double.valueOf(asPoint[1]);
				} catch (Exception oEx) {
					System.out.println("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto y dell'area ");
					dY = 0;
				}
				aoPoints[iCount] = new apoint(dX * s_dConversionFactor, dY * s_dConversionFactor, 0);

			}
		}
		oPoligon.setVertex(aoPoints);
		
		return oPoligon;
	}
	
	private static void setEnableSensorsAndSensorModes(ArrayList<SatSensor> aoSatSensors, ArrayList<SensorViewModel> aoSatelliteSensorsEnabled)
	{
		
//		ArrayList<SatSensor> aoSatSensors = oSatellite.getSensors();
		for (SatSensor oSensor : aoSatSensors) 
		{
			for(SensorViewModel oSensorEnabled : aoSatelliteSensorsEnabled)
			{
				if(oSensor.getDescription().equals(oSensorEnabled.getDescription()))
				{
					oSensor.setEnabled(true);
					
					ArrayList<SensorMode> oSensorModes = oSensor.getSensorModes();
					ArrayList<SensorModeViewModel> oSensorModesEnabled = oSensorEnabled.getSensorModes();
					setEnableSensorModes(oSensorModes,oSensorModesEnabled);
//					for (SensorMode oSensorMode : oSensorModes) {
//						for(SensorModeViewModel oSensorModeEnabled:oSensorModesEnabled)
//						{
//							if(oSensorMode.getName().equals(oSensorModeEnabled.getName()))
//							{
//								oSensorMode.setEnabled(true);
//							}
//							else
//							{
//								oSensorMode.setEnabled(false);
//							}
//
//						}
//					}
				}
//				else
//				{
//					oSensor.setEnabled(false);
//				}
			}
		}
		
		
	}
	
	private static void setEnableSensorModes(ArrayList<SensorMode> oSensorModes, ArrayList<SensorModeViewModel> oSensorModesEnabled)
	{
		for (SensorMode oSensorMode : oSensorModes) {
			for(SensorModeViewModel oSensorModeEnabled:oSensorModesEnabled)
			{
				if(oSensorMode.getName().equals(oSensorModeEnabled.getName()))
				{
					oSensorMode.setEnabled(true);
				}
				/*else
				{
					oSensorMode.setEnabled(false);
				}*/

			}
		}
	}
	
	private static LookingType convertLookingTypeString(String sLookingType)
	{
		sLookingType = sLookingType.trim();
		sLookingType = sLookingType.toUpperCase();
		switch (sLookingType) {
		case "NONE":
			return LookingType.NONE ;
		case "LEFT":
			return LookingType.LEFT ;
		case "RIGHT":
			return LookingType.RIGHT ;
		default:
			break;
		}
		return null;
	}
	
	private static ViewAngle convertViewAngleString(String sViewAngle)
	{
		sViewAngle = sViewAngle.trim();
		if(sViewAngle.isEmpty())
		{
			return null;
		}
		String [] asViewAngleSplitted = sViewAngle.split(",");
		asViewAngleSplitted[0] = asViewAngleSplitted[0].replace("(nearAngle:", "");
		asViewAngleSplitted[1] = asViewAngleSplitted[1].replace("farAngle:", "");
		asViewAngleSplitted[1] = asViewAngleSplitted[1].replace(")", "");
		if(asViewAngleSplitted[0].isEmpty() || asViewAngleSplitted[1].isEmpty())
		{
			return null;
		}
		ViewAngle oViewAngleReturnValue = new ViewAngle(Double.parseDouble(asViewAngleSplitted[0]), Double.parseDouble(asViewAngleSplitted[1]));
		return oViewAngleReturnValue;
	}
	private static swathSize convertSwathSizeString(String sSwathSize)
	{
		sSwathSize = sSwathSize.trim();
		if(sSwathSize.isEmpty())
		{
			return null;
		}
		String [] asSwathSizeSplitted = sSwathSize.split(",");
		asSwathSizeSplitted[0] = asSwathSizeSplitted[0].replace("(length:", "");
		asSwathSizeSplitted[1] = asSwathSizeSplitted[1].replace("width:", "");
		asSwathSizeSplitted[1] = asSwathSizeSplitted[1].replace(")", "");
		if(asSwathSizeSplitted[0].isEmpty() || asSwathSizeSplitted[1].isEmpty())
		{
			return null;
		}
		swathSize oSwathSize = new swathSize(Double.parseDouble(asSwathSizeSplitted[0]), Double.parseDouble(asSwathSizeSplitted[1]));
		return oSwathSize;
	}
	public static void test() {
		// inizializzo i satelliti
		ArrayList<ISatellite> satelliti = new ArrayList<ISatellite>();

		for (int i = 0; i < s_sOrbitSats.length; i++) {
//			InputStream strm = TestSat.class
//					.getResourceAsStream(s_sOrbitSats[i]);
			Satellite sat;
			try {
				//sat = new Satellite(strm);
				sat=SatFactory.buildSat(s_sOrbitSats[i]);
			} catch (Throwable oEx) {
				// TODO Auto-generated catch block
				oEx.printStackTrace();
				return;
			}
			// visualizzo le info del satellite
			sat.printInfo();
			// di ogni satellite devo specificare quali sensori attivare e quali
			// angoli considerare
			// (di Default nessun sensore è abilitato)

			// ottengo l'elenco dei sensori disponibili sul satellite
			ArrayList<SatSensor> sns = sat.getSensors();
			for (SatSensor sensor : sns) {
				// attivo solo acquisizioni StripMap - right
				if (sensor.getSName().startsWith("StripMap (HIMAGE) - Right")) {
					sensor.setEnabled(true);

					// ottengo l'elenco di tutti i fasci (angoli) di
					// acquisizione disponibili per questo sensore
					ArrayList<SensorMode> snsmode = sensor.getSensorModes();
					// per questo sensore attivo tutti i possibili fasci
					for (SensorMode itm : snsmode)
						itm.setEnabled(true);
				}
			}
			satelliti.add(sat);
		}

		// preparo l'area di interesse
		InterestArea iarea = new InterestArea("area test");
		Polygon pol = new Polygon();
		double k = Math.PI / 180.0d;
		// setto i punti dell'area di interesse
		pol.setVertex(new apoint[] {
				// i punti devono essere convertiti da wgs84 in radianti
				new apoint(8.63 * k, 44.51 * k, 0),
				new apoint(9.10 * k, 44.46 * k, 0),
				new apoint(9.05 * k, 44.29 * k, 0),
				new apoint(8.66 * k, 44.32 * k, 0), });
		iarea.setArea(pol);

		// scelgo il periodo di osservazione
		// DAL 31/07/2013 15:00:00
		Time datetime_start = new Time("20130731150000");

		// AL 01/08/2013 17:00:00
		Time datetime_end = new Time("20130801170000");

		// preparo la richiesta di copertura
		CoverageRequest coverageRequest = new CoverageRequest();

		// aggiungo l'area di interesse (posso aggiungerne anche più di una)
		coverageRequest.addInterestArea(iarea);

		// imposto la lista di satelliti da utilizzare per la ricerca
		coverageRequest.setISatellite(satelliti);

		// imposto le date di inizio e fine osservazione
		coverageRequest.setFirstDate(datetime_start);
		coverageRequest.setSecondDate(datetime_end);

		// Eseguo la ricerca
		// se a solveRequest passo false ottengo soltanto la potenziale
		// copertura, i
		// fasci non vengono considerati.
		// se passo true per ogni potenziale copertura viene calcolata anche la
		// copertura
		// dei fasci attivati precedentemente
		ArrayList<CoverageSwathResult> ris = coverageRequest.solveRequest(true);
		// ris contiente l'elenco di tutte le potenziali coperture

		// visualizzo i risultati
		for (CoverageSwathResult cov : ris) {
			// visualizzo tutti i dettagli..
			System.out.println("ID swath: " + cov.getSwathName());
			System.out.println("satellite: " + cov.getSat().getName());
			System.out.println("Sensore utilizzato: "
					+ cov.getSensor().getSName());
			// fascio del sensore utilizzato (angoli)
			System.out.println("Sensore Mode: " + cov.getMode());
			// se sono state specificate più aree di interesse è utilse sapere
			// questo swath che area copre
			System.out.println("area di interesse coperta: "
					+ cov.getCoveredArea().getName());
			System.out.println("% copertura " + cov.getCoverage() * 100);
			System.out.println("Inizio acquisizione: "
					+ cov.getTimeStart().getDateTimeStr());
			System.out.println("Fine acquisizione: "
					+ cov.getTimeEnd().getDateTimeStr());
			System.out.println("Durata " + cov.getDuration());
			System.out.println("larghezza copertura "
					+ cov.getswathSize().getWidth());
			System.out.println("lunghezza copertura "
					+ cov.getswathSize().getLength());
			// visualizzo le coordinate della copertura

			apoint[] vrtx = cov.getFootprint().getVertex();
			for (apoint pnt : vrtx)
				// converto i punti da radianti a lon lat (wgs84)
				System.out.println("lon: " + pnt.x / k + " lat: " + pnt.y / k);
			// se a solveRequest ho passato true avro' anche le aree di
			// copertura di
			// ogni singolo fascio
			ArrayList<SwathArea> chld = cov.getChilds();
			for (SwathArea itm : chld) {
				// posso visualizzare le stesse info di sopra poichè
				// SwathArea è la superclasse di CoverageSwathResult

				// Con printDetail visualizzo le stesse info
				itm.printDetail();
			}

		}
	}

}

/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.eodc;

import com.google.common.base.Preconditions;

import wasdi.shared.opensearch.DiasQueryTranslator;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class DiasQueryTranslatorEODC extends DiasQueryTranslator {

	static String s_sQueryPrefix ="<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" service=\"CSW\" version=\"2.0.2\" resultType=\"results\" startPosition=\"1\" maxRecords=\"10\" outputFormat=\"application/json\" outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/cat/csw/2.0.2 http://schemas.opengis.net/csw/2.0.2/CSW-discovery.xsd\"><csw:Query typeNames=\"csw:Record\"><csw:ElementSetName>full</csw:ElementSetName><csw:Constraint version=\"1.1.0\"><ogc:Filter><ogc:And>";
	static String s_sQuerySuffix = "</ogc:And></ogc:Filter></csw:Constraint></csw:Query></csw:GetRecords>";
	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	protected String translate(String sQueryFromClient) {
		String sQuery = prepareQuery(sQueryFromClient);
		String sTranslatedQuery = "";
		
		if(!Utils.isNullOrEmpty(sQuery)) {
			sTranslatedQuery += s_sQueryPrefix;
			sTranslatedQuery += parseFootPrint(sQuery);
			sTranslatedQuery += parseTimeFrame(sQuery);
			//todo translate
			sTranslatedQuery += s_sQuerySuffix;
		}
		return sTranslatedQuery;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#parseTimeFrame(java.lang.String)
	 */
	@Override
	protected String parseTimeFrame(String sQuery) {
		
		String[] asInterval = {null, null};
		
		//beginPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
		String sKeyword = "beginPosition";
		parseInterval(sQuery, sKeyword, asInterval);
		String sStart = asInterval[0];
		String sEnd = asInterval[1];
		
		//endPosition:[2020-01-30T00:00:00.000Z TO 2020-02-06T23:59:59.999Z]
		sKeyword = "endPosition";
		parseInterval(sQuery, sKeyword, asInterval);
		if(Utils.isNullOrEmpty(sStart) && !Utils.isNullOrEmpty(asInterval[0])) {
			sStart = asInterval[0];
		}
		if(Utils.isNullOrEmpty(sEnd) && !Utils.isNullOrEmpty(asInterval[1])) {
			sEnd = asInterval[1];
		}

		String sTranslatedTimeFrame = "";
		if(!Utils.isNullOrEmpty(sStart)) {
			sTranslatedTimeFrame += "<ogc:PropertyIsGreaterThanOrEqualTo><ogc:PropertyName>dc:date</ogc:PropertyName><ogc:Literal>";
			sTranslatedTimeFrame += sStart;
			sTranslatedTimeFrame += "</ogc:Literal></ogc:PropertyIsGreaterThanOrEqualTo>";
		}
		if(!Utils.isNullOrEmpty(sEnd)) {
			sTranslatedTimeFrame += "<ogc:PropertyIsLessThanOrEqualTo><ogc:PropertyName>dc:date</ogc:PropertyName><ogc:Literal>";
			sTranslatedTimeFrame += sEnd;
			sTranslatedTimeFrame += "</ogc:Literal></ogc:PropertyIsLessThanOrEqualTo>";
		}

		//todo add interval
		
		return sTranslatedTimeFrame;
	}

	/**
	 * @param sQuery
	 * @param sKeyword
	 * @param alStartEnd
	 */
	protected void parseInterval(String sQuery, String sKeyword, String[] asInterval) {
		Preconditions.checkNotNull(sQuery, "DiasQueryTranslatorEODC.parseInterval: query is null");
		Preconditions.checkNotNull(sKeyword, "DiasQueryTranslatorEODC.parseInterval: field keyword is null");
		Preconditions.checkNotNull(asInterval, "DiasQueryTranslatorEODC.parseInterval: String array is null");
		Preconditions.checkElementIndex(0, asInterval.length, "DiasQueryTranslatorEODC.parseInterval: 0 is not a valid element index");
		Preconditions.checkElementIndex(1, asInterval.length, "DiasQueryTranslatorEODC.parseInterval: 1 is not a valid element index");
		
		if( sQuery.contains(sKeyword)) {
			int iStart = Math.max(0, sQuery.indexOf(sKeyword));
			iStart = Math.max(iStart, sQuery.indexOf('[', iStart) + 1);
			int iEnd = sQuery.indexOf(']', iStart);
			if(iEnd < 0) {
				iEnd = sQuery.length()-1;
			};
			String[] asTimeQuery= sQuery.substring(iStart, iEnd).trim().split(" TO ");
			asInterval[0] = asTimeQuery[0];
			asInterval[1] = asTimeQuery[1];
		}
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#parseFootPrint(java.lang.String)
	 */
	@Override
	protected String parseFootPrint(String sQuery) {
		String sResult = "";
		try {
			if(sQuery.contains("footprint")) {
				String sIntro = "( footprint:\"intersects ( POLYGON ( ( ";
				int iStart = sQuery.indexOf(sIntro);
				if(iStart >= 0) {
					iStart += sIntro.length();
				}
				int iEnd = sQuery.indexOf(')', iStart);
				if(0>iEnd) {
					iEnd = sQuery.length();
				}
				Double dNorth = Double.NEGATIVE_INFINITY;
				Double dSouth = Double.POSITIVE_INFINITY;
				Double dEast = Double.NEGATIVE_INFINITY;
				Double dWest = Double.POSITIVE_INFINITY;
				try {
					String[] asCouples = sQuery.substring(iStart, iEnd).trim().split(",");

					for (String sPair: asCouples) {
						try {
							String[] asTwoCoord = sPair.split(" ");
							Double dParallel = Double.parseDouble(asTwoCoord[1]);
							dNorth = Double.max(dNorth, dParallel);
							dSouth = Double.min(dSouth, dParallel);

							Double dMeridian = Double.parseDouble(asTwoCoord[0]);
							dEast = Double.max(dEast, dMeridian);
							dWest = Double.min(dWest, dMeridian);
						} catch (Exception oE) {
							Utils.log("ERROR", "DiasQueryTranslatorEODC.parseFootprint: issue with current coordinate pair: " + sPair + ": " + oE);
						}
					}
					//todo check coordinates are within bounds
					if(
							-90 <= dNorth && 90 >= dNorth &&
							-90 <= dSouth && 90 >= dSouth &&
							-180 <= dEast && 180 >= dEast &&
							-180 <= dWest && 180 >= dWest
					) {
						sResult += "<ogc:BBOX><ogc:PropertyName>ows:BoundingBox</ogc:PropertyName><gml:Envelope><gml:lowerCorner>";
						sResult += dSouth + " " + dWest;
						sResult += "</gml:lowerCorner><gml:upperCorner>";
						sResult += dNorth + " " + dEast;
						sResult += "</gml:upperCorner></gml:Envelope></ogc:BBOX>";
					}
						
				} catch (Exception oE) {
					Utils.log("ERROR", "DiasQueryTranslatorEODC.parseFootprint: could not complete: " + oE);
				}
			}
		} catch (Exception oE) {
			Utils.log("ERROR", "DiasQueryTranslatorEODC.parseFootprint: could not identify footprint substring limits: " + oE);
		}
		return sResult;
	}

	public static void main(String[] args) {
		String sQuery = "( footprint:\"intersects(POLYGON((92.36417183697604 12.654592055231863,92.36417183697604 26.282214356266774,99.48157676962991 26.282214356266774,99.48157676962991 12.654592055231863,92.36417183697604 12.654592055231863)))\" ) AND ( beginPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] AND endPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD AND relativeorbitnumber:33)&offset=0&limit=10";
		DiasQueryTranslatorEODC oEODC = new DiasQueryTranslatorEODC();
		String sResult = oEODC.translate(sQuery);
		System.out.println(sResult);
	}
	
}

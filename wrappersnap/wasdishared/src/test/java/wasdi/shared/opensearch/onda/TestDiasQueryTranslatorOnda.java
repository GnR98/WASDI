/**
 * 
 */
package wasdi.shared.opensearch.onda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author c.nattero
 *
 */
class TestDiasQueryTranslatorOnda {

	/**
	 * Test method for {@link wasdi.shared.opensearch.onda.DiasQueryTranslatorONDA#parseProductName(java.lang.String)}.
	 */
	@Test
	void testParseFreeText() {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		
		assertEquals(null, oDQT.parseProductName(""));
	}

	@ParameterizedTest
	@ValueSource( strings = {
		//full name without extension
		"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
		"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*",
		"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
		"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*"
	})
	final void testParseFreeText_fullNameWithoutExtensions(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*", oDQT.parseProductName(sInputText + sSuffix));
	}

	@ParameterizedTest
	@ValueSource( strings = {
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",

			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*",

			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",

			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*",
	})
	final void testParseFreeText_fullNameWithExtensions(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423", oDQT.parseProductName(sInputText + sSuffix));
	}
	
	@ParameterizedTest
	@ValueSource( strings = {
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*"
	})
	final void testParseFreeText_noHeadWithoutExtensions(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423*", oDQT.parseProductName(sInputText + sSuffix));
	}
	
	@ParameterizedTest
	@ValueSource( strings = {
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.zip*",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423.*"
	})
	final void testParseFreeText_noHeadWithExtensions(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D_7423", oDQT.parseProductName(sInputText + sSuffix));
	}
	
	
	@ParameterizedTest
	@ValueSource( strings = {
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*",
			"S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D",
			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D",
			"*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*"
	})
	final void testParseFreeText_noTail(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*S1B_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*", oDQT.parseProductName(sInputText + sSuffix));
	}
	
	
	@ParameterizedTest
	@ValueSource( strings = {
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*",
			"*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D",
			"_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D"
	})
	final void testParseFreeText_noHeadNoTail(String sInputText) {
		DiasQueryTranslatorONDA oDQT = new DiasQueryTranslatorONDA();
		String sSuffix = " AND ( beginPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] AND endPosition:[2021-01-10T00:00:00.000Z TO 2021-01-12T23:59:59.999Z] )";
		assertEquals("*_IW_GRDH_1SDV_20210112T053522_20210112T053547_025117_02FD7D*", oDQT.parseProductName(sInputText + sSuffix));
	}
}

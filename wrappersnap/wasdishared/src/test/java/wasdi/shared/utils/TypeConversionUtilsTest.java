/**
 * 
 */
package wasdi.shared.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.Assert.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author c.nattero
 *
 */
class TypeConversionUtilsTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	
	/**
	 * Test method for {@link wasdi.shared.utils.TypeConversionUtils#getListOfObjects(java.util.List)}.
	 */
	@Test
	final void testGetListOfObjectsFromString_SameList() {
		List<String> asList = Arrays.asList("1", "2", "banana");
		//List<Object> aoList = Utils.getListOfObjects(asList); 
		assertEquals(asList, TypeConversionUtils.getListOfObjects(asList));
	}

	/**
	 * Test method for {@link wasdi.shared.utils.TypeConversionUtils#getListOfListsOfObjects(java.util.List)}.
	 */
	@Test
	final void testGetListOfListsOfObjects() {
		List<List<String>> aoList = new ArrayList<>();
		List<String> asList = Arrays.asList("1", "2", "banana");
		aoList.add(asList);
		asList = Arrays.asList("3", "4", "cherry");
		asList = Arrays.asList("5", "6", "plantain");
		
		assertEquals(aoList, TypeConversionUtils.getListOfListsOfObjects(aoList));
	}

}

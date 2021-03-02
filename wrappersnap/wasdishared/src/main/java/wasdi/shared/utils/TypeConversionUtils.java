/**
 * 
 */
package wasdi.shared.utils;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * @author c.nattero
 *
 */
public class TypeConversionUtils {

	public static List<Object> getListOfObjects(List<String> asList){
		Preconditions.checkNotNull(asList, "Passed a null list");
		try {
			List<Object> aoList = new ArrayList<Object>(asList.size());
			for (String sItem : asList) {
				aoList.add(sItem);
			}
			return aoList;
		} catch (Exception oE) {
			Utils.debugLog("Utils.getListOfObjectsFromString: " + oE);
		}
		return null;
	}

	public static List<List<Object>> getListOfListsOfObjects(List<List<String>> aoInput){
		Preconditions.checkNotNull(aoInput, "Input is null");
		try {
			List<List<Object>> aoOutput = new ArrayList<>();
			for (List<String> asList : aoInput) {
				aoOutput.add(getListOfObjects(asList));
			}
			return aoOutput;
		} catch (Exception oE) {
			Utils.debugLog("Utils.getListOfListsOfObjects: " + oE);
		}
		return null;
	}
}

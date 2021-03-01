package stats; /**
 * Created by s.adamo on 23/09/2016.
 */

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Launcher Configuration Reader
 * @author p.campanella
 *
 */
public class ConfigReader {

	private ConfigReader() {
		// / private constructor to hide the public implicit one 
	}

	static Map<String,String> m_aoProperties;

	private static void loadPropValues() throws IOException {

		try {
			Properties prop = new Properties();
			String propFileName = "config.properties";

			Path oPath = Paths.get(System.getProperty("user.dir"));
			String sPath = oPath.resolve("resources").resolve(propFileName).toAbsolutePath().normalize().toString();

			try(InputStream inputStream = new FileInputStream(sPath)){

				prop.load(inputStream);

				Enumeration<String> aoProperties =  (Enumeration<String>) prop.propertyNames();
				//Clear all
				m_aoProperties.clear();

				String sKey = aoProperties.nextElement();

				while (sKey != null) {
					m_aoProperties.put(sKey, prop.getProperty(sKey));
					if (aoProperties.hasMoreElements()) {
						sKey = aoProperties.nextElement();
					}
					else  {
						break;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public static String getPropValue(String sValue) throws IOException
	{
		if (m_aoProperties == null) {
			m_aoProperties = new HashMap<>();
			loadPropValues();
		}

		return m_aoProperties.get(sValue);
	}

	public static String getPropValue(String sValue, String sDefault) throws IOException
	{
		if (m_aoProperties == null) {
			m_aoProperties = new HashMap<>();
			loadPropValues();
		}

		String sRet = m_aoProperties.get(sValue);
		return sRet==null ? sDefault : sRet;
	}
}

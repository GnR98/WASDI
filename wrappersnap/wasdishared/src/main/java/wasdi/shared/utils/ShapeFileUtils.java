package wasdi.shared.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;

public class ShapeFileUtils {
	String m_sLoggerPrefix = "ShapeFileUtils."; 
	static Logger s_oLogger = Logger.getLogger(ShapeFileUtils.class);
		
	/**
	 * Constructor that initialize the prefix for logger
	 * @param sPrefix prefix of the logger
	 */
	public ShapeFileUtils(String sPrefix) {
		this.m_sLoggerPrefix = sPrefix + " - " + this.m_sLoggerPrefix ;
	}
	
	/**
	 * Check if a file is a (presumed) Shape File: it checks if it contains a .shp file
	 * It also checks that the total number of files inside the zip itself is 
	 * limited, to avoid processor cycle waste. 
	 * The limit imposed it 
	 * @param sZipFile Full path of the zip file
	 * @param iMaxFileInZipFile the maximum number of file allowed to be considered inside the zip file
	 * @return True if the zip contains a .shp file, False if it's not contained and the value iMaxFileInZipFile is exceeded
	 */
	public boolean isShapeFileZipped(String sZipFile, int iMaxFileInZipFile) {
		int iFileCounter = 0;
		Path oZipPath = Paths.get(sZipFile).toAbsolutePath().normalize();
		if(!oZipPath.toFile().exists()) {
			return false;
		}
		try (ZipFile oZipFile = new ZipFile(oZipPath.toString())){
		
			Enumeration<? extends ZipEntry> aoEntries = oZipFile.entries();
			
			while(aoEntries.hasMoreElements()) {
				ZipEntry oZipEntry = aoEntries.nextElement();
				
				if (iFileCounter > iMaxFileInZipFile) {
					s_oLogger.error(this.m_sLoggerPrefix + "isShapeFileZipped: too many files inside the zip. The limit is " + iMaxFileInZipFile);
					return false;
				}
				
				if (oZipEntry.getName().toLowerCase().endsWith(".shp")) {
					return true;
				}
				iFileCounter++;
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

}

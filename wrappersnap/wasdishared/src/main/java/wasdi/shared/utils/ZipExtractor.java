/*
 * Copyright (c) Fadeout Software 2020. Marco Menapace
 *
 */

package wasdi.shared.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

/**
 * Utility class for zip extraction operation with some security considerations.
 *
 * @see <a href="http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-1263">CVE-2018-1263</a>
 * @see <a href="http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-16131">CVE-2018-16131</a>
 */
public class ZipExtractor {
	// These parameters must be instantiated using configurations values
	static final int BUFFER = 512;
	long m_lToobigtotal = 1024L * 1024L * 1024L; // Max size of unzipped total Data, 1 GB
	long m_lToobigsingle = 1024L * 1024L * 512L; // Max size of unzipped total Data, 512 MB
	int m_lToomany = 1024; // Maximum number of files that can be extracted
	String m_sLoggerPrefix = "ZipExtractor."; 
	static Logger s_oLogger = Logger.getLogger(ZipExtractor.class);


	/**
	 * Safer version of the unzip method.
	 * This method take in account the total dimension extracted and the dimension for each file.
	 * If the extraction fails a clean method should be invoked, removing all the files extracted till
	 * that point.
	 * Try-with-resources approach for the ZipInputStream.
	 * The extraction creates a temporary directory which is programmatically deleted if
	 * - errors occurs (I.E. a file is bigger than the limit imposed)
	 * - the unzip is completed without errors
	 *
	 * @param sFilename the filename of the Zip file to be extracted
	 * @param sPath     The intended path in which the file should be extracted
	 * @throws java.io.IOException Throws IO exception in case the zip file is not founded
	 */
	public String unzip(String sFilename, String sPath) throws java.io.IOException {
		FileInputStream oFis = new FileInputStream(WasdiFileUtils.fixPathSeparator(sFilename));
		ZipEntry oEntry;
		int iEntries = 0;
		long lTotal = 0;
		long lSingle = 0;
		int iRandom = new SecureRandom().nextInt() & Integer.MAX_VALUE;
		String sTemp = "tmp-" + iRandom + File.separator;
		String sTempPath = WasdiFileUtils.fixPathSeparator(sPath);
		if(!sTempPath.endsWith(File.separator)) {
			sTempPath += File.separator;
		}
		sTempPath += sTemp;
		
		Path oPath = Paths.get(sTempPath).toAbsolutePath().normalize();
		if (oPath.toFile().mkdir()) {
			s_oLogger.info(m_sLoggerPrefix + "unzip: Temporary directory created");
		} else {
			throw new IOException("Can't create temporary dir " + sTempPath);
		}

		try (ZipInputStream oZis = new ZipInputStream(new BufferedInputStream(oFis))) {
			while ((oEntry = oZis.getNextEntry()) != null) {
				s_oLogger.info(m_sLoggerPrefix + "Extracting: " + oEntry);
				int iCount;
				byte[] ayData = new byte[BUFFER];
				// Write the files to the disk, but ensure that the filename is valid,
				// and that the file is not insanely big
				String sName = validateFilename(sTempPath + oEntry.getName(), sTempPath); // throws exception in case
				// Random used to mitigate attacks
				if (oEntry.isDirectory()) {
					s_oLogger.info(m_sLoggerPrefix + "unzip: Creating directory " + sName);
					if (new File(sName).mkdir()) {
						s_oLogger.info(m_sLoggerPrefix + "unzip: Directory "+sName+" created");
					}
					iEntries++; // count also a directory creation as an entry
					continue;
				}
				FileOutputStream oFos = new FileOutputStream(sName);
				try (BufferedOutputStream oDest = new BufferedOutputStream(oFos, BUFFER)){
					while (lTotal + BUFFER <= m_lToobigtotal &&
							lSingle + BUFFER <= m_lToobigsingle &&
							(iCount = oZis.read(ayData, 0, BUFFER)) != -1) {
						oDest.write(ayData, 0, iCount);
						lTotal += iCount;
						lSingle += iCount;
					}
					oDest.flush();
					oZis.closeEntry();
					iEntries++;
					if (lSingle + BUFFER > m_lToobigsingle) {
						cleanTempDir(sTempPath, sTemp);
						s_oLogger.error(m_sLoggerPrefix + "unzip: File being unzipped is too big. The limit is " + humanReadableByteCountSI(m_lToobigsingle));
						throw new IllegalStateException("File being unzipped is too big. The limit is " + humanReadableByteCountSI(m_lToobigsingle));
					}
					if (lTotal + BUFFER > m_lToobigtotal) {
						cleanTempDir(sTempPath, sTemp);
						s_oLogger.error(m_sLoggerPrefix + "unzip: File extraction interrupted because total dimension is over extraction limits. The limit is " + humanReadableByteCountSI(m_lToobigtotal));
						throw new IllegalStateException("File extraction interrupted because total dimension is over extraction limits. The limit is " + humanReadableByteCountSI(m_lToobigtotal));
					}
					if (iEntries > m_lToomany) {
						cleanTempDir(sTempPath, sTemp);
						s_oLogger.error(m_sLoggerPrefix + "unzip: Too many files inside the archive. The limit is "+m_lToomany);
						throw new IllegalStateException("Too many files inside the archive. The limit is "+m_lToomany);
					}
					lSingle = 0; // resets single file byte-counter
				}
			}
			/// IF everything went well cp temp content to original folder (overwrite it's fine) and delete temp dir
			cleanTempDir(sTempPath, sTemp);
		}
		return sTempPath;
	}

	/**
	 * Instantiates a ZipExtractor with default parameters
	 */
	public ZipExtractor() {
	}
	
	/**
	 * Instantiates a ZipExtractor with default parameters and initialize the logger
	 * prefix
	 * @param String sLoggerPrefix a string that must be passed in order to identify the process from a logging perspective
	 */
	public ZipExtractor(String sLoggerPrefix) {
		this.m_sLoggerPrefix = sLoggerPrefix + " - " + this.m_sLoggerPrefix;
	}

	/**
	 * Instantiates a ZipExtractor passing 3 critical parameters (in bytes)
	 *
	 * @param lToobigtotal  the total maximum size allowed for extraction
	 * @param lToobigsingle the maximum single size for each file
	 * @param lToomany      the maximum number of files allowed to be extracted
	 * @param String sLoggerPrefix a string that must be passed in order to identify the process from a logging perspective
	 */
	public ZipExtractor(long lToobigtotal, long lToobigsingle, int lToomany, String sLoggerPrefix) {
		this.m_lToobigtotal = lToobigtotal;
		this.m_lToobigsingle = lToobigsingle;
		this.m_lToomany = lToomany;
		this.m_sLoggerPrefix = sLoggerPrefix + " - " + this.m_sLoggerPrefix; 
	}

	/**
	 * Checks that the the output dir is coherent with the current dir.
	 * This methods filters out directory traversal attempts.
	 *
	 * @param sFilename    the file name of the zip file to be extracted
	 * @param sIntendedDir the intended directory where the extraction must be done
	 * @return the canonical path of the file
	 * @throws java.io.IOException in case of the file is outside target extraction directory
	 */
	private String validateFilename(String sFilename, String sIntendedDir)
			throws java.io.IOException {
		File oF = new File(sFilename);
		String sCanonicalPath = oF.getCanonicalPath();

		File oIDir = new File(sIntendedDir);
		String sCanonicalID = oIDir.getCanonicalPath();

		if (sCanonicalPath.startsWith(sCanonicalID)) {
			return sCanonicalPath;
		} else {
			s_oLogger.error(m_sLoggerPrefix + "validateFilename: File is outside extraction target directory." );
			throw new IllegalStateException("File is outside extraction target directory.");
		}
	}

	/**
	 * Util methods to obtain a human readable byte count(GB,MB,KB) from
	 * a byte count.
	 *
	 * @param lBytes the number of the bytes that should be considered
	 * @return String with the human readable string (e.g. kB, MB, GB)
	 */
	private String humanReadableByteCountSI(long lBytes) {
		if (-1000 < lBytes && lBytes < 1000) {
			return lBytes + " B";
		}
		CharacterIterator oCi = new StringCharacterIterator("kMGTPE");
		while (lBytes <= -999_950 || lBytes >= 999_950) {
			lBytes /= 1000;
			oCi.next();
		}
		return String.format("%.1f %cB", lBytes / 1000.0, oCi.current());
	}

	/**
	 * After a successful extraction this method is invoked in order to move the
	 * temporary folder content and cleanup the directory.
	 * @param sTempPath the temp path where to clean up
	 * @param sTemp the folder name of the temp path [temp-{random-generated-id}]
	 * @return True if the operation is done without errors nor exceptions. False instead
	 */
	private boolean cleanTempDir(String sTempPath, String sTemp) {
		File oDir = new File(sTempPath); // point one dir

		try (Stream<Path> aoPathStream = Files.walk(oDir.toPath())){
			aoPathStream
			.sorted(Comparator.naturalOrder()). // this make the dir before other files
			map(Path::toFile).
			forEach(oFile -> {
				try {
					File oDest = new File(oFile.getCanonicalPath().replace(sTemp, "")); // removes the tmp-part from the destination files
					if (oDest.isDirectory()) return; // checks the existence of the dir
					Files.copy(oFile.getCanonicalFile().toPath(), oDest.getCanonicalFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException oE) {
					oE.printStackTrace();
				}
			});
		} catch (IOException oE) {
			oE.printStackTrace();
			return false;
		}

		try {
			deleteDirectory(oDir.toPath());
		} catch (IOException oE) {
			oE.printStackTrace();
			return false;
		}
		return true;

	}

	/**
	 * Util method that recursively delete the path passed as input
	 * On unix systems is equivalent to "rm -rf [path]"
	 * @param oToBeDeleted path to be deleted
	 * @throws IOException
	 */
	protected void deleteDirectory(Path oToBeDeleted) throws IOException {
		try (Stream<Path> aoPathStream = Files.walk(oToBeDeleted)){
			aoPathStream.sorted(Comparator.reverseOrder())
			.map(Path::toFile)
			.forEach(File::delete);
		}
	}

	/**
	 * Setter of the parameter that checks the total size of the extracted files
	 *
	 * @param lTooBigTotal size, in bytes, of the limit
	 */
	public void setTOOBIGTOTAL(long lTooBigTotal) {
		m_lToobigtotal = lTooBigTotal;
	}

	/**
	 * Setter of the parameter that checks the size of each single file
	 *
	 * @param lTooBigSingle size, in bytes, of the limit
	 */
	public void setTOOBIGSINGLE(long lTooBigSingle) {
		m_lToobigsingle = lTooBigSingle;
	}

	/**
	 * Setter of the parameter that checks the number of files being extracted
	 *
	 * @param iTooMany count of the files that must not be exceed
	 */
	public void setTOOMANY(int iTooMany) {
		m_lToomany = iTooMany;
	}

}

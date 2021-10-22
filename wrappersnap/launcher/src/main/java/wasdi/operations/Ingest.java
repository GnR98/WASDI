package wasdi.operations;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import wasdi.LauncherMain;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.payload.IngestPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.ShapeFileUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipExtractor;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class Ingest extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		m_oLocalLogger.debug("Ingest.executeOperation");

		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

        try {
        	
            IngestFileParameter oParameter = (IngestFileParameter) oParam;
            
            File oFileToIngestPath = new File(oParameter.getFilePath());

            if (!oFileToIngestPath.canRead()) {
                String sMsg = "Ingest.executeOperation: ERROR: unable to access file to Ingest " + oFileToIngestPath.getAbsolutePath();
                m_oLocalLogger.error(sMsg);
                throw new IOException("Unable to access file to Ingest");
            }

            
            // get file size
            long lFileSizeByte = oFileToIngestPath.length();

            // set file size
            setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);
            // Update status
            updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

            String sDestinationPath = LauncherMain.getWorkspacePath(oParameter);

            File oDstDir = new File(sDestinationPath);

            if (!oDstDir.exists()) {
                oDstDir.mkdirs();
            }

            m_oProcessWorkspaceLogger.log("Ingest Start - File " + oFileToIngestPath.getName() + " in Workspace " + oParameter.getWorkspace());

            if (!oDstDir.isDirectory() || !oDstDir.canWrite()) {
                m_oLocalLogger.error("Ingest.executeOperation: ERROR: unable to access destination directory " + oDstDir.getAbsolutePath());
                m_oProcessWorkspaceLogger.log("Error accessing destination directory");
                throw new IOException("Unable to access destination directory for the Workspace");
            }

            // Usually, we do not unzip after the copy
            boolean bUnzipAfterCopy = false;

            // Try to read the Product view Model
			WasdiProductReader oReadProduct = WasdiProductReaderFactory.getProductReader(oFileToIngestPath);
			ProductViewModel oImportProductViewModel = oReadProduct.getProductViewModel();

            String sDestinationFileName = oFileToIngestPath.getName();

            // Did we got the View Model ?
            if (oImportProductViewModel == null) {

                m_oLocalLogger.warn("Ingest.executeOperation: Impossible to read the Product View Model");

                // Check if this is a Zipped Shape File
                if (oFileToIngestPath.getName().toLowerCase().endsWith(".zip")) {
                    ShapeFileUtils oShapeFileUtils = new ShapeFileUtils(oParameter.getProcessObjId());
                    if (oShapeFileUtils.isShapeFileZipped(oFileToIngestPath.getPath(), 30)) {

                        // May be.
                        m_oLocalLogger.info("Ingest.executeOperation: File to ingest looks can be a zipped shape file, try to unzip");

                        // Unzip
                        ZipExtractor oZipExtractor = new ZipExtractor(oParameter.getProcessObjId());
                        oZipExtractor.unzip(oFileToIngestPath.getCanonicalPath(), oFileToIngestPath.getParent());

                        // Get the name of shp from the zip file (case safe)
                        String sShapeFileTest = oShapeFileUtils.getShpFileNameFromZipFile(oFileToIngestPath.getPath(), 30);

                        if (Utils.isNullOrEmpty(sShapeFileTest) == false) {
                            // Ok, we have our file
                            File oShapeFileIngestPath = new File(oFileToIngestPath.getParent() + "/" + sShapeFileTest);
							
							WasdiProductReader oReadShapeProduct = WasdiProductReaderFactory.getProductReader(oShapeFileIngestPath);
							
                            // Now get the view model again
							oImportProductViewModel = oReadShapeProduct.getProductViewModel();
                            bUnzipAfterCopy = true;
                            m_oLocalLogger.info("Ingest.executeOperation: Ok, zipped shape file found");

                            m_oProcessWorkspaceLogger.log("Found shape file");

                            sDestinationFileName = sShapeFileTest;
                        }
                    }
                }
            }

            // If we do not have the view model here, we were not able to open the file
            if (oImportProductViewModel == null) {

                m_oProcessWorkspaceLogger.log("Error reading the input product.");

                m_oLocalLogger.error("Ingest.executeOperation: ERROR: unable to get the product view model");
                throw new IOException("Unable to get the product view model");
            }

            updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);

            // copy file to workspace directory
            if (!oFileToIngestPath.getParent().equals(oDstDir.getAbsolutePath())) {

                m_oLocalLogger.debug("Ingest.executeOperation: File in another folder make a copy");
                FileUtils.copyFileToDirectory(oFileToIngestPath, oDstDir);

                m_oProcessWorkspaceLogger.log("File ingestion done");

                // Must be unzipped?
                if (bUnzipAfterCopy) {

                    m_oLocalLogger.debug("File must be unzipped");
                    ZipExtractor oZipExtractor = new ZipExtractor(oParameter.getProcessObjId());
                    oZipExtractor.unzip(oFileToIngestPath.getCanonicalPath(), oDstDir.getCanonicalPath());
                    m_oLocalLogger.debug("Unzip done");

                    m_oProcessWorkspaceLogger.log("File unzipped");
                }
            } else {
                m_oLocalLogger.debug("Ingest.executeOperation: File already in the right path no need to copy");
                m_oProcessWorkspaceLogger.log("File already in place");
            }

            File oDstFile = new File(oDstDir, sDestinationFileName);

            updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 75);

            // Snap set the name of geotiff files as geotiff: let replace with the file name
            if (oImportProductViewModel.getName().equals("geotiff")) {
                oImportProductViewModel.setName(oImportProductViewModel.getFileName());
            }

            // add product to db
            addProductToDbAndWorkspaceAndSendToRabbit(oImportProductViewModel, oDstFile.getAbsolutePath(),
                    oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.INGEST.name(), null, true, true, oParameter.getStyle());

            try {
                IngestPayload oPayload = new IngestPayload();
                oPayload.setFile(oFileToIngestPath.getName());
                oPayload.setWorkspace(oParameter.getWorkspace());

                String sPayload = LauncherMain.s_oMapper.writeValueAsString(oPayload);
                oProcessWorkspace.setPayload(sPayload);
            } catch (Exception oPayloadEx) {
                m_oLocalLogger.error("Ingest.executeOperation: payload exception: " + oPayloadEx.toString());
            }

            updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            m_oProcessWorkspaceLogger.log("Ingestion Done (Burp) - " + new EndMessageProvider().getGood());

            return true;

        } 
        catch (Throwable e) {
            String sMsg = "Ingest.executeOperation: ERROR: Exception occurrend during file ingestion";

            m_oProcessWorkspaceLogger.log("Exception ingesting the file");

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
            m_oLocalLogger.error("Ingest.executeOperation: " + sMsg);
            m_oLocalLogger.error("Ingest.executeOperation: " + sError);

            if (oProcessWorkspace != null) {
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            }
            if (m_oSendToRabbit != null) {
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.INGEST.name(), oParam.getWorkspace(), sError, oParam.getExchange());
            }

        }
        
		return false;
	}

}

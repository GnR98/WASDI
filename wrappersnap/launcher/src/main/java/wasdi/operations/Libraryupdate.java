package wasdi.operations;

import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;

public class Libraryupdate extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		m_oLocalLogger.debug("Libraryupdate.executeOperation");
		
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

		
		try {		
	        // Update Lib
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;
	        
	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType(),
	        		WasdiConfig.s_oConfig.paths.DownloadRootPath,
	        		WasdiConfig.s_oConfig.paths.DOCKER_TEMPLATE_PATH,
	        		WasdiConfig.s_oConfig.TOMCAT_USER);
	        oEngine.setParameter(oParameter);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        return oEngine.libraryUpdate(oParameter);
		}
		catch (Exception oEx) {
			m_oLocalLogger.error("Libraryupdate.executeOperation: exception", oEx);
		}
		
        return false;
	}

}

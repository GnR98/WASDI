package wasdi.operations;

import java.io.File;

import wasdi.LauncherMain;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;

public class Redeployprocessor extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		m_oLocalLogger.debug("Redeployprocessor.executeOperation");
		
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}
		
		try {
	        // redeploy User Processor
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;			
			
            // First Check if processor exists
            String sProcessorName = oParameter.getName();
            String sProcessorId = oParameter.getProcessorID();

            ProcessorRepository oProcessorRepository = new ProcessorRepository();
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            // Check processor
            if (oProcessor == null) {
                LauncherMain.s_oLogger.error("Redeployprocessor.executeOperation: oProcessor is null [" + sProcessorId + "]");
                return false;
            }

            // Set the processor path
            String sDownloadRootPath = WasdiConfig.Current.paths.downloadRootPath;
            if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";

            String sProcessorFolder = sDownloadRootPath + "/processors/" + sProcessorName + "/";            
            File oProcessorFolder = new File(sProcessorFolder);
            
            // Is the processor installed in this node?
            if (!oProcessorFolder.exists()) {
                LauncherMain.s_oLogger.error("Redeployprocessor.executeOperation: Processor [" + sProcessorName + "] not installed in this node, return");
                return true;            	
            }

	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
	        oEngine.setSendToRabbit(m_oSendToRabbit);
	        oEngine.setParameter(oParameter);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        boolean bRet =  oEngine.redeploy(oParameter);
	        
	        try {
	        	
	        	// In the exchange we should have the workspace from there the user requested the Redeploy
	        	String sOriginalWorkspaceId = oParam.getExchange();
	        	
	        	// Check if it is valid
	        	if (Utils.isNullOrEmpty(sOriginalWorkspaceId)==false) {
	        		
	        		// Read the workspace
	        		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
	        		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sOriginalWorkspaceId);
	        		
	        		if (oWorkspace != null) {
	        			
	        			String sNodeCode = "wasdi";
	        			
	        			if (!Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
	        				sNodeCode = oWorkspace.getNodeCode();
	        			}
	        			
	        			// This is the computing node where the request came from?
	        			if (sNodeCode.equals(WasdiConfig.Current.nodeCode)) {
	        				
	        				// Notify the user
				        	String sName = oParameter.getName();
				        	
				        	if (Utils.isNullOrEmpty(sName)) sName = "Your Processor";
				        	
				            String sInfo = "Re Deploy Done<br>" + sName + " is now available";
				            
				            if (!bRet) {
				            	sInfo = "GURU MEDITATION<br>There was an error re-deploying " + sName + " :(";
				            }
				            
				            m_oSendToRabbit.SendRabbitMessage(bRet, LauncherOperations.INFO.name(), oParam.getExchange(), sInfo, oParam.getExchange());	        				
	        			}
	        		}	        		
	        	}
	        	
	        }
	        catch (Exception oRabbitException) {
				m_oLocalLogger.error("Deployprocessor.executeOperation: exception sending Rabbit Message", oRabbitException);
			}
            
            return bRet;	        
		}
		catch (Exception oEx) {
			m_oLocalLogger.error("Redeployprocessor.executeOperation: exception", oEx);
		}
		
		return false;        

	}

}

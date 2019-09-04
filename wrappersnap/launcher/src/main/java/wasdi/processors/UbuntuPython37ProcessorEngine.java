package wasdi.processors;

import java.util.ArrayList;

public class UbuntuPython37ProcessorEngine extends DockerProcessorEngine {

	public UbuntuPython37ProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath)  {
		super(sWorkingRootPath,sDockerTemplatePath);

		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "python37";
		
	}

	@Override
	protected void handleRunCommand(String sCommand, ArrayList<String> asArgs) {
		
	}

	@Override
	protected void handleBuildCommand(String sCommand, ArrayList<String> asArgs) {
		
	}

	@Override
	protected void handleUnzippedProcessor(String sProcessorFolder) {
		
	}
}

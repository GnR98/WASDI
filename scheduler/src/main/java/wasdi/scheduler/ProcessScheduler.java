package wasdi.scheduler;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;

import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;

public class ProcessScheduler extends Thread {
	
	/**
	 * the folder that contains the serialised parameters used by each process
	 */
	protected File m_oParametersFilesFolder = null;
	/**
	 * sleeping time between iterations
	 */
	protected long m_lSleepingTimeMS = 2000;
	/**
	 * sleeping time between iterations
	 */
	protected long m_lWaitProcessStartMS = 2000;
	/**
	 * number concurrent process
	 */
	protected int m_iNumberOfConcurrentProcess = 1;

	/**
	 * launcher installation path
	 */
	protected String m_sLauncherPath;
	/**
	 * java executable path
	 */
	protected String m_sJavaExePath;
	/**
	 * mongo repository for processworkspace collection
	 */
	protected ProcessWorkspaceRepository m_oProcessWorkspaceRepository;
	/**
	 * map of already launched processes. Used to avoid multiple execution of the same process
	 */
	protected Map<String, Date> m_aoLaunchedProcesses = new HashMap<String, Date>();
	
	/**
	 * Process Scheduler Log Prefix
	 */
	protected String m_sLogPrefix = "ProcessScheduler: ";
	
	/**
	 * Key of this scheduler instance
	 */
	protected String  m_sSchedulerKey = "ProcessScheduler";
	
	/**
	 * Wasdi Node
	 */
	protected String  m_sWasdiNode = "wasdi";	
	
	/**
	 * Bool flag to stop the thread
	 */
	private volatile boolean m_bRunning = true;
	
	public boolean init(String sSchedulerKey) {
		
		try {
			// Save the scheduler Key
			m_sSchedulerKey = sSchedulerKey;
			// Init the scheduler log prefix
			m_sLogPrefix = m_sSchedulerKey + ": ";
			
			//Read the Serialisation Path
			File oFolder = new File(ConfigReader.getPropValue("SerializationPath", "/usr/lib/wasdi/params/"));
			
			if (!oFolder.isDirectory()) {
				WasdiScheduler.log("ERROR: cannot access parameters folder: " + oFolder.getAbsolutePath());
			}
			
			// Save the path 
			m_oParametersFilesFolder = oFolder;
			
			// Read Max Size of Concurrent Processes of this scheduler 
			try {
				int iMaxConcurrents = Integer.parseInt(ConfigReader.getPropValue(m_sSchedulerKey.toUpperCase()+"_MAX_QUEUE"));
				if (iMaxConcurrents>0) m_iNumberOfConcurrentProcess = iMaxConcurrents;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			try {
				long iThreadSleep = Long.parseLong(ConfigReader.getPropValue("ProcessingThreadSleepingTimeMS", "2000"));
				if (iThreadSleep>0) m_lSleepingTimeMS = iThreadSleep;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				long iStartWaitSleep = Long.parseLong(ConfigReader.getPropValue("ProcessingThreadWaitStartMS", "2000"));
				if (iStartWaitSleep>0) m_lWaitProcessStartMS = iStartWaitSleep;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// Read the Lancher Path
			m_sLauncherPath = ConfigReader.getPropValue("LauncherPath", "/usr/lib/wasdi/launcher/launcher.jar");
			// Read Java Exe Path
			m_sJavaExePath = ConfigReader.getPropValue("JavaExe", "java");
			// Read Wasdi Node Id
			m_sWasdiNode = ConfigReader.getPropValue("WASDI_NODE", "wasdi");
			
			// Create the Repo
			m_oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		}
		catch (Exception oEx) {
			WasdiScheduler.error("ProcessScheduler.init Exception: " + oEx.toString());
			return false;
		}
		return true;
		
	}
	
	@Override
	public void run() {
		
		WasdiScheduler.log(m_sLogPrefix+" Thread started");
		
		// Infinite Loop		
		while(getIsRunning()) {
			
			try {
				// Get the updated list of running processes
				List<ProcessWorkspace> aoRunningList = getRunningList();
				
				WasdiScheduler.log(m_sLogPrefix + aoRunningList.size() + " Running Processes");
				
				// Do we have any free slot?
				if (aoRunningList.size() < m_iNumberOfConcurrentProcess) {
					
					// Yes: get the list of Ready Processes
					List<ProcessWorkspace> aoReadyList = getReadyList();
					
					// For each ready process
					while (aoReadyList.size()> 0) {
						
						// If we fished free slots, stop the cycle
						if (aoRunningList.size()>=m_iNumberOfConcurrentProcess) break;
												
						// Get the ready process
						ProcessWorkspace oReadyProcess = aoReadyList.get(0);
						
						// Update the status to running
						oReadyProcess.setStatus(ProcessStatus.RUNNING.name());
						m_oProcessWorkspaceRepository.updateProcess(oReadyProcess);
						
						WasdiScheduler.log(m_sLogPrefix + "Resumed " + oReadyProcess.getProcessObjId());
						
						// Move in the running list
						aoReadyList.remove(0);
						aoRunningList.add(oReadyProcess);						
					}
					
					// Get the list of Created Processes
					List<ProcessWorkspace> aoCreatedList = getCreatedList();
					
					// For each created process
					while (aoCreatedList.size()> 0) {
						
						// If we fished free slots, stop the cycle
						if (aoRunningList.size()>=m_iNumberOfConcurrentProcess) break;
												
						// Get the Created process
						ProcessWorkspace oCreatedProcess = aoCreatedList.get(0);
						
						// Update the status to running
						oCreatedProcess.setStatus(ProcessStatus.RUNNING.name());
						m_oProcessWorkspaceRepository.updateProcess(oCreatedProcess);
						
						// Execute the process
						executeProcess(oCreatedProcess);
						WasdiScheduler.log(m_sLogPrefix + "Lauched " + oCreatedProcess.getProcessObjId());
						// Give a little bit of time to the launcher to start
						waitForProcessToStart();
						
						// Move The process in the running list
						aoCreatedList.remove(0);
						aoRunningList.add(oCreatedProcess);
						
					}
				}
				else {
					WasdiScheduler.log(m_sLogPrefix + "Running Queue full, next cycle.");
				}
				
			} 
			catch (Exception e) {
				e.printStackTrace();
			} 
			finally {
				try {
					//Sleep before starting next iteration
					catnap();
				} 
				catch (Exception oEx) {
					WasdiScheduler.error(oEx.getMessage());
				}
			}
		}
	}
	
	/**
	 * Get the list of running processes
	 * @return list of running processes
	 */
	protected List<ProcessWorkspace> getRunningList() {
		return m_oProcessWorkspaceRepository.getRunningProcessesByNode(m_sWasdiNode);
	}
	
	/**
	 * Get the list of created processes
	 * @return list of created processes
	 */
	protected List<ProcessWorkspace> getCreatedList() {
		return m_oProcessWorkspaceRepository.getCreatedProcessesByNode(m_sWasdiNode);
	}
	
	/**
	 * Get the list of ready processes
	 * @return list of ready processes
	 */
	protected List<ProcessWorkspace> getReadyList() {
		return m_oProcessWorkspaceRepository.getReadyProcessesByNode(m_sWasdiNode);
	}	
	
	/**
	 * Trigger the real execution of a process
	 * @param oProcessWorkspace
	 * @return Process Object Identifier
	 */
	private String executeProcess(ProcessWorkspace oProcessWorkspace) {
		
		File oParameterFilePath = new File(m_oParametersFilesFolder, oProcessWorkspace.getProcessObjId());

		String sShellExString = m_sJavaExePath + " -jar " + m_sLauncherPath +
				" -operation " + oProcessWorkspace.getOperationType() +
				" -parameter " + oParameterFilePath.getAbsolutePath();

		WasdiScheduler.log(m_sLogPrefix + "executing command for process " + oProcessWorkspace.getProcessObjId() + ": ");
		WasdiScheduler.log(sShellExString);

		try {
			
			Process oSystemProc = Runtime.getRuntime().exec(sShellExString);
			WasdiScheduler.log(m_sLogPrefix + "executed!!!");
			m_aoLaunchedProcesses.put(oProcessWorkspace.getProcessObjId(), new Date());
			
		} 
		catch (IOException oEx) {
			WasdiScheduler.log(m_sLogPrefix + " executeProcess : Exception" + oEx.toString());
			oEx.printStackTrace();
			WasdiScheduler.log(m_sLogPrefix + " executeProcess : try to set the process in Error");
			
			try {
				oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
				m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);				
				WasdiScheduler.log(m_sLogPrefix + " executeProcess : Error status set");
			}
			catch (Exception oInnerEx) {
				WasdiScheduler.log(m_sLogPrefix + " executeProcess : INNER Exception" + oInnerEx.toString());
				oInnerEx.printStackTrace();
			}
			
			return null;
		}
		
		return oProcessWorkspace.getProcessObjId();
	}
	
	/**
	 * Sleep method for the Scheduler Cycle
	 */
	protected void catnap() {
		try {
			sleep(m_lSleepingTimeMS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sleep method to ensure the start of a new process
	 */
	protected void waitForProcessToStart() {
		try {
			sleep(m_lWaitProcessStartMS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Safe method to get the thread status
	 * @return
	 */
	public synchronized boolean getIsRunning() {
		return m_bRunning;
	}
	
	/**
	 * Stops the thread
	 */
	public synchronized void stopThread() {
		interrupt();
		m_bRunning = false;
	}


}

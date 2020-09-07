import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.xml.DOMConfigurator;

import wasdi.ConfigReader;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.AppCategory;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.WorkspaceSharing;
import wasdi.shared.data.AppsCategoriesRepository;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.WorkspaceSharingRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

public class dbUtils {
	
	public static boolean isProductOnThisNode(DownloadedFile oDownloadedFile, WorkspaceRepository oWorkspaceRepository) {
		try {
			String sPath = oDownloadedFile.getFilePath();
			
			String [] asSplittedPath = sPath.split("/");
			
			if (asSplittedPath == null) {
				System.out.println(oDownloadedFile.getFileName() + " - CANNOT SPLIT PATH");
				return false;
			}
			
			if (asSplittedPath.length<2) {
				System.out.println(oDownloadedFile.getFileName() + " - SPLITTED PATH HAS ONLY ONE ELEMENT");
				return false;
			}
			
			String sWorkspaceId = asSplittedPath[asSplittedPath.length-2];
			
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			
			if (oWorkspace == null) {
				System.out.println(oDownloadedFile.getFileName() + " - IMPOSSIBILE TO FIND WORKSPACE " + sWorkspaceId);
				return false;
				
			}
			
			String sNode = oWorkspace.getNodeCode();
			
			if (Utils.isNullOrEmpty(sNode)) {
				sNode = "wasdi";
			}
			
			if (sNode.equals(s_sMyNodeCode) == false) {
				System.out.println(oDownloadedFile.getFileName() + " - IS ON ANOTHER NODE  [" + sNode + "]");
				return false;
			}
			
		}
		catch (Exception oEx) {
			System.out.println(" DOWNLOADED FILE EX " + oEx.toString());
			oEx.printStackTrace();
			return false;
		}
		
		return true;
	}

	/**
	 * Tools to fix the downloaded products table
	 */
	public static void downloadedProducts() {
		try {
			
	        System.out.println("Ok, what we do with downloaded products?");
	        
	        System.out.println("\t1 - List products with broken files");
	        System.out.println("\t2 - Delete products with broken files");
	        System.out.println("\t3 - Clear S1 S2 published bands");
	        System.out.println("\tx - back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();
	        
	        boolean bDelete = false;
	        
	        if (sInputString.equals("1") || sInputString.equals("2")) {
	        	
		        if (sInputString.equals("1")) {
		        	bDelete = false;
		        }
		        else if (sInputString.equals("2")) {
		        	bDelete = true;
		        }		        
				
		        WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
				
				DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
				
				List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepository.getList();
				
				System.out.println("Found " + aoDownloadedFiles.size() + " Downloaded Files");
				
				int iDeleted = 0;
				
				for (DownloadedFile oDownloadedFile : aoDownloadedFiles) {
					
					String sPath = oDownloadedFile.getFilePath();
					File oFile = new File(sPath);
					
					if (oFile.exists() == false) {
						
						if (!isProductOnThisNode(oDownloadedFile, oWorkspaceRepository)) continue;
						
						iDeleted ++;
						
						if (bDelete == false) {
							System.out.println(oDownloadedFile.getFileName() + " - FILE DOES NOT EXISTS " + oDownloadedFile.getFilePath());
						}
						else {
							
							System.out.println("DELETING " + oDownloadedFile.getFileName() + " - FILE DOES NOT EXISTS " + oDownloadedFile.getFilePath());
							oDownloadedFilesRepository.deleteByFilePath(oDownloadedFile.getFilePath());
							
							// Delete Product Workspace
							ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
							oProductWorkspaceRepository.deleteByProductName(oDownloadedFile.getFilePath());

							System.out.println("DELETED " + oDownloadedFile.getFileName());
						}
					}
				}
				
				System.out.println("");
				System.out.println("");
				System.out.println("---------------------------------------------");
				String sSummary = "";
				if (bDelete) {
					sSummary = "DELETED " + iDeleted + " db Entry";
				}
				else {
					sSummary = "Found " + iDeleted + " db Entry to delete";
				}
				
				System.out.println(sSummary);
	        }
	        else if (sInputString.equals("3")) {
	        	System.out.println("Clean S1 and S2 published bands");
	        	cleanPublishedBands();
	        }
	        else if (sInputString.equals("x")) {
	        	return;
	        }
		}
		catch (Exception oEx) {
			System.out.println("downloadedProducts: exception " + oEx.toString());
			oEx.printStackTrace();
		}
	}
	
	/**
	 * Utils to fix product workspace table
	 */
	public static void productWorkspace() {
		try {
			
	        System.out.println("Ok, what we do with product Workspaces?");
	        
	        System.out.println("\t1 - Clean by not existing Workspace");
	        System.out.println("\t2 - Clean by not existing Product Name");
	        System.out.println("\tx - Back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();
	        
	        boolean bWorkspace = false;
	        
	        if (sInputString.equals("x")) {
	        	return;
	        }
	        
	        if (sInputString.equals("1")) {
	        	bWorkspace = true;
	        }
	        else if (sInputString.equals("2")) {
	        	bWorkspace = false;
	        }		        
	        
	        if (sInputString.equals("1") || sInputString.equals("2")) {
		        if (bWorkspace) {
		        	System.out.println("Deleting all product workspace with not existing workspace");
		        	
		        	WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		        	ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
		        	
		        	List<ProductWorkspace> aoAllProductWorkspace = oProductWorkspaceRepository.getList();
		        	
		        	int iDeleted=0;
		        	
		        	System.out.println("productWorkspace: found " + aoAllProductWorkspace.size() + " Product Workspace");
		        	
		        	for (ProductWorkspace oProductWorkspace : aoAllProductWorkspace) {
						
		        		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oProductWorkspace.getWorkspaceId());
		        		
		        		if (oWorkspace == null) {
		        			System.out.println("productWorkspace: workspace " + oProductWorkspace.getWorkspaceId() + " does not exist, delete entry");
		        			oProductWorkspaceRepository.deleteByProductName(oProductWorkspace.getProductName());
		        			iDeleted++;
		        		}
					}
		        	
		        	System.out.println("");
		        	System.out.println("---------------------------------------------------");
		        	System.out.println("productWorkspace: Deleted " + iDeleted + " Product Workspace");
		        }
		        else {
		        	System.out.println("Deleting all product workspace with not existing product Name");
		        	
		        	DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		        	ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
		        	
		        	List<ProductWorkspace> aoAllProductWorkspace = oProductWorkspaceRepository.getList();
		        	
		        	int iDeleted=0;
		        	
		        	System.out.println("productWorkspace: found " + aoAllProductWorkspace.size() + " Product Workspace");
		        	
		        	for (ProductWorkspace oProductWorkspace : aoAllProductWorkspace) {
						
		        		DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(oProductWorkspace.getProductName());
		        		
		        		if (oDownloadedFile == null) {
		        			System.out.println("productWorkspace: Downloaded File " + oProductWorkspace.getProductName() + " does not exist, delete entry");
		        			oProductWorkspaceRepository.deleteByProductName(oProductWorkspace.getProductName());
		        			iDeleted++;
		        		}
					}
		        	
		        	System.out.println("");
		        	System.out.println("---------------------------------------------------");
		        	System.out.println("productWorkspace: Deleted " + iDeleted + " Product Workspace");
		        }
	        }
			
			
		}
		catch (Exception oEx) {
			System.out.println("productWorkspace: exception " + oEx);
			oEx.printStackTrace();
		}
	}

	
	public static void processors() {
		
		try {
			
	        System.out.println("Ok, what we do with processors?");
	        
	        System.out.println("\t1 - Extract Log");
	        System.out.println("\t2 - Clear Log");
	        System.out.println("\t3 - Redeploy");
	        System.out.println("\t4 - Fix Processor Creation/Update date");
	        System.out.println("\tx - Back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();

	        if (sInputString.equals("x")) {
	        	return;
	        }	        
	        
	        ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();

	        if (sInputString.equals("1")) {
	        	
		        System.out.println("Please input ProcessWorkspaceId");
		        String sProcessWorkspaceId = s_oScanner.nextLine();
	        	
	        	
	        	String sOuptutFile = "./" + sProcessWorkspaceId + ".txt";
	        	
				System.out.println("Extracting Log of Processor " + sProcessWorkspaceId + " in " + sOuptutFile);
				
				List<ProcessorLog> aoLogs = oProcessorLogRepository.getLogsByProcessWorkspaceId(sProcessWorkspaceId);
				
				if (aoLogs == null) {
					System.out.println("Log row list is null, exit");
					return;			
				}
				
				System.out.println("Log Rows " + aoLogs.size());
				
				try (FileWriter oWriter = new FileWriter(sOuptutFile)) {
					
					try (BufferedWriter oBufferedWriter = new BufferedWriter(oWriter)) {
						for (ProcessorLog oLogRow : aoLogs) {
							oBufferedWriter.write(oLogRow.getLogDate());
							oBufferedWriter.write(" - " );
							oBufferedWriter.write(oLogRow.getLogRow());
							oBufferedWriter.write("\n");
						}
						
						oBufferedWriter.flush();
						oBufferedWriter.close();						
					}
				} 
				catch (IOException e) {
					System.err.format("IOException: %s%n", e);
				}
				
				System.out.println("Log Extraction done");	        	
	        }
	        else if (sInputString.equals("2")) {
	        	
		        System.out.println("Please input ProcessWorkspaceId");
		        String sProcessWorkspaceId = s_oScanner.nextLine();
	        	
	        	System.out.println("Deleting logs of " + sProcessWorkspaceId);
	        	oProcessorLogRepository.deleteLogsByProcessWorkspaceId(sProcessWorkspaceId);
	        	System.out.println(sProcessWorkspaceId + " logs DELETED");
	        }
	        else if (sInputString.equals("3")) {
		        System.out.println("Please input Processor Name");
		        String sProcessorName = s_oScanner.nextLine();
		        
		        ProcessorRepository oProcessorRepository = new ProcessorRepository();
		        Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);
		        
		        if (oProcessor == null) {
		        	System.out.println(sProcessorName + " does NOT exists");
		        	return;
		        }
		        
		        String sBasePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
		        String sDockerTemplatePath = ConfigReader.getPropValue("DOCKER_TEMPLATE_PATH");
		        
		        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oProcessor.getType(), sBasePath, sDockerTemplatePath);
		        
		        ProcessorParameter oParameter = new ProcessorParameter();
		        
		        oParameter.setName(oProcessor.getName());
		        oParameter.setProcessorID(oProcessor.getProcessorId());
		        
		        System.out.println("Created Parameter with Name: " + oProcessor.getName() + " ProcessorId: " + oProcessor.getProcessorId());
		        
		        oEngine.redeploy(oParameter);
		        
	        }
	        else if (sInputString.equals("4")) { 
	        	ProcessorRepository oProcessorRepository = new ProcessorRepository();
	        	List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();
	        	
	        	System.out.println("Found " + aoProcessors.size() + " Processors");
	        	
	        	Date oNow = new Date();
	        	
	        	for (Processor oProcessor : aoProcessors) {
	        		
	        		boolean bUpdate = false;
	        		
	        		if (oProcessor.getUploadDate() == null) {
	        			oProcessor.setUploadDate((double) oNow.getTime());
	        			bUpdate = true;	        			
	        		}
	        		else if (oProcessor.getUploadDate()<=0) {
	        			oProcessor.setUploadDate((double) oNow.getTime());
	        			bUpdate = true;
	        		}
	        		
	        		
	        		if (oProcessor.getUpdateDate() == null) {
						oProcessor.setUpdateDate((double) oNow.getTime());
						bUpdate = true;	        			
	        		}
	        		else if (oProcessor.getUpdateDate()<=0) {
						oProcessor.setUpdateDate((double) oNow.getTime());
						bUpdate = true;
					}
					
					if (bUpdate) {
						System.out.println("Updating " + oProcessor.getName());
						oProcessorRepository.updateProcessor(oProcessor);
					}
				}
	        	
	        	System.out.println("Processors Update Done");
	        }
		}
		catch (Exception oEx) {
			System.out.println("processors Exception: " + oEx);
			oEx.printStackTrace();
		}
	}
	
	
	public static void metadata() {
		
		try {
			
	        System.out.println("Ok, what we do with metadata?");
	        
	        System.out.println("\t1 - Clear Unlinked metadata");
	        System.out.println("\tx - back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();

	        if (sInputString.equals("x")) {
	        	return;
	        }

	        if (sInputString.equals("1")) {
	        	
	        	System.out.println("Searching Metadata files to delete");
	        	
	        	File oMetadataPath = new File("/data/wasdi/metadata");
	        	
	        	WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
	        	// Get all the downloaded files
	        	DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
	        	List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getList();
	        	
	        	ArrayList<String> asMetadataFileReference = new ArrayList<String>();
	        	
	        	// Generate the list of valid metadata file reference
	        	for (DownloadedFile oDownloadedFile : aoDownloadedFileList) {
	        		
	        		if (!isProductOnThisNode(oDownloadedFile, oWorkspaceRepository)) {
	        			System.out.println("Product " + oDownloadedFile.getFileName() + " NOT IN THIS NODE JUMP");
	        			continue;
	        		}
	        		
	        		// Get the view model
	        		ProductViewModel oVM = oDownloadedFile.getProductViewModel();
	        		
	        		if (oVM != null) {
	        			if (!Utils.isNullOrEmpty(oVM.getMetadataFileReference())){
	        				// Check metadata file refernece
	        				if (!asMetadataFileReference.contains(oVM.getMetadataFileReference())) {
	        					// Add to the list
	        					asMetadataFileReference.add(oVM.getMetadataFileReference());
	        				}
	        			}
	        		}
				}
	        	
	        	// Files to delete
	        	ArrayList<File> aoFilesToDelete = new ArrayList<File>();
	        	
	        	// For all the files in metadata
	        	for (File oFile : oMetadataPath.listFiles()) {
	        		
	        		// Get the name:
	        		String sName = oFile.getName();
	        		
	        		// Is linked?
	        		if (!asMetadataFileReference.contains(sName)) {
	        			// No!!
	        			aoFilesToDelete.add(oFile);
	        		}
	        	}
	        	
	        	
	        	for (File oFile : aoFilesToDelete) {
	        		System.out.println("Deleting metadata File: " + oFile.getPath());
	        		if (oFile.delete()==false) {
	        			System.out.println("Error Deleting metadata File: " + oFile.getPath());
	        		}
				}
      	
	        }
		}
		catch (Exception oEx) {
			System.out.println("metadata Exception: " + oEx);
			oEx.printStackTrace();
		}
	}

	
	private static void password() {
		try {
			
	        System.out.println("Ok, what we do with Password?");
	        
	        System.out.println("\t1 - Encrypt Password");
	        System.out.println("\t2 - Force Update User Password");
	        System.out.println("\tx - back");
	        System.out.println("");

	        String sInputString = s_oScanner.nextLine();

	        if (sInputString.equals("x")) {
	        	return;
	        }
	        
	        PasswordAuthentication oAuth = new PasswordAuthentication();

	        if (sInputString.equals("1")) {
	        	
	        	System.out.println("Insert the password to Encrypt:");
	        	String sInputPw = s_oScanner.nextLine();
	        	
	    		String sToChanget = oAuth.hash(sInputPw.toCharArray());
	    		System.out.println("Encrypted Password:");
	    		System.out.println(sToChanget);
      	
	        }
	        else if (sInputString.equals("2")) {

	        	System.out.println("Insert the user Id:");
	        	String sUserId = s_oScanner.nextLine();

	        	System.out.println("Insert the password to Encrypt:");
	        	String sInputPw = s_oScanner.nextLine();
	        	
	        	UserRepository oUserRepo = new UserRepository();
	        	User oUser = oUserRepo.getUser(sUserId);
	        	
	        	if (oUser == null) {
	        		System.out.println("User [" + sUserId + "] not found");
	        		return;
	        	}
	        	
	        	oUser.setPassword(oAuth.hash(sInputPw.toCharArray()));
	        	
	        	oUserRepo.updateUser(oUser);
	        	
	        	System.out.println("Update password for user [" + sUserId + "]");

	        }
		}
		catch (Exception oEx) {
			System.out.println("password Exception: " + oEx);
			oEx.printStackTrace();
		}
	}
	
	private static void workflows () {
		try {
			
	        System.out.println("Ok, what we do with workflows?");
	        
	        System.out.println("\t1 - Copy workflows from user folder to generic folder");
	        System.out.println("\tx - back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();
	        
	        if (sInputString.equals("x")) {
	        	return;
	        }	        

	        if (sInputString.equals("1")) {
	        	
	        	System.out.println("Getting workflows");
	        	
	        	SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
	        	List<SnapWorkflow> aoWorkflows = oSnapWorkflowRepository.getList();
	        	
	    		String sBasePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
	    		if (!sBasePath.endsWith("/")) {
	    			sBasePath += "/";
	    		}
	    		sBasePath += "workflows/";
	    		
	    		File oDestinationPath = new File(sBasePath);
	    		
	    		if (!oDestinationPath.exists()) {
	    			oDestinationPath.mkdirs();
	    		}
	        	
	        	// Search one by one
	        	for (SnapWorkflow oWorkflow : aoWorkflows) {
	        		
	        		String sWorkflowPath = sBasePath + oWorkflow.getWorkflowId() + ".xml";
	        		
	        		File oOriginalFile = new File(sWorkflowPath);
	        		File oDestinationFile = new File(oDestinationPath, oOriginalFile.getName());
	        		
	        		if (!oDestinationFile.exists()) {
	        			System.out.println("File does not exists, make a copy [" + oDestinationFile.getPath() + "]");
	        			
	        			try {
		        			FileUtils.copyFileToDirectory(oOriginalFile, oDestinationPath);
		        			
		        			oWorkflow.setFilePath(oDestinationFile.getPath());
		        			oSnapWorkflowRepository.updateSnapWorkflow(oWorkflow);	        				
	        			}
	        			catch (Exception oEx) {
	        				System.out.println("File Copy Exception: " + oEx);
	        				oEx.printStackTrace();
						}
	        		}
	        		else {
	        			System.out.println("File already exists, jump");
	        		}
				}
	        	
	        	System.out.println("All workflows copied");
	        }

		}
		catch (Exception oEx) {
			System.out.println("Workflows Exception: " + oEx);
			oEx.printStackTrace();
		}		
	}
	
	private static void users() {
		try {
			
	        System.out.println("Ok, what we do with Users?");
	        
	        System.out.println("\t1 - Delete User");
	        System.out.println("\t2 - Print User Mails");
	        System.out.println("\tx - back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();

	        if (sInputString.equals("x")) {
	        	return;
	        }
	        if (sInputString.equals("1")) {
	        	
	        	System.out.println("Insert the userId to Delete:");
	        	String sUserId = s_oScanner.nextLine();
	        	
	        	if (Utils.isNullOrEmpty(sUserId)) {
	        		System.out.println("User Id is null or empty");
	        		return;
	        	}
	        	
	        	UserRepository oUserRepo = new UserRepository();
	        	User oTestUser = oUserRepo.getUser(sUserId);
	        	
	        	if (oTestUser == null) {
	        		System.out.println("User Id not valid");
	        		return;	        		
	        	}
	        	
	        	// Get all the workspaces
	        	WorkspaceRepository oWorkspaceRepo = new WorkspaceRepository();
	        	List<Workspace> aoWorkspaces = oWorkspaceRepo.getWorkspaceByUser(sUserId);
	        	
	        	// Delete one by one
	        	for (Workspace oWorkspace : aoWorkspaces) {
	        		deleteWorkspace(oWorkspace.getWorkspaceId(), oWorkspace.getUserId());
				}
	        	
	        	// Clean the log/processing history
	        	ProcessWorkspaceRepository oProcWsRepo = new ProcessWorkspaceRepository();
	        	ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
	        	
	        	List<ProcessWorkspace> aoProcWs = oProcWsRepo.getProcessByUser(sUserId);
	        	
	        	System.out.println("Deleting Process Workpsaces and Logs : " + aoProcWs.size());
	        	
	        	for (ProcessWorkspace oProcWorkspace : aoProcWs) {
	        		String sProcId = oProcWorkspace.getProcessObjId();
	        		
	        		oProcessorLogRepository.deleteLogsByProcessWorkspaceId(sProcId);
	        		oProcWsRepo.deleteProcessWorkspaceByProcessObjId(sProcId);
				}
	        	
	        	// Clean the user table
	        	System.out.println("Deleting User Db Entry ");
	        	
	        	UserRepository oUserRepository = new UserRepository();
	        	oUserRepository.deleteUser(sUserId);
	        	
	        	// Clean the user folder
	        	System.out.println("Deleting User Folder ");
	        	
	    		String sBasePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
	    		if (!sBasePath.endsWith("/")) {
	    			sBasePath += "/";
	    		}
	    		sBasePath +=sUserId;
	    		sBasePath += "/";
	    		
				FileUtils.deleteDirectory(new File(sBasePath));
	        	
	        }
	        else if (sInputString.equals("2")) {
	        	UserRepository oUserRepo = new  UserRepository();
	        	ArrayList<User> aoUsers = oUserRepo.getAllUsers();
	        	
	        	for (User oUser : aoUsers) {
					System.out.println(oUser.getUserId());
				}
	        }
		}
		catch (Exception oEx) {
			System.out.println("USERS Exception: " + oEx);
			oEx.printStackTrace();
		}
	}
	
	private static String getWorkspacePath(String sWorkspaceOwner,String  sWorkspaceId) throws IOException {
		String sBasePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
		if (!sBasePath.endsWith("/")) {
			sBasePath += "/";
		}
		sBasePath +=sWorkspaceOwner;
		sBasePath += "/";
		sBasePath += sWorkspaceId;
		sBasePath += "/";
		
		return sBasePath;
	}
	
	private static void deleteWorkspace(String sWorkspaceId, String sWorkspaceOwner) {
		
		try {
			// repositories
			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			PublishedBandsRepository oPublishRepository = new PublishedBandsRepository();
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			
			if (oWorkspace.getNodeCode().equals(s_sMyNodeCode) == false) {
				System.out.println("Workspace " + sWorkspaceId + " of user " + sWorkspaceOwner + " IS IN NODE " + oWorkspace.getNodeCode());
				return;
			}
			
			// get workspace path
			String sWorkspacePath = getWorkspacePath(sWorkspaceOwner, sWorkspaceId);
			
			System.out.println("deleting Workspace " + sWorkspaceId + " of user " + sWorkspaceOwner);

			// Delete Workspace Db Entry
			if (oWorkspaceRepository.deleteWorkspace(sWorkspaceId)) {
				
				// Get all Products in workspace
				List<ProductWorkspace> aoProductsWorkspaces = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

				Utils.debugLog("Deleting workspace layers");

				// GeoServer Manager Object
				GeoServerManager oGeoServerManager = new GeoServerManager(ConfigReader.getPropValue("GS_URL"), ConfigReader.getPropValue("GS_USER"), ConfigReader.getPropValue("GS_PASSWORD"));
				
				// For each product in the workspace
				for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {
					
					// Get the downloaded file
					DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(oProductWorkspace.getProductName());
					
					// Is the product used also in other workspaces?
					List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(oDownloadedFile.getFileName());
					
					if (aoDownloadedFileList.size()>1) {
						// Yes, it is in other Ws, jump
						Utils.debugLog("The file is also in other workspaces, leave the bands as they are");
						continue;
					}
					
					// We need the View Model product name: start from file name
					String sProductName = oDownloadedFile.getFileName();
					
					// If view model is available (should be), get the name from the view model
					if (oDownloadedFile.getProductViewModel() != null) {
						sProductName = oDownloadedFile.getProductViewModel().getName();
					}
					
					// Get the list of published bands by product name
					List<PublishedBand> aoPublishedBands = oPublishRepository.getPublishedBandsByProductName(sProductName);
					
					// For each published band
					for (PublishedBand oPublishedBand : aoPublishedBands) {
						
						try {
							// Remove Geoserver layer (and file)
							if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
								Utils.debugLog("error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
							}

							try {									
								// delete published band on database
								oPublishRepository.deleteByProductNameLayerId(oDownloadedFile.getProductViewModel().getName(), oPublishedBand.getLayerId());
							} 
							catch (Exception oEx) {
								Utils.debugLog("error deleting published band on data base " + oEx.toString());}

						} catch (Exception oEx) {
							Utils.debugLog("error deleting layer id " + oEx.toString());
						}

					}
				}			
				
				try {

					Utils.debugLog("Delete workspace folder " + sWorkspacePath);
					
					// delete directory
					FileUtils.deleteDirectory(new File(sWorkspacePath));
					
					// delete download file on database
					for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {
						
						try {
							
							Utils.debugLog("Deleting file " + oProductWorkspace.getProductName());
							oDownloadedFilesRepository.deleteByFilePath(oProductWorkspace.getProductName());
							
						} 
						catch (Exception oEx) {
							Utils.debugLog( "Error deleting download on data base: " + oEx);
						}
					}

				} catch (Exception oEx) {
					Utils.debugLog("Error deleting workspace directory: " + oEx);
				}
				
				// Delete Product Workspace entry 
				oProductWorkspaceRepository.deleteByWorkspaceId(sWorkspaceId);
				
				// Delete also the sharings, it is deleted by the owner..
				WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
				oWorkspaceSharingRepository.deleteByWorkspaceId(sWorkspaceId);

			} 
			else {
				Utils.debugLog("Error deleting workspace on data base");
			}

		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.DeleteWorkspace: " + oEx);
		}		
		
	}
	
	public static void sample() {
		System.out.println("sample method running");
	}
	
	private static void refreshProductsTable() {
		DownloadedFilesRepository oDownloadedFileRepo = new DownloadedFilesRepository();
		
		List<DownloadedFile> aoProducts = oDownloadedFileRepo.getList();
		
		for (DownloadedFile oProduct : aoProducts) {
			
			if (oProduct.getFileName().startsWith("S1") || oProduct.getFileName().startsWith("S2")) {
				ProductViewModel oVM = oProduct.getProductViewModel();
				
				List<BandViewModel> aoBands = oVM.getBandsGroups().getBands();
				
				if (aoBands== null) continue;
				
				boolean bChanged = false;
				for (BandViewModel oBand : aoBands) {
					if (oBand.getPublished() == true) {
						oBand.setPublished(false);
						bChanged = true;
					}
				}
				
				if (bChanged) {
					oDownloadedFileRepo.updateDownloadedFile(oProduct);
				}
			}
			
			
		}
	}

	private static void cleanPublishedBands() throws MalformedURLException, IOException {
		
		try {
			GeoServerManager oGeoServerManager = new GeoServerManager(ConfigReader.getPropValue("GEOSERVER_ADDRESS"), ConfigReader.getPropValue("GEOSERVER_USER"), ConfigReader.getPropValue("GEOSERVER_PASSWORD"));

			PublishedBandsRepository oPublishedBandRepo = new PublishedBandsRepository();
			
			List<PublishedBand> aoBands = oPublishedBandRepo.getList();
			
			for (PublishedBand oPublishedBand : aoBands) {
				
				if (oPublishedBand.getProductName().startsWith("S1") || oPublishedBand.getProductName().startsWith("S2")) {
					
					System.out.println("DELETE " + oPublishedBand.getLayerId());
					
					if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
						System.out.println("ProductResource.DeleteProduct: error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
					}

					try {
						// delete published band on data base
						oPublishedBandRepo.deleteByProductNameLayerId(oPublishedBand.getProductName(), oPublishedBand.getLayerId());
					} catch (Exception oEx) {
						System.out.println( "ProductResource.DeleteProduct: error deleting published band on data base " + oEx);
					}				
				}
				else {
					System.out.println("KEEP " + oPublishedBand.getLayerId());
				}
			}		
		}
		catch (Exception e) {
			System.out.println( "ProductResource.DeleteProduct: error deleting published band on data base " + e);
		}
		

		refreshProductsTable();
	}
	
	private static void workspaces() {
		try {
			
	        System.out.println("Ok, what we do with workspaces?");
	        
	        System.out.println("\t1 - Clean shared ws errors");
	        System.out.println("\t2 - Move Workpsace to new node");
	        System.out.println("\tx - back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();
	        
	        if (sInputString.equals("x")) {
	        	return;
	        }	        

	        if (sInputString.equals("1")) {
	        	
	        	System.out.println("Getting workspace sharings");
	        	
	        	WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
	        	WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
	        	
	        	List<WorkspaceSharing> aoWorkspacesSharings = oWorkspaceSharingRepository.getWorkspaceSharings();
	        	
	        	for (WorkspaceSharing oWorkspaceSharing : aoWorkspacesSharings) {
	        		
					Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oWorkspaceSharing.getWorkspaceId());

					if (oWorkspace == null) {
						Utils.debugLog("WorkspaceSharings: DELETE WS Shared not available " + oWorkspaceSharing.getWorkspaceId());
						
						oWorkspaceSharingRepository.deleteByUserIdWorkspaceId(oWorkspaceSharing.getUserId(), oWorkspaceSharing.getWorkspaceId());
						continue;
					}	        		
				}

	        	System.out.println("All workspace sharings cleaned");
	        }
	        else if (sInputString.equals("2")) {
	        	// Not easy to use...
	        	System.out.println("NOTE: feature only updates the DB, you must before backup the tables from the old node and import in the new one");
	        	
	        	// Insert WS and destination node
	        	System.out.println("Please Insert the WS ID");
	        	String sWorkspaceId = s_oScanner.nextLine();
	        	System.out.println("Please Insert the new node code");
	        	String sNewNodeCode = s_oScanner.nextLine();
	        	
	        	// Find and open the workspace
	        	WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
	        	Workspace oWorkpsace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
	        	
	        	if (oWorkpsace == null) {
	        		System.out.println("Impossible to find the WS: " + sWorkspaceId);
	        		return;
	        	}
	        	
	        	if (oWorkpsace.getNodeCode().equals(sNewNodeCode)) {
	        		System.out.println("The WS: " + sWorkspaceId + " is already in the node " + sNewNodeCode);
	        		return;
	        	}
	        	
	        	NodeRepository oNodeRepository = new NodeRepository();
	        	Node oNode = oNodeRepository.getNodeByCode(sNewNodeCode);

	        	if (oNode == null) {
	        		System.out.println("Impossible to find the Node: " + sNewNodeCode);
	        		return;
	        	}

	        	// Get all the process workspace to migrate
	        	ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
	        	List<ProcessWorkspace> aoProcessesInOldNode = oProcessWorkspaceRepository.getProcessByWorkspace(oWorkpsace.getWorkspaceId());
	        	
	        	System.out.println("Found " + aoProcessesInOldNode.size() + " process workspace.");
	        		        	
	        	for (ProcessWorkspace oProcessWorkpsace : aoProcessesInOldNode) {
	        		
	        		if (oProcessWorkpsace.getNodeCode().equals(sNewNodeCode) == false) {
	        			System.out.println("Updating Process Workspace " + oProcessWorkpsace.getProcessObjId());
		        		oProcessWorkpsace.setNodeCode(sNewNodeCode);
		        		oProcessWorkspaceRepository.updateProcess(oProcessWorkpsace);	        			
	        		}
				}
	        		        	
	        	System.out.println("Updating Workpsace.");
	        	oWorkpsace.setNodeCode(sNewNodeCode);
	        	oWorkspaceRepository.updateWorkspace(oWorkpsace);
	        	
	        	System.out.println("Update done");	        	
	        }
		}
		catch (Exception oEx) {
			System.out.println("Workspace Sharing Exception: " + oEx);
			oEx.printStackTrace();
		}				
	}
	
	/*
	 *
	 */
	public static void migrateToLocal() {

		try {
			
	        System.out.println("Ok, what do we migrate?");
	        
	        System.out.println("\t1 - Copy Process Workspace of this Node in the local Database");
	        System.out.println("\tx - back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();
	        
	        if (sInputString.equals("x")) {
	        	return;
	        }	        

	        if (sInputString.equals("1")) {
	        	
	    		//connect to main DB
	    		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
	    		oProcessWorkspaceRepository.setRepoDb("wasdi");
	    		
	    		//get processWorkspaces on local node from main DB 
	    		String sNodeCode = s_sMyNodeCode;
	    		
	    		List<ProcessWorkspace> aoProcessesToBePorted = oProcessWorkspaceRepository.getByNode(sNodeCode);
	    		
	    		System.out.println("Got " + aoProcessesToBePorted.size() + " processes to migrate");
	    		
	    		ArrayList<String> asIds = new ArrayList<String>();
	    		
	    		for (ProcessWorkspace oProcessWS : aoProcessesToBePorted) {
	    			asIds.add(oProcessWS.getProcessObjId());
	    		}	    		
	    		
	    		System.out.println("Start logs search ");
	    		
	    		// Find and save corresponding logs 
	    		ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
	    		oProcessorLogRepository.setRepoDb("wasdi");
	    		
	    		List<ProcessorLog> aoLogsToBePorted = new ArrayList<>();
	    		
	    		aoLogsToBePorted = oProcessorLogRepository.getLogsByArrayProcessWorkspaceId(asIds);
	    		
	    		// Switch to local db
	    		oProcessWorkspaceRepository.setRepoDb("local");
	    		oProcessorLogRepository.setRepoDb("local");
	    		
	    		//insert processes
	    		oProcessWorkspaceRepository.insertProcessListWorkspace(aoProcessesToBePorted);
	    		
	    		//insert logs
	    		oProcessorLogRepository.insertProcessLogList(aoLogsToBePorted);
	    		
	    		System.out.println("Do you want to delete entries from the central database? (1=YES, 2=NO)");
	    		
	    		String sDeleteEntries = s_oScanner.nextLine();
	    		
	    		if (sDeleteEntries.equals("1")) {
	    			System.out.println("Setting Repositories to main db");
		    		oProcessWorkspaceRepository.setRepoDb("wasdi");
		    		oProcessorLogRepository.setRepoDb("wasdi");
		    		System.out.println("Deleting logs");
		    		
		    		for (ProcessWorkspace oProcessWorkspace : aoProcessesToBePorted) {
						
		    			oProcessorLogRepository.deleteLogsByProcessWorkspaceId(oProcessWorkspace.getProcessObjId());
		    			oProcessWorkspaceRepository.deleteProcessWorkspaceByProcessObjId(oProcessWorkspace.getProcessObjId());
					}
		    		
		    		System.out.println("Deleting process workspaces");
		    		
	    		}
	        }

		}
		catch (Exception oEx) {
			System.out.println("Migrate Exception: " + oEx);
			oEx.printStackTrace();
		}			
	}
	
	/**
	 * Works with process workspaces
	 */
	public static void processWorkpsaces() {

		try {
			
	        System.out.println("So you want to work with process workspaces?");
	        
	        System.out.println("\t1 - Delete ProcessWorkspace where Workspace does not exist");
	        System.out.println("\tx - back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();
	        
	        if (sInputString.equals("x")) {
	        	return;
	        }	        

	        if (sInputString.equals("1")) {
	        	
	        	System.out.println("Preparing Valid Workspace Id List");
	        	
	        	// Get the list of workspaces
	        	WorkspaceRepository oWorkspaceRepo = new WorkspaceRepository();
	        	
	        	List<Workspace> aoWorkspaces = oWorkspaceRepo.getWorkspacesList();
	        	ArrayList<String> asWorkspacesId = new ArrayList<String>();
	        	
	        	for (Workspace oWorkspace : aoWorkspaces) {
					asWorkspacesId.add(oWorkspace.getWorkspaceId());
				}
	        	
	        	System.out.println("Query ProcessWorkspaces");
	        	
	    		// Get the total list of process workspaces
	    		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();	    		
	    		List<ProcessWorkspace> aoProcessWorkspaces = oProcessWorkspaceRepository.getList();
	    		
	    		System.out.println("Got " + aoProcessWorkspaces.size() + " to analyze");
	    		
	    		// Will be used later to clean logs
	    		ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
	    		
	    		// For each one
	    		for (ProcessWorkspace oProcessWS : aoProcessWorkspaces) {
	    			
	    			// Is there the workspace?
	    			if (!asWorkspacesId.contains(oProcessWS.getWorkspaceId())) {
	    				
	    				// No, delete it
	    				String sProcWSId = oProcessWS.getProcessObjId();
	    				System.out.println("Deleting " + sProcWSId + " in not existing workspace " + oProcessWS.getWorkspaceId());
	    				
	    				oProcessWorkspaceRepository.deleteProcessWorkspaceByProcessObjId(sProcWSId);
	    				
	    				// It has logs?
	    				if (oProcessWS.getOperationType().equals("RUNPROCESSOR") || oProcessWS.getOperationType().equals("RUNIDL") || oProcessWS.getOperationType().equals("RUNMATLAB")) {
	    					// Maybe yes, delete logs
	    					System.out.println("Deleting also logs of " + sProcWSId);
	    					oProcessorLogRepository.deleteLogsByProcessWorkspaceId(sProcWSId);
	    				}
	    			}
	    		}
	    		
	    		System.out.println("Clean done!! Bye");
	        }

		}
		catch (Exception oEx) {
			System.out.println("processWorkpsaces Exception: " + oEx);
			oEx.printStackTrace();
		}			
	}
	
	/**
	 * Works with process workspaces
	 */
	public static void logs() {

		try {
			
	        System.out.println("What we do with our logs?");
	        
	        System.out.println("\t1 - Extract Logs");
	        System.out.println("\t2 - Clean Logs with non existing Process Workspace");
	        System.out.println("\t3 - Clean old logs");
	        System.out.println("\tx - back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();
	        
	        if (sInputString.equals("x")) {
	        	return;
	        }	        

	        if (sInputString.equals("1")) {
	        	System.out.println("Well, ok, but this feature is Main Menu -> 3 Processors -> 1 - Extract Log. Go there!");
	        }
	        else if (sInputString.equals("2")) {
	        	
	        	System.out.println("This will take more than a while, let's start with a list of valid local process workspaces");
	        	
	    		// Get the total list of process workspaces
	    		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();	    		
	    		List<ProcessWorkspace> aoProcessWorkspaces = oProcessWorkspaceRepository.getList();
	    		
	    		System.out.println("Got " + aoProcessWorkspaces.size() + " to analyze");
	    		
	        	ArrayList<String> asProcWorkspacesId = new ArrayList<String>();
	        	
	        	for (ProcessWorkspace oProcWorkspace : aoProcessWorkspaces) {
					asProcWorkspacesId.add(oProcWorkspace.getProcessObjId());
				}

	        	ArrayList<String> asAlreadyCleanedProcessWorkspace = new ArrayList<String>();
	    		
	    		// Will be used later to clean logs
	    		ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
	    		
	    		List<ProcessorLog> aoAllTheLogs = oProcessorLogRepository.getList();
	    		
	    		// For each one
	    		for (ProcessorLog oLog : aoAllTheLogs) {
	    			
	    			// Is there the workspace?
	    			if (!asProcWorkspacesId.contains(oLog.getProcessWorkspaceId())) {
	    				
	    				if (!asAlreadyCleanedProcessWorkspace.contains(oLog.getProcessWorkspaceId())) {
		    				// No, delete it
		    				System.out.println("Deleting all log rows of ProcessWorkspace " + oLog.getProcessWorkspaceId());
		    				
		    				oProcessorLogRepository.deleteLogsByProcessWorkspaceId(oLog.getProcessWorkspaceId());
		    				
		    				asAlreadyCleanedProcessWorkspace.add(oLog.getProcessWorkspaceId());
	    				}
	    				
	    			}
	    		}
	    		
	    		System.out.println("Clean done!! Bye");
	        }
	        else if (sInputString.equals("3")) {
	        	System.out.println("Please insert upper bound date in format YYYY-MM-DD:");
	        	
	        	String sDate= s_oScanner.nextLine();
	        	
	        	if (Utils.isNullOrEmpty(sDate)) {
	        		System.out.println("not valid date " + sDate);
	        		return;
	        	}
	        	
	        	String []asSplit = sDate.split("-");
	        	
	        	if (asSplit == null) {
	        		System.out.println("not valid date " + sDate);
	        		return;
	        	}
	        	
	        	if (asSplit.length != 3) {
	        		System.out.println("not valid date " + sDate);
	        		return;
	        	}
	        	
	        	//{"$lt":"2019-05-04 00:00:00"}})
	        	ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
	        	oProcessorLogRepository.deleteLogsOlderThan(sDate);
	        	
	        	System.out.println("Logs cleaned!");
	        }

		}
		catch (Exception oEx) {
			System.out.println("logs Exception: " + oEx);
			oEx.printStackTrace();
		}			
	}		
	
	/*
	 *
	 */
	public static void categories() {

		try {
			
	        System.out.println("So, you want to work with categories?");
	        
	        System.out.println("\t1 - List Categories");
	        System.out.println("\t2 - Add a new Category");
	        System.out.println("\t3 - Delete Categories");
	        System.out.println("\tx - back");
	        System.out.println("");
	        
	        String sInputString = s_oScanner.nextLine();
	        
	        if (sInputString.equals("x")) {
	        	return;
	        }	        

	        if (sInputString.equals("1")) {
	        	
	    		//connect to main DB
	    		AppsCategoriesRepository oAppsCategoriesRepository = new AppsCategoriesRepository();
	    		
	    		List<AppCategory> aoCategories = oAppsCategoriesRepository.getCategories();
	    		
	    		for (AppCategory oCategory : aoCategories) {
					System.out.println("ID: ["+oCategory.getId()+"]: " + oCategory.getCategory());
				}
	    		
	    		System.out.println("Printed " + aoCategories.size() + " Categories");
	        }
	        else if (sInputString.equals("2")) {
	        	System.out.println("Insert Category Name:");
	        	String sCategory = s_oScanner.nextLine();
	        	AppCategory oAppCategory = new AppCategory();
	        	oAppCategory.setCategory(sCategory);
	        	oAppCategory.setId(Utils.GetRandomName());
	        	
	        	AppsCategoriesRepository oAppsCategoriesRepository = new AppsCategoriesRepository();
	        	oAppsCategoriesRepository.insertCategory(oAppCategory);
	        	
	        	System.out.println("Category Created with ID: " + oAppCategory.getId());
	        }
	        else if (sInputString.equals("3")) {
	        	System.out.println("Insert Id Of the category to delete:");
	        	String sCategoryId = s_oScanner.nextLine();
	        	
	        	AppsCategoriesRepository oAppsCategoriesRepository = new AppsCategoriesRepository();
	        	AppCategory oCategory = oAppsCategoriesRepository.getCategoryById(sCategoryId);
	        	
	        	if (oCategory == null) {
	        		System.out.println("Category NOT FOUND with ID: " + sCategoryId);
	        	}
	        	else {
	        		System.out.println("Category FOUND: " + oCategory.getCategory());
	        		
	        		oAppsCategoriesRepository.deleteCategory(sCategoryId);
	        		
	        		ProcessorRepository oProcessorRepository = new ProcessorRepository();
	        		List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();
	        		
	        		for (Processor oProcessor : aoProcessors) {
						
	        			if (oProcessor.getCategories() != null) {
	        				if (oProcessor.getCategories().size()>0) {
	        					if (oProcessor.getCategories().contains(sCategoryId)) {
	        						oProcessor.getCategories().remove(sCategoryId);
	        						oProcessorRepository.updateProcessor(oProcessor);
	        						System.out.println("Category removed from processor " + oProcessor.getName() + " ID: " + oProcessor.getProcessorId());
	        					}
	        				}
	        			}
					}
	        		
	        		System.out.println("Category deleted: " + sCategoryId);
	        	}
	        	
	        }	        

		}
		catch (Exception oEx) {
			System.out.println("Categories Exception: " + oEx);
			oEx.printStackTrace();
		}
	}	
	
	public static String s_sMyNodeCode = "wasdi";
	private static Scanner s_oScanner;
		
	public static void main(String[] args) {
		
        try {
        	//this is how you read parameters:
			MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
	        MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
	        MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
	        MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
	        MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
	        
	        String sNode = ConfigReader.getPropValue("NODECODE");
	        if (!Utils.isNullOrEmpty(sNode)) {
	        	s_sMyNodeCode = sNode;
	        }
	        
			try {
				// get jar directory
				File oCurrentFile = new File(
						dbUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				// configure log
				String sThisFilePath = oCurrentFile.getParentFile().getPath();
				DOMConfigurator.configure(sThisFilePath + "/log4j.xml");

			} catch (Exception exp) {
				// no log4j configuration
				System.err.println("DbUtils - Error loading log configuration.  Reason: "
						+ org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exp));
			}
	        
			// If this is not the main node
			if (!s_sMyNodeCode.equals("wasdi")) {
				System.out.println("Adding local mongo config");
				// Configure also the local connection: by default is the "wasdi" port + 1
				MongoRepository.addMongoConnection("local", MongoRepository.DB_USER, MongoRepository.DB_PWD, MongoRepository.SERVER_ADDRESS, MongoRepository.SERVER_PORT+1, MongoRepository.DB_NAME);				
			}
			
	        boolean bExit = false;
	        
	        s_oScanner = new Scanner( System.in);
	        
	        while (!bExit) {
		        System.out.println("---- WASDI db Utils ----");
		        System.out.println("Welcome, how can I help you?");
		        
		        System.out.println("\t1 - Downloaded Products");
		        System.out.println("\t2 - Product Workspace");
		        System.out.println("\t3 - Processors");
		        System.out.println("\t4 - Metadata");
		        System.out.println("\t5 - Password");
		        System.out.println("\t6 - Users");
		        System.out.println("\t7 - Workflows");
		        System.out.println("\t8 - Workspaces");
		        System.out.println("\t9 - Migrate DB to local");
		        System.out.println("\t10 - Categories");
		        System.out.println("\t11 - ProcessWorkspace");
		        System.out.println("\t12 - Logs");
		        System.out.println("\tx - Exit");
		        System.out.println("");
		        
		        
		        String sInputString = s_oScanner.nextLine();
		        
		        if (sInputString.equals("1")) {
		        	downloadedProducts();
		        }
		        else if (sInputString.equals("2")) {
		        	productWorkspace();
		        }		        
		        else if (sInputString.equals("3")) {
		        	processors();
		        }		        
		        else if (sInputString.equals("4")) {
		        	metadata();
		        }		       
		        else if (sInputString.equals("5")) {
		        	password();
		        }
		        else if (sInputString.equals("6")) {
		        	users();
		        }
		        else if (sInputString.equals("7")) {
		        	workflows();
		        }
		        else if (sInputString.equals("8")) {
		        	workspaces();
		        }		        
		        else if(sInputString.equals("9")) {
		        	migrateToLocal();
		        }
		        else if(sInputString.equals("10")) {
		        	categories();
		        }
		        else if(sInputString.equals("11")) {
		        	processWorkpsaces();
		        }
		        else if(sInputString.equals("12")) {
		        	logs();
		        }		        
		        else if (sInputString.toLowerCase().equals("x")) {
		        	bExit = true;
		        }		        
		        else {
		        	System.out.println("Please select a valid option or x to exit");
		        	System.out.println("");
		        	System.out.println("");
		        }
	        }
	        
	        
	        System.out.println("bye bye");
	        
	        s_oScanner.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



}

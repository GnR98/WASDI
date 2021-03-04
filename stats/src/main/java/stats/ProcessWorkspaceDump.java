/**
 * 
 */
package stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class ProcessWorkspaceDump {

	static public String s_sNode;
	static public String s_sOutfile = "processWorkspace.dump.csv";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//read arguments
			if(args!=null) {
				//node name
				if(args.length > 0 && !Utils.isNullOrEmpty(args[0])) {
					s_sNode = args[0];
				} else {
					s_sNode = "NODE";
				}
				//output file name
				if(args.length > 1 && !Utils.isNullOrEmpty(args[1])) {
					s_sOutfile = args[1];
					if(!s_sOutfile.toLowerCase().endsWith(".csv")) {
						s_sOutfile += ".csv";
					}
				} 
				setUpMongo();
				dumpProcessWorkspace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	/**
	 * @throws IOException
	 */
	public static void setUpMongo() throws IOException {
		MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
		MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
		MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
		MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_COMPUTATIONAL_NODE_PORT"));

	}

	public static void dumpProcessWorkspace() {
		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		List<ProcessWorkspace> aoProcesses = oProcessWorkspaceRepository.getAll();
		System.out.println("ProcessWorkspaceDump.dumpProcessWorkspace: downloaded " + aoProcesses.size() + " processWorkspaces, preparing to dump to file");
		try (FileWriter oWriter = new FileWriter(s_sOutfile)) {
			System.out.println("ProcessWorkspaceDump.dumpProcessWorkspace: FileWriter could be created :-)");
			try (BufferedWriter oBufferedWriter = new BufferedWriter(oWriter)) {
				System.out.println("ProcessWorkspaceDump.dumpProcessWorkspace: BufferedWriter could be created :-)");
				oBufferedWriter.newLine();
				System.out.println("ProcessWorkspaceDump.dumpProcessWorkspace: it is possible towrite to file :-)");
				oBufferedWriter.write("productName,workspaceId,userId,operationType,"
						+ "operationSubType,operationDate,operationStartDate,operationEndDate,"
						+ "lastStateChangeDate,processObjId,fileSize,status,"
						+ "progressPerc,pid,payload,nodeCode,"
						+ "parentId,subprocessPid");
				System.out.println("ProcessWorkspaceDump.dumpProcessWorkspace: header could be written :-)");
				int iDone = 0;
				for (ProcessWorkspace oProcessWorkspace : aoProcesses) {
					try {
						oBufferedWriter.write(oProcessWorkspace.getProductName() + ",");
						oBufferedWriter.write(oProcessWorkspace.getWorkspaceId() + ",");
						oBufferedWriter.write(oProcessWorkspace.getUserId() + ",");
						oBufferedWriter.write(oProcessWorkspace.getOperationType() + ",");
						oBufferedWriter.write(oProcessWorkspace.getOperationSubType() + ",");
						oBufferedWriter.write(oProcessWorkspace.getOperationDate() + ",");
						oBufferedWriter.write(oProcessWorkspace.getOperationStartDate() + ",");
						oBufferedWriter.write(oProcessWorkspace.getOperationEndDate() + ",");
						oBufferedWriter.write(oProcessWorkspace.getLastStateChangeDate() + ",");
						oBufferedWriter.write(oProcessWorkspace.getProcessObjId() + ",");
						oBufferedWriter.write(oProcessWorkspace.getFileSize() + ",");
						oBufferedWriter.write(oProcessWorkspace.getStatus() + ",");
						oBufferedWriter.write(oProcessWorkspace.getProgressPerc() + ",");
						oBufferedWriter.write(oProcessWorkspace.getPid() + ",");
						oBufferedWriter.write(oProcessWorkspace.getPayload() + ",");
						oBufferedWriter.write(oProcessWorkspace.getNodeCode() + ",");
						oBufferedWriter.write(oProcessWorkspace.getParentId() + ",");
						oBufferedWriter.write(oProcessWorkspace.getSubprocessPid() + ",");

						oBufferedWriter.write("\n");
						++iDone;
					} catch (Exception oE) {
						System.err.println("ProcessWorkspaceDump.dumpProcessWorkspace: could not print single line due to: " + oE);
					}
				}
				System.out.println("ProcessWorkspaceDump.dumpProcessWorkspace: written " + iDone + " out " + aoProcesses.size());
				oBufferedWriter.flush();
				oBufferedWriter.close();						
			}
		}
		catch (IOException oE) {
			System.err.println("ProcessWorkspaceDump.dumpProcessWorkspace: outer loop: " + oE);
		}

	}
}


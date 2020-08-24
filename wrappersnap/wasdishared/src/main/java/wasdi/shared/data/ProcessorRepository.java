package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.Processor;

public class ProcessorRepository extends  MongoRepository {
	
	public ProcessorRepository() {
		m_sThisCollection = "processors";
	}
	
	/**
	 * Create a new processor
	 * @param oProcessor Processor Entity
	 * @return True or False in case of exception
	 */
    public boolean insertProcessor(Processor oProcessor) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oProcessor);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Get a processor from the WASDI Id
     * @param sProcessorId WASDI id of the processor
     * @return Entity
     */
    public Processor getProcessor(String sProcessorId) {

        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("processorId", sProcessorId)).first();

            String sJSON = oWSDocument.toJson();

            Processor oProcessor = s_oMapper.readValue(sJSON,Processor.class);

            return oProcessor;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }
    
    /**
     * Get a Processor by Name
     * @param sName Name of the processor
     * @return Processor Entity
     */
    public Processor getProcessorByName(String sName) {

        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("name", sName)).first();

            String sJSON = oWSDocument.toJson();

            Processor oProcessor = s_oMapper.readValue(sJSON,Processor.class);

            return oProcessor;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }

    /**
     * Update a processor
     * @param oProcessor Entity to update
     * @return True or False in case of exception
     */
    public boolean updateProcessor(Processor oProcessor) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oProcessor);
            
            Bson oFilter = new Document("processorId", oProcessor.getProcessorId());
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

            if (oResult.getModifiedCount()==1) return  true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }

    /**
     * Get the processor owned by sUserId
     * @param sUserId Id User of the processor's owner
     * @return List of processors of the user
     */
    public List<Processor> getProcessorByUser(String sUserId) {

        final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    Processor oProcesor = null;
                    try {
                        oProcesor = s_oMapper.readValue(sJSON,Processor.class);
                        aoReturnList.add(oProcesor);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the next http port available for a processor
     * @return New Port. 
     */
    public int getNextProcessorPort() {
    	
    	int iPort = -1;

        try {
        	Document oWSDocument = getCollection(m_sThisCollection).find().sort(new Document("port", -1)).first();
            String sJSON = oWSDocument.toJson();
            Processor oProcessor = s_oMapper.readValue(sJSON,Processor.class);
            iPort = oProcessor.getPort();
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        if (iPort == -1) iPort = 5000;
        else iPort++;
        
        return iPort;
    }
    
    /**
     * Delete a processor from WASDI Id
     * @param sProcessorId Processor Id
     * @return True or false in case of exception
     */
    public boolean deleteProcessor(String sProcessorId) {

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("processorId", sProcessorId));

            if (oDeleteResult != null)
            {
                if (oDeleteResult.getDeletedCount() == 1 )
                {
                    return  true;
                }
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }
    
    /**
     * Delete all the processors of a user
     * @param sUserId Owner of the procs to delete
     * @return Number of deleted processors
     */
    public int deleteProcessorByUser(String sUserId) {

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
    
    /**
     * Get the list of all the deployed processors
     * @return List of all the processors
     */
    public List<Processor> getDeployedProcessors() {
        return getDeployedProcessors("_id");
    }
    
    public List<Processor> getDeployedProcessors(String sOrderBy) {
    	return getDeployedProcessors(sOrderBy, 1);
    }
    
    public List<Processor> getDeployedProcessors(String sOrderBy, int iDirection) {

        final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
        try {

            //FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("port", new Document("$gt", 4999)));
        	FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find().sort(new Document(sOrderBy, iDirection));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    Processor oWorkflow = null;
                    try {
                        oWorkflow = s_oMapper.readValue(sJSON,Processor.class);
                        aoReturnList.add(oWorkflow);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }    

	public void updateProcessorDate(Processor oProcessor){
		Date oDate = new Date();
		oProcessor.setUpdateDate( (double) oDate.getTime());
		updateProcessor(oProcessor);
	}
	
}

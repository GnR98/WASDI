package wasdi.shared.data;

import static com.mongodb.client.model.Filters.eq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.Workspace;
import wasdi.shared.utils.Utils;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceRepository extends  MongoRepository {
	
	public WorkspaceRepository() {
		m_sThisCollection = "workspaces";
	}
	
	/**
	 * Insert a new Workspace
	 * @param oWorkspace
	 * @return
	 */
    public boolean insertWorkspace(Workspace oWorkspace) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkspace);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Update the name of a workpsace
     * @param oWorkspace
     * @return
     */
    public boolean updateWorkspaceName(Workspace oWorkspace) {

        try {
            getCollection(m_sThisCollection).updateOne(eq("workspaceId", oWorkspace.getWorkspaceId()), new Document("$set", new Document("name",oWorkspace.getName())));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Update the node of a workspace
     * @param oWorkspace workspaceViewModel passed as input
     * @return
     */
    public boolean updateWorkspaceNodeCode(Workspace oWorkspace) {

        try {
            getCollection(m_sThisCollection).updateOne(eq("workspaceId", oWorkspace.getWorkspaceId()), new Document("$set", new Document("nodeCode",oWorkspace.getNodeCode())));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }


    /**
     * 
     * @param oWorkspace
     * @return
     */
    public boolean updateWorkspace(Workspace oWorkspace) {

        try {
        	
            String sJSON = s_oMapper.writeValueAsString(oWorkspace);
            
            Bson oFilter = new Document("workspaceId", oWorkspace.getWorkspaceId());
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

            if (oResult.getModifiedCount()==1) return  true;        	

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Get a workspace by Id
     * @param sWorkspaceId
     * @return
     */
    public Workspace getWorkspace(String sWorkspaceId) {

        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("workspaceId", sWorkspaceId)).first();
            
            if (oWSDocument != null) {
                String sJSON = oWSDocument.toJson();

                Workspace oWorkspace = s_oMapper.readValue(sJSON,Workspace.class);

                return oWorkspace;            	
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }

    /**
     * Get all the workspaces of a user
     * @param sUserId
     * @return
     */
    public List<Workspace> getWorkspaceByUser(String sUserId) {

        final ArrayList<Workspace> aoReturnList = new ArrayList<Workspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));
            
            fillList(aoReturnList, oWSDocuments, Workspace.class);
            
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    /**
     * Find a workspace by userId and workspace name.
     * @param sUserId the userId
     * @param sName the name of the workspace
     * @return the first workspace found or null
     */
    public Workspace getByUserIdAndWorkspaceName(String sUserId, String sName) {
    	try {
    		Document oWSDocument = getCollection(m_sThisCollection).find(
    				Filters.and(
    						Filters.eq("userId", sUserId),
    						Filters.eq("name", sName)
    						)
    		).first();
    		
    		if(null!=oWSDocument) {
    			String sJSON = oWSDocument.toJson();
    			
                Workspace oWorkspace = null;
                try {
                    oWorkspace = s_oMapper.readValue(sJSON,Workspace.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                return oWorkspace;
    		}
    		
    	} catch (Exception oE) {
			Utils.debugLog("WorkspaceRepository.getByName( " + sName + "): error: " + oE);
		}
    	
    	return null;
    }

    /**
     * Find a workspace by name and node.
     * @param sName the name of the workspace
     * @param sNode the node
     * @return the first workspace found or null
     */
    public Workspace getByNameAndNode(String sName, String sNode) {
    	try {
    		Document oWSDocument = getCollection(m_sThisCollection).find(
    				Filters.and(
    						Filters.eq("name", sName),
    						Filters.eq("nodeCode", sNode)
    						)
    		).first();
    		
    		if(null!=oWSDocument) {
    			String sJSON = oWSDocument.toJson();
    			
                Workspace oWorkspace = null;
                try {
                    oWorkspace = s_oMapper.readValue(sJSON,Workspace.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                return oWorkspace;
    		}
    		
    	} catch (Exception oE) {
			Utils.debugLog("WorkspaceRepository.getByNameAndNode( " + sName + ", " + sNode + "): error: " + oE);
		}
    	
    	return null;
    }
    
    /**
     * Delete a workspace by Id
     * @param sWorkspaceId
     * @return
     */
    public boolean deleteWorkspace(String sWorkspaceId) {
    	
    	if (Utils.isNullOrEmpty(sWorkspaceId)) return false;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("workspaceId", sWorkspaceId));

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
     * Delete all the workspaces of User
     * @param sUserId
     * @return
     */
    public int deleteByUser(String sUserId) {
    	
    	if (Utils.isNullOrEmpty(sUserId)) return 0;

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
     * Check if User is the owner of Workspace
     * @param sUserId
     * @param sWorkspaceId
     * @return
     */
    public boolean isOwnedByUser(String sUserId, String sWorkspaceId) {
    	try {
	    	Document oWSDocument = getCollection(m_sThisCollection).find(
	        		Filters.and(
	        				Filters.eq("userId", sUserId),
	        				Filters.eq("workspaceId", sWorkspaceId)
	        				)
	        		).first();
	    	if(null!=oWSDocument) {
	    		return true;
	    	}
    	}catch (Exception oE) {
			Utils.debugLog("WorkspaceRepository.belongsToUser( " + sUserId + ", " + sWorkspaceId + " ): error: " + oE);
		}
    	return false;
    }
    
    /**
     * Get all the workspaces
     * @param sUserId
     * @return
     */
    public List<Workspace> getWorkspacesList() {

        final ArrayList<Workspace> aoReturnList = new ArrayList<Workspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
            
            fillList(aoReturnList, oWSDocuments, Workspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }    
}

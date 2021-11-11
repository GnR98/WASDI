package wasdi.shared.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;

/**
 * Base Repository Class
 * Created by p.campanella on 21/10/2016.
 */
public class MongoRepository {
	/**
	 * Db Name
	 */
    public static String DB_NAME = "wasdi";
    /**
     * Server Address
     */
    public static String SERVER_ADDRESS = "127.0.0.1";
    /**
     * Server Port
     */
    public static int SERVER_PORT = 27017;
    /**
     * Db User
     */
    public static String DB_USER = "user";
    /**
     * Db Password
     */
    public static String DB_PWD = "password";

    /**
     * Object Mapper
     */
    public static ObjectMapper s_oMapper = new ObjectMapper();

    static  {
        s_oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    /**
     * Static Connections Dictionary
     */
    private static HashMap<String, MongoConnection> s_aoDbConnections = new HashMap<>();
    
    /**
     * Name of the collection related to this Repository
     */
    protected String m_sThisCollection;
    
    /**
     * Name of the database connected to this repo
     */
    protected String m_sRepoDb = "wasdi";
    
    /**
     * Flag to notify with a log if the repository has no local connection and 
     * reverts to the default wasdi connection
     */
    private static boolean s_bDbSwitchLogged = false;
    
    public static void readConfig() {
        MongoRepository.SERVER_ADDRESS = WasdiConfig.Current.mongo.address;
        MongoRepository.SERVER_PORT = WasdiConfig.Current.mongo.port;
        MongoRepository.DB_NAME = WasdiConfig.Current.mongo.dbName;
        MongoRepository.DB_USER = WasdiConfig.Current.mongo.user;
        MongoRepository.DB_PWD = WasdiConfig.Current.mongo.password;    	
    }
    
	/**
     * Add a new Mongo Connection
     * @param sDbCode Code of the connection
     * @param sUser User 
     * @param sPassword Pw
     * @param sAddress Address 
     * @param iServerPort Port
     * @param sDbName db name
     */
    public static void addMongoConnection(String sDbCode, String sUser, String sPassword, String sAddress, int iServerPort, String sDbName) {
    	try 
    	{
    		// Check if this does not exists yet
    		MongoConnection oMongoConnection = s_aoDbConnections.get(sDbCode);
    		
    		if (oMongoConnection == null) {
    			
    			// Create the connection config
    			oMongoConnection = new MongoConnection();
            	
            	String sConnectionString = "mongodb://" + sUser+":"+sPassword+"@"+sAddress+":"+iServerPort+"/?authSource=" + sDbName;
            	oMongoConnection.m_oMongoClient = MongoClients.create(sConnectionString);
            	oMongoConnection.m_oMongoDatabase = oMongoConnection.m_oMongoClient.getDatabase(sDbName);
            	
            	// Add it to the dictionary
            	s_aoDbConnections.put(sDbCode, oMongoConnection);
            	
            	Utils.debugLog("MongoRepository.addMongoConnection: Configuration added: " + sDbCode);
    		}
    	}
    	catch (Exception e) 
    	{
    		Utils.debugLog("MongoRepository.addMongoConnection: exception " + e.getMessage());
			e.printStackTrace();
    	}    	
    }
    
    /**
     * Get The database Object
     * @return
     */
    public static MongoDatabase getMongoDatabase(String sDbCode) {
    	try 
    	{
    		// Get the connection config from the dictionary
    		MongoConnection oMongoConnection = s_aoDbConnections.get(sDbCode);
    		
    		// Check if exists
    		if (oMongoConnection == null) {
    			// If is the default wasdi and does not exist
    			if (sDbCode.equals("wasdi")) {
    				// Create default connection
        			oMongoConnection = new MongoConnection();
                	String sConnectionString = "mongodb://" + DB_USER+":"+DB_PWD+"@"+SERVER_ADDRESS+":"+SERVER_PORT+"/?authSource=" + DB_NAME;
                	oMongoConnection.m_oMongoClient = MongoClients.create(sConnectionString);
                	oMongoConnection.m_oMongoDatabase = oMongoConnection.m_oMongoClient.getDatabase(DB_NAME);
                	// Add it to the dictionary
                	s_aoDbConnections.put("wasdi", oMongoConnection);
    			}
    			else {
    				
    				if (!s_bDbSwitchLogged) {
    					Utils.debugLog("MongoRepository.getMongoDatabase: Db Code " + sDbCode + " NOT FOUND. try to recover with default db");
    					s_bDbSwitchLogged = true;
    				}
    				
    				
    				// If is not the default one, but does not exists, recover with the defualt
    				return getMongoDatabase("wasdi");
    			}
    		}
    		
    		// Return the database
    		return oMongoConnection.m_oMongoDatabase;
    	}
    	catch (Exception e) 
    	{
    		Utils.debugLog("MongoRepository.getMongoDatabase: exception " + e.getMessage());
			e.printStackTrace();
    	}
        return null;
    }

    /**
     * Get a named collection
     * @param sCollection
     * @return
     */
    public MongoCollection<Document> getCollection(String sCollection) {
    	MongoDatabase oMongoDb = getMongoDatabase(m_sRepoDb);
    	
    	if (oMongoDb != null) {
    		return oMongoDb.getCollection(sCollection);
    	}
    	else {
    		return null;
    	}
    }
    
    /**
     * Shut down the connection
     */
    public static void shutDownConnection() {
    	
    	try {
    		
    		Collection<MongoConnection> aoConnections = s_aoDbConnections.values();
    		
    		for (MongoConnection oConnection : aoConnections) {
        		try {
        			oConnection.m_oMongoClient.close();
        		}
        		catch (Exception e) {
    				Utils.debugLog("MongoRepository.shutDownConnection: exception " + e.getMessage());
    				e.printStackTrace();
    			}
			}
    	}
    	catch (Exception e) {
			Utils.debugLog("MongoRepository.shutDownConnection: exception " + e.getMessage());
			e.printStackTrace();
		}    	
    }
    
    /**
     * Fill a list of entities from the result of a Query.
     * @param <T>
     * @param aoReturnList
     * @param oMongoDocuments
     * @param sCollectionName
     * @param oClass
     */
    public <T> void fillList(List<T> aoReturnList, FindIterable<Document> oMongoDocuments, Class<T> oClass) {
    	
        MongoCursor<Document> oCursor = oMongoDocuments.iterator();
        
        try {
        	while (oCursor.hasNext()) {
                String sJSON = oCursor.next().toJson();
                
                T oEntity = null;
                try {
                	oEntity = s_oMapper.readValue(sJSON,oClass);
                    aoReturnList.add(oEntity);
                } catch (Exception oEx) {
                	Utils.debugLog(m_sThisCollection + ".fillList: " + oEx);
                }            		
        	}
        }
        finally {
			oCursor.close();
		}
	}
    
    public <T> void fillList(HashSet<T> aoReturnList, FindIterable<Document> oMongoDocuments, Class<T> oClass) {
    	
        MongoCursor<Document> oCursor = oMongoDocuments.iterator();
        
        try {
        	while (oCursor.hasNext()) {
                String sJSON = oCursor.next().toJson();
                
                T oEntity = null;
                try {
                	oEntity = s_oMapper.readValue(sJSON,oClass);
                    aoReturnList.add(oEntity);
                } catch (Exception oEx) {
                	Utils.debugLog(m_sThisCollection + ".fillList: " + oEx);
                }            		
        	}
        }
        finally {
			oCursor.close();
		}
	}
    
	public String add(Object oNewDocument, String sCollection, String sRepositoryCommand) {
		String sResult = "";
		if(oNewDocument != null) {
			try {
				String sJSON = s_oMapper.writeValueAsString(oNewDocument);
				Document oDocument = Document.parse(sJSON);
				getCollection(sCollection).insertOne(oDocument);
				sResult = oDocument.getObjectId("_id").toHexString();
	
			} catch (Exception oEx) {
				Utils.debugLog(sRepositoryCommand + ": " + oEx);
			}
		}
		return sResult;
	}
	
	public int delete(BasicDBObject oCriteria, String sCollectionName ){
        try {
            DeleteResult oDeleteResult = getCollection(sCollectionName).deleteOne(oCriteria);
            if (oDeleteResult != null)
            {
                return (int) oDeleteResult.getDeletedCount();
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
	}
	
	public void setRepoDb(String sRepoDb) {	
			this.m_sRepoDb = sRepoDb;
	}
	
	public boolean update(BasicDBObject oCriteria, Object oNewDocument, String sCollectionName) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oNewDocument);
            
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            UpdateResult oResult = getCollection(sCollectionName).updateOne(oCriteria, oUpdateOperationDocument);
            
            if (oResult.getModifiedCount()==1) return true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }
	
}

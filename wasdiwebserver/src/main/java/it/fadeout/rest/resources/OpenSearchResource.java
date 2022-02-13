package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import it.fadeout.Wasdi;
import wasdi.shared.business.User;
import wasdi.shared.config.CatalogueConfig;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.ConcreteQueryTranslator;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.queryexecutors.QueryExecutorFactory;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.DataProviderViewModel;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Open Search Resource.
 * Hosts API for:
 * 	.query the Data Providers
 * @author p.campanella
 *
 */
@Path("/search")
public class OpenSearchResource {
	/**
	 * Static reference to this class name for logs
	 */
	private String m_sClassName;
	
	/**
	 * Concrete Query Translator to get the View Model from the input query
	 */
	private ConcreteQueryTranslator m_oConcreteQueryTranslator = new ConcreteQueryTranslator();
	
	/**
	 * Constructor
	 */
	public OpenSearchResource() {
		// Set this class name
		m_sClassName = "OpenSearchResource";		
	}
	
	/**
	 * Get the number of total results for a query
	 * @param sSessionId User Session Id
	 * @param sQuery Query
	 * @param sProviders Data Provider.
	 * @return number of results. -1 in case of any problem
	 */
	@GET
	@Path("/query/count")
	@Produces({ "application/xml", "application/json", "text/html" })
	public int count(@HeaderParam("x-session-token") String sSessionId, @QueryParam("query") String sQuery,
			@QueryParam("providers") String sProviders) {
		
		try {
			
			// Check the session
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
			if (oUser == null) {
				Utils.debugLog(m_sClassName + ".count: invalid session");
				return -1;
			}
	
			int iCounter = 0;
			
			Utils.debugLog(m_sClassName + ".count, user: " + oUser.getUserId() + ", providers: " + sProviders + ", query: " + sQuery);
			
			if (Utils.isNullOrEmpty(sProviders)) sProviders = "AUTO";
			String sOriginalProvider = sProviders;
			String sPlatformType = getPlatform(sQuery);
			sProviders = getProvider(sProviders, sPlatformType);
			int iNextProvider = 1;
			
			while (sProviders!=null) {
				try {
					
					QueryExecutor oExecutor = QueryExecutorFactory.getExecutor(sProviders);
					
					iCounter = oExecutor.executeCount(sQuery);
					
					if (iCounter>=0) {
						return iCounter;
					}
					
				} catch (Exception oE) {
					Utils.debugLog(m_sClassName + ".count: " + oE);
				}
				
				sProviders = getProvider(sOriginalProvider, sPlatformType,iNextProvider);
				iNextProvider++;
				
				if (sProviders!=null) {
					Utils.debugLog(m_sClassName + ".count: selected next provider " + sProviders);
				}				
			}

			return iCounter;
		} catch (Exception oE) {
			Utils.debugLog(m_sClassName + ".count: " + oE);
		}
		return -1;
	}
	
	/**
	 * Make a paginated query to a provider.
	 * The API converts the input query in a query for the data provider.
	 * It executes the query and get results from data provider.
	 * Results are converted in a unique Query Result View Model
	 * 
	 * These operation are handled by specific Data Providers objects in wasdi.shared.queryexecutors 
	 * 
	 * @param sSessionId User Session Id
	 * @param sProvider Provider code. 
	 * @param sQuery Query
	 * @param sOffset results offset
	 * @param sLimit number of elements 
	 * @param sSortedBy sort column
	 * @param sOrder Order by column
	 * @return List of Query Result View Models
	 */
	@GET
	@Path("/query")
	@Produces({ "application/json", "text/html" })
	public QueryResultViewModel[] search(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("providers") String sProvider, @QueryParam("query") String sQuery,
			@QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit,
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder) {
		
		Utils.debugLog(m_sClassName + ".search( Providers: " + sProvider + ", Query: " +
				sQuery + ", Offset: " + sOffset + ", Limit: " + sLimit + ", SortedBy: " + sSortedBy + ", Order: " + sOrder + " )");
		
		// Domain Check
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			Utils.debugLog(m_sClassName + ".search: invalid session");
			return null;
		}
		
		// Check the data provider
		if (Utils.isNullOrEmpty(sProvider)) sProvider = "AUTO";
		String sOriginalProvider = sProvider;
		String sPlatformType = getPlatform(sQuery);
		sProvider = getProvider(sProvider, sPlatformType);
		
		if (Utils.isNullOrEmpty(sProvider)) {
			Utils.debugLog(m_sClassName + ".search: Impossible to find the Provider ");
			return null;
		}
		
		Utils.debugLog(m_sClassName + ".search: Selected Provider " +sProvider);
				
		// Get the number of elements per page
		ArrayList<QueryResultViewModel> aoResults = new ArrayList<>();
		
		int iNextProvider = 1;
		
		// If we have providers to query
		while (sProvider != null) {
			
			try {
				// Get the query executor
				QueryExecutor oExecutor = QueryExecutorFactory.getExecutor(sProvider);
				
				// Create the paginated query
				PaginatedQuery oQuery = new PaginatedQuery(sQuery, sOffset, sLimit, sSortedBy, sOrder);
				// Execute the query
				List<QueryResultViewModel> aoProviderResults = oExecutor.executeAndRetrieve(oQuery);
				
				// Do we have results?
				if (aoProviderResults != null) {
					
					if (sOriginalProvider.equals("AUTO")) {
						// Set the provider as it was in original
						for (QueryResultViewModel oResult : aoProviderResults) {
							oResult.setProvider(sOriginalProvider);
						}								
					}
					
					Utils.debugLog(m_sClassName + ".search: found " + aoProviderResults.size() + " results for " + sProvider);
					
					if (aoProviderResults.size()>0) {
						// Yes perfect add all
						aoResults.addAll(aoProviderResults);
					}
					else {
						// Nothing to add
						Utils.debugLog(m_sClassName + ".search: no results found for " + sProvider);
					}					
					
					return aoResults.toArray(new QueryResultViewModel[aoResults.size()]);
				} 
			}
			catch (Exception oE) {
				Utils.debugLog(m_sClassName + ".search: " + oE);
			}
			
			sProvider = getProvider(sOriginalProvider, sPlatformType, iNextProvider);
			iNextProvider ++;
			
			if (sProvider!=null) {
				Utils.debugLog(m_sClassName + ".search: selected next provider " + sProvider);
			}			
		}
		
		return aoResults.toArray(new QueryResultViewModel[aoResults.size()]);
	}
	
	/**
	 * Get the list of Data Providers
	 * @param sSessionId User Session
	 * @return List of Search Provider View Models
	 */
	@GET
	@Path("/providers")
	@Produces({ "application/json", "text/html" })
	public ArrayList<DataProviderViewModel> getDataProviders(@HeaderParam("x-session-token") String sSessionId) {
		Utils.debugLog(m_sClassName + ".getDataProviders");
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				return null;
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog(m_sClassName + ".getDataProviders: invalid session");
				return null;
			}			
			
			ArrayList<DataProviderViewModel> aoRetProviders = new ArrayList<>();
			
			for (DataProviderConfig oDataProviderConfig: WasdiConfig.Current.dataProviders) {
				DataProviderViewModel oSearchProvider = new DataProviderViewModel();
				oSearchProvider.setCode(oDataProviderConfig.name);
				
				String sDescription = oDataProviderConfig.description;
				if (Utils.isNullOrEmpty(sDescription)) sDescription = oDataProviderConfig.name;
				oSearchProvider.setDescription(sDescription);
				
				String sLink = oDataProviderConfig.link;
				oSearchProvider.setLink(sLink);
				aoRetProviders.add(oSearchProvider);
			}
			
			return aoRetProviders;
		} catch (Exception oE) {
			Utils.debugLog(m_sClassName + ".getDataProviders: " + oE);
			return null;
		}
	}
	
	/**
	 * Get the total count of results for different queries
	 * @param sSessionId User Session
	 * @param sProviders Provider data provider
	 * @param asQueries list of strings, each representing a query
	 * @return Total number of products found
	 */
	@POST
	@Path("/query/countlist")
	@Produces({ "application/xml", "application/json", "text/html" })
	public int countList(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("providers") String sProviders,
			ArrayList<String> asQueries) {
		
		String sQuery = "";

		Utils.debugLog(m_sClassName + ".countList( Providers: " + sProviders + ", Queries: " + asQueries + " )");
		try {
			
			// Validate the input			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog(m_sClassName + ".countList, session: invalid");
				return -1;
			}
			
			// We need query!
			if(null==asQueries || asQueries.size() <= 0) {
				Utils.debugLog(m_sClassName + ".countList, asQueries is null");
				return -1;
			}
			
			// Total results counter
			int iCounter = 0;
			
			// Save the original Provider
			String sOriginalProvider = sProviders;
	
			for (int iQueries = 0; iQueries < asQueries.size(); iQueries++) {
				
				// Select the query
				sQuery = asQueries.get(iQueries);
				
				// Get Platform and Data Provider
				String sPlatformType = getPlatform(sQuery);
				sProviders = getProvider(sOriginalProvider, sPlatformType);
				
				int iNextProvider = 1;
				
				// We need a valid provider
				while(sProviders != null) {
					
					try {
						
						// Get the Executor
						QueryExecutor oExecutor = QueryExecutorFactory.getExecutor(sProviders);
						
						// Make the count
						int iQueryCount = oExecutor.executeCount(sQuery);
						
						// Every result >= 0 means a valid result
						if (iQueryCount>=0) {
							// Increment the counter and go to the next query
							iCounter += iQueryCount;
							break;
						}
						
					} catch (Exception oE) {
						Utils.debugLog(m_sClassName + ".countList: " + oE);
					}
					
					// Try to get next provider
					sProviders = getProvider(sOriginalProvider, sPlatformType, iNextProvider);
					iNextProvider++;
					
					if (sProviders!=null) {
						Utils.debugLog(m_sClassName + ".countList: selected next provider " + sProviders);
					}
				}
			}
			
			return iCounter;
		} catch (Exception oE) {
			Utils.debugLog(m_sClassName + ".countList (maybe your request was ill-formatted: "+ sQuery + " ?): " + oE);
		}
		return -1;
	}
	
	/**
	 * Make a NOT paginated query to a provider.
	 * The API converts the input query in a query for the data provider.
	 * It executes the query and get results from data provider.
	 * Results are converted in a unique Query Result View Model
	 * 
	 * These operation are handled by specific Data Providers objects in wasdi.shared.opensearch 
	 * 
	 * @param sSessionId User Session
	 * @param sProvider Data Provider
	 * @param asQueries Array of strings with the query to execute
	 * @return
	 */
	@POST
	@Path("/querylist")
	@Produces({ "application/json", "text/html" })
	public QueryResultViewModel[] searchList(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("providers") String sProvider, ArrayList<String> asQueries) {

		Utils.debugLog(m_sClassName + ".searchList( Providers: " + sProvider + " )");
		try { 
			
			// Validate the User
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog(m_sClassName + ".searchList, session is invalid");
				return null;
			}
						
			// Check if we have at least one query
			if(null==asQueries || asQueries.size()<= 0) {
				Utils.debugLog(m_sClassName + ".searchList, user: "+oUser.getUserId()+", asQueries = "+asQueries);
				return null;
			}
	
			// Prepare the output list
			ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
			
			if (Utils.isNullOrEmpty(sProvider)) sProvider = "AUTO";
			
			// Save the original provider
			String sOriginalProviders = sProvider;
			
			// For Each Input query
			for (int iQueries = 0; iQueries < asQueries.size(); iQueries++) {
				
				try {
					String sQuery = asQueries.get(iQueries);
					
					// Get the query
					Utils.debugLog(m_sClassName + ".searchList; query = " + sQuery);
					
					String sPlatformType = getPlatform(sQuery);
					sProvider = getProvider(sOriginalProviders, sPlatformType);
					Utils.debugLog(m_sClassName + ".searchList; selected provider = " + sProvider);
					
					int iNextProvider = 1;
					
					// For each provider
					while (sProvider != null) {
						
						// Get the executor
						QueryExecutor oExecutor = QueryExecutorFactory.getExecutor(sProvider);
						
						// Get the Provider Total Count
						int iTotalResultsForProviders = oExecutor.executeCount(sQuery);
						
						// Any result >= 0 is valid 
						if (iTotalResultsForProviders>=0) {
							Utils.debugLog(m_sClassName + ".searchList: [" + sProvider + "] Images Found " + iTotalResultsForProviders);
							
							// Get the real results, paginated
							int iObtainedResults = 0;
							
							// Page size
							int iLimit = getPageLimitForProvider(sProvider);
														
							float fMaxPages = iTotalResultsForProviders / (float)iLimit;
							int iMaxPages = (int) Math.ceil(fMaxPages);
							iMaxPages *= 2;
							Utils.debugLog(m_sClassName + ".searchList: Augmentented Max Pages: " + iMaxPages + " Total Results: " + iTotalResultsForProviders + " Limit " + iLimit);
							
							
							int iActualPage = 0;
							
							// Until we do not get all the results
							while (iObtainedResults < iTotalResultsForProviders) {
								
								if (iActualPage>iMaxPages) {
									Utils.debugLog(m_sClassName + ".searchList: cycle running out of control, actual page " + iActualPage + " , Max " + iMaxPages + " break");
									break;
								}
								
								// Actual Offset
								String sCurrentOffset = "" + iObtainedResults;
								
								String sOriginalLimit = "" + iLimit;
		
								// How many elements do we need yet?
								if ((iTotalResultsForProviders - iObtainedResults) < iLimit) {
									iLimit = iTotalResultsForProviders - iObtainedResults;
								}
			
								String sCurrentLimit = "" + iLimit;
								
								// Create the paginated Query
								PaginatedQuery oQuery = new PaginatedQuery(sQuery, sCurrentOffset, sCurrentLimit, null, null, sOriginalLimit);
								
								// Log the query
								Utils.debugLog(m_sClassName + ".searchList, query page " + iActualPage);
								
								try {
									// Execute the query
									List<QueryResultViewModel> aoProviderPageResult = oExecutor.executeAndRetrieve(oQuery, false);
									
									// Did we got a result?
									if (aoProviderPageResult != null && !aoProviderPageResult.isEmpty()) {
										
										// Sum the grand total
										iObtainedResults += aoProviderPageResult.size();
										
										int iAddedResults = 0;
										
										// Here add the results checking to avoid duplicates
										for (QueryResultViewModel oTempResult : aoProviderPageResult) {
											if (!aoResults.contains(oTempResult)) {
												aoResults.add(oTempResult);
												iAddedResults++;
											}
											else {
												Utils.debugLog(m_sClassName + ".searchList: found duplicate image " + oTempResult.getTitle());
											}
										}
										
										Utils.debugLog(m_sClassName + ".searchList added " + iAddedResults + " results for Query#" + iQueries +" for " + sProvider);
									} else {
										Utils.debugLog(m_sClassName + ".searchList, NO results found for " + sProvider);
									}
								} catch (Exception oEx) {
									Utils.debugLog(m_sClassName + ".searchList: " + oEx);
								}
								
								iActualPage ++;
							}
							
							// Exit from the providers cylcle
							sProvider = null;				
						}
						else {
							Utils.debugLog(m_sClassName + " Error contacting " + sProvider + " try next provider");
							sProvider = getProvider(sOriginalProviders, sPlatformType, iNextProvider);
							iNextProvider++;
							
							if (sProvider != null) {
								Utils.debugLog(m_sClassName + " selected Provider " + sProvider);
							}
							else {
								Utils.debugLog(m_sClassName + " no more providers abailable ");	
							}
						}
					}
				} catch (Exception oE) {
					Utils.debugLog(m_sClassName + ".SearchList: (maybe your request was ill-formatted: " + oE);
				}
			}
			
			// Set the provider as it was in origin
			for (QueryResultViewModel oRes : aoResults) {
				oRes.setProvider(sOriginalProviders);
			}
			
			return aoResults.toArray(new QueryResultViewModel[aoResults.size()]);
		} catch (Exception oE) {
			Utils.debugLog(m_sClassName + ".SearchList: " + oE);
		}
		return null;
	}
	
	
	/**
	 * Get the provider type from the provider submitted by the user and the platform type 
	 * @param sProviderInput User supplied provider request
	 * @param sPlatform Platform type
	 * @return Provider Code or null if there are no more choices
	 */
	String getProvider(String sProviderInput, String sPlatform) {
		return getProvider(sProviderInput, sPlatform, 0);
	}
	
	/**
	 * Get the Provider Code from the provider submitted by the user, the platform type 
	 * and the priority
	 * 
	 * @param sProviderInput User supplied provider request
	 * @param sPlatform Platform type of the actual query
	 * @param iPriority Priority of the catalogue (provider) to obtain
	 * @return Provider Code or null if there are no more choices
	 */
	String getProvider(String sProviderInput, String sPlatform, int iPriority) {
		try {
			if (Utils.isNullOrEmpty(sProviderInput) || sProviderInput.equals("AUTO")) {
				CatalogueConfig oCatalogueConfig = WasdiConfig.Current.getCatalogueConfig(sPlatform);
				
				if (oCatalogueConfig!=null) {
					if (oCatalogueConfig.catalogues != null) {
						if (iPriority<oCatalogueConfig.catalogues.size()) {
							return oCatalogueConfig.catalogues.get(iPriority);
						}
					}
				}
				
				return null;
			}
		}
		catch (Exception oEx) {
			Utils.debugLog(m_sClassName + ".getProvider: " + oEx.toString());
		}
		
		if (iPriority == 0) {
			return sProviderInput;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get the Platform from the user query
	 * @param sQuery User Query
	 * @return Platform Type as enumered in Platforms
	 */
	public String getPlatform(String sQuery) {
		
		QueryViewModel oQuery = m_oConcreteQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (oQuery != null) {
			return oQuery.platformName;
		}
		else {
			return "";
		}
	}
	
	/**
	 * Get the limit of results that can be queried to each Data Provider
	 * @param sProvider Data Provider
	 * @return Specific Limit
	 */
	public int getPageLimitForProvider(String sProvider) {
		// Page size
		int iLimit = 100;
		
		try {
			
			DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(sProvider);
			
			String sProviderLimit = oDataProviderConfig.searchListPageSize;
			
			if (!Utils.isNullOrEmpty(sProviderLimit)) {
				try {
					iLimit = Integer.parseInt(sProviderLimit);
					Utils.debugLog(sProvider + " using " + sProviderLimit + " Page Size ");
				}
				catch (Exception e) {
				}
			}
			
			// Check the value, never known...
			if (iLimit<=0) iLimit = 100;
			
			return iLimit;			
		}
		catch (Exception oEx) {
		}
		
		return iLimit;
		
	}

}

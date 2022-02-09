using System.Security.AccessControl;
using System.Web;

using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using WasdiLib.Configuration;
using WasdiLib.Helpers;
using WasdiLib.Models;
using WasdiLib.Services;

namespace WasdiLib
{
    public class WasdiLib
    {

        private readonly ILogger<WasdiLib> _logger;
        private IConfiguration _configuration;


        private IProcessWorkspaceService _processWorkspaceService;
        private IProductService _productService;
        private IWasdiService _wasdiService;
        private IWorkflowService _workflowService;
        private IWorkspaceService _workspaceService;


        private string m_sUser = "";
        private string m_sPassword = "";
        private string m_sActiveWorkspace = "";
        private string m_sWorkspaceBaseUrl = "";
        private string m_sWorkspaceOwner = "";
        private string m_sSessionId = "";
        private string m_sBaseUrl = "https://www.wasdi.net/wasdiwebserver/rest/";
        private bool m_bIsOnServer = false;
        private bool m_bDownloadActive = true;
        private bool m_bUploadActive = true;
        private string m_sBasePath = "";
        private string m_sMyProcId = "";
        private bool m_bVerbose = true;
        private Dictionary<string, string> m_aoParams = new Dictionary<string, string>();
        private string m_sParametersFilePath = "";
        private string m_sDefaultProvider = "AUTO";


        public WasdiLib()
        {


            Startup.RegisterServices();
            Startup.SetupErrorLogger();

            _logger = Startup.ServiceProvider.GetService<ILogger<WasdiLib>>();
            _logger.LogDebug("WasdiLib()");

            _processWorkspaceService = Startup.ServiceProvider.GetService<IProcessWorkspaceService>();
            _productService = Startup.ServiceProvider.GetService<IProductService>();
            _wasdiService = Startup.ServiceProvider.GetService<IWasdiService>();
            _workflowService = Startup.ServiceProvider.GetService<IWorkflowService>();
            _workspaceService = Startup.ServiceProvider.GetService<IWorkspaceService>();

        }

        public bool Init(string sConfigFilePath = null)
        {
            _logger.LogDebug("Init({0})", sConfigFilePath);

            bool loadConfiguration = LoadConfigurationFile(sConfigFilePath);

            if (!loadConfiguration)
                return false;

            LoadConfigurationValues();

            LoadParametersFile();


            if (InternalInit(GetUser(), GetPassword(), GetSessionId()))
            {
                if (String.IsNullOrEmpty(m_sActiveWorkspace))
                {
                    string sWorkspaceName = _configuration.GetValue<string>("WORKSPACE", "");

                    _logger.LogInformation("Workspace to open: {0}", sWorkspaceName);

                    if (sWorkspaceName != "")
                    {
                        OpenWorkspace(sWorkspaceName);
                    }
                }
                else
                {
                    OpenWorkspaceById(m_sActiveWorkspace);
                    _logger.LogInformation("Active workspace set " + m_sActiveWorkspace);
                }

                return true;
            }
            else
            {
                return false;
            }

        }

        private bool LoadConfigurationFile(string sConfigFilePath = null)
        {

            if (String.IsNullOrEmpty(sConfigFilePath))
            {
                _logger.LogInformation("Init: \"" + sConfigFilePath + "\" is not a valid path, trying with the default file");
                sConfigFilePath = Path.GetFullPath("appsettings.json");
            }

            if (!File.Exists(sConfigFilePath))
            {
                _logger.LogError("Init: \"" + sConfigFilePath + "\" is not a valid path for a config file, aborting");
                return false;
            }

            Startup.LoadConfiguration(sConfigFilePath);
            _configuration = Startup.ConfigurationRoot;

            if (_configuration == null)
            {
                _logger.LogError("Init: \"" + sConfigFilePath + "\" is not a valid path for a config file, aborting");
                return false;
            }

            return true;
        }

        private void LoadConfigurationValues()
        {

            m_sUser = _configuration.GetValue<string>("USER", "");
            m_sPassword = _configuration.GetValue<string>("PASSWORD", "");
            m_sBasePath = _configuration.GetValue<string>("BASEPATH", "");
            m_sBaseUrl = _configuration.GetValue<string>("BASEURL", "https://www.wasdi.net/wasdiwebserver/rest");

            if (!m_sBaseUrl.EndsWith("/"))
                m_sBaseUrl += "/";

            m_sSessionId = _configuration.GetValue<string>("SESSIONID", "");
            m_sActiveWorkspace = _configuration.GetValue<string>("WORKSPACEID", "");
            m_sMyProcId = _configuration.GetValue<string>("MYPROCID", "");
            m_sParametersFilePath = _configuration.GetValue<string>("PARAMETERSFILEPATH", "./parameters.txt");

            string sVerbose = _configuration.GetValue<string>("VERBOSE", "");
            if (sVerbose == "1" || sVerbose.ToUpper() == "TRUE")
                m_bVerbose = true;

            _logger.LogInformation("SessionId from config " + m_sSessionId);

            string sDownloadActive = _configuration.GetValue<string>("DOWNLOADACTIVE", "1");
            if (sDownloadActive == "0" || sDownloadActive.ToUpper() == "FALSE")
                m_bDownloadActive = false;

            string sUploadactive = _configuration.GetValue<string>("UPLOADACTIVE", "1");
            if (sUploadactive == "0" || sUploadactive.ToUpper() == "FALSE")
                SetUploadActive(false);

            string sIsOnServer = _configuration.GetValue<string>("ISONSERVER", "0");
            if (sIsOnServer == "1" || sUploadactive.ToUpper() == "TRUE")
            {
                m_bIsOnServer = true;
                // On Server Force Download to false
                m_bDownloadActive = false;
                m_bVerbose = true;
            }
            else
            {
                m_bIsOnServer = false;
            }

            if (m_sBasePath == "")
            {
                if (!m_bIsOnServer)
                {
                    string sUserHome = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
                    string sWasdiHome = sUserHome + "/.wasdi/";

                    //create the directory
                    if (!Directory.Exists(sWasdiHome))
                    {
                        Directory.CreateDirectory(sWasdiHome);
                    }

                    m_sBasePath = sWasdiHome;
                }
                else
                {
                    m_sBasePath = "/data/wasdi/";
                }
            }
        }

        private void LoadParametersFile()
        {
            if (String.IsNullOrEmpty(m_sParametersFilePath))
            {
                _logger.LogWarning("Init: the parameters-file path is not set, trying to use the parameters.json file");
                m_sParametersFilePath = Path.GetFullPath("parameters.json");
            }

            if (File.Exists(m_sParametersFilePath))
            {
                var sParametersJson = File.ReadAllText(m_sParametersFilePath);
                try
                {
                    m_aoParams = SerializationHelper.FromJson<Dictionary<string, string>>(sParametersJson);
                }
                catch (Exception ex)
                {
                    _logger.LogError("LoadParametersFile: the content of the {0} file could not be read as a JSON file.", m_sParametersFilePath);
                }

                if (m_aoParams == null)
                    m_aoParams = new Dictionary<string, string>();
            }
        }



        public bool InternalInit()
        {
            _logger.LogDebug("InternalInit");

            return InternalInit(GetUser(), GetPassword(), GetSessionId());
        }

        public bool InternalInit(string sUser, string sPassword, string sSessionId)
        {
            _logger.LogDebug("InternalInit");
            _logger.LogDebug("C# WASDILib Init");

            // User Name Needed
            if (sUser == null)
                return false;

            _logger.LogDebug("User not null " + sUser);

            // Is there a password?
            if (!String.IsNullOrEmpty(sPassword))
            {
                _logger.LogDebug("Password not null. Try to login");

                // Try to log in
                sSessionId = ObtainSessionId(sUser, sPassword);

                if (sSessionId != null)
                {
                    SetSessionId(sSessionId);
                    _logger.LogInformation("User logged: session ID " + sSessionId);

                    return true;
                }
            }
            else if (sSessionId != null)
            {
                _logger.LogInformation("Check Session: session ID " + sSessionId);

                // Check User supplied Session
                if (CheckSession(sSessionId, sUser))
                {
                    _logger.LogInformation("Check Session: session ID OK");
                    return true;
                }
                else
                {
                    _logger.LogError("Check Session: session ID not valid");
                    return false;
                }
            }

            return false;
        }


        public string GetDefaultProvider()
        {
            return m_sDefaultProvider;
        }

        public void SetDefaultProvider(string sProvider)
        {
            _logger.LogDebug("SetDefaultProvider({0})", sProvider);

            if (String.IsNullOrEmpty(sProvider))
            {
                _logger.LogError("SetDefaultProvider: the provider cannot be null or empty, aborting");
                return;
            }

            this.m_sDefaultProvider = sProvider;
        }

        public string GetUser()
        {
            return m_sUser;
        }

        public void SetUser(string sUser)
        {
            _logger.LogDebug("SetUser({0})", sUser);

            if (String.IsNullOrEmpty(sUser))
            {
                _logger.LogError("SetUser: user null or empty, aborting");
                return;
            }

            this.m_sUser = sUser;
        }

        public string GetPassword()
        {
            return m_sPassword;
        }

        public void SetPassword(string sPassword)
        {
            _logger.LogDebug("SetPassword( ********** )");

            this.m_sPassword = sPassword;
        }

        public string GetActiveWorkspace()
        {
            return m_sActiveWorkspace;
        }

        public void SetActiveWorkspace(string sNewActiveWorkspaceId)
        {
            _logger.LogDebug("SetActiveWorkspace({0})", sNewActiveWorkspaceId);

            this.m_sActiveWorkspace = sNewActiveWorkspaceId;

            if (!String.IsNullOrEmpty(m_sActiveWorkspace))
            {
                m_sWorkspaceOwner = GetWorkspaceOwnerByWSId(sNewActiveWorkspaceId);
            }

        }

        public string GetSessionId()
        {
            return m_sSessionId;
        }

        public void SetSessionId(string sSessionId)
        {
            _logger.LogDebug("SetSessionId({0})", sSessionId);

            if (String.IsNullOrEmpty(sSessionId))
            {
                _logger.LogError("SetSessionId: session null or empty, aborting");
                return;
            }

            this.m_sSessionId = sSessionId;
        }

        public string GetBaseUrl()
        {
            return m_sBaseUrl;
        }

        public void SetBaseUrl(string sBaseUrl)
        {
            _logger.LogDebug("SetBaseUrl({0})", sBaseUrl);

            Uri oURI = new Uri(sBaseUrl);
            if (oURI == null || oURI.AbsolutePath == null)
            {
                _logger.LogError("SetBaseUrl: \"" + sBaseUrl + "\" is not a valid URL: cannot obtain URI from URL, aborting");
                return;
            }

            this.m_sBaseUrl = sBaseUrl;
        }

        public bool GetIsOnServer()
        {
            return m_bIsOnServer;
        }

        public void SetIsOnServer(bool bIsOnServer)
        {
            _logger.LogDebug("SetIsOnServer({0})", bIsOnServer);

            this.m_bIsOnServer = bIsOnServer;
        }

        public Boolean GetDownloadActive()
        {
            return m_bDownloadActive;
        }

        public void SetDownloadActive(bool bDownloadActive)
        {
            _logger.LogDebug("SetDownloadActive({0})", bDownloadActive);

            this.m_bDownloadActive = bDownloadActive;
        }

        public bool GetUploadActive()
        {
            return m_bUploadActive;
        }

        public void SetUploadActive(bool bUploadActive)
        {
            _logger.LogDebug("SetUploadActive({0})", bUploadActive);

            this.m_bUploadActive = bUploadActive;
        }

        public string GetBasePath()
        {
            return m_sBasePath;
        }

        public void SetBasePath(string sBasePath)
        {
            _logger.LogDebug("SetBasePath({0})", sBasePath);

            if (String.IsNullOrEmpty(sBasePath) || sBasePath.Contains(".."))
            {
                _logger.LogError("SetBasePath: \"" + sBasePath + "\" is not a valid path, aborting");
                return;
            }

            if (!Directory.Exists(sBasePath))
            {
                _logger.LogInformation("SetBasePath: \"" + sBasePath + "\" does not exist, attempting to create it...");
                DirectoryInfo directoryInfo = Directory.CreateDirectory(sBasePath);

                if (directoryInfo == null || !directoryInfo.Exists)
                {
                    _logger.LogError("SetBasePath: directory \"" + sBasePath + "\" could not be created, aborting");
                    return;
                }
                else
                {
                    _logger.LogInformation("SetBasePath: directory \"" + sBasePath + "\" successfully created");
                }

                //check accessibility
                if (!directoryInfo.Exists || !HasWritePermissionOnDir(sBasePath))
                {
                    _logger.LogError("SetBasePath: \"" + sBasePath + "\" canot be read properly, aborting");
                    return;
                }
            }

            this.m_sBasePath = sBasePath;
        }

        public static bool HasWritePermissionOnDir(string path)
        {
            var writeAllow = false;
            var writeDeny = false;

            DirectoryInfo directoryInfo = new DirectoryInfo(path);
            var accessControlList = directoryInfo.GetAccessControl();

            if (accessControlList == null)
                return false;

            var accessRules = accessControlList.GetAccessRules(true, true,                        
                typeof(System.Security.Principal.SecurityIdentifier));

            if (accessRules == null)
                return false;

            foreach (FileSystemAccessRule rule in accessRules)
            {
                if ((FileSystemRights.Write & rule.FileSystemRights) != FileSystemRights.Write)
                    continue;

                if (rule.AccessControlType == AccessControlType.Allow)
                    writeAllow = true;
                else if (rule.AccessControlType == AccessControlType.Deny)
                    writeDeny = true;
            }

            return writeAllow && !writeDeny;
        }

        public string GetMyProcId()
        {
            return m_sMyProcId;
        }

        public void SetMyProcId(string sMyProcId)
        {
            _logger.LogDebug("SetMyProcId({0})", sMyProcId);

            if (String.IsNullOrEmpty(sMyProcId))
            {
                _logger.LogError("SetSessionId:: processor ID is null or empty, aborting");
                return;
            }

            this.m_sMyProcId = sMyProcId;
        }

        public bool GetVerbose()
        {
            return m_bVerbose;
        }

        public void SetVerbose(bool bVerbose)
        {
            _logger.LogDebug("SetVerbose({0})", bVerbose);

            this.m_bVerbose = bVerbose;
        }

        public Dictionary<string, string> GetParams()
        {
            Dictionary<string, string> aoParamsCopy = new Dictionary<string, string>(m_aoParams);

            if (aoParamsCopy.ContainsKey("user"))
                aoParamsCopy.Remove("user");

            if (aoParamsCopy.ContainsKey("sessionid"))
                aoParamsCopy.Remove("sessionid");

            if (aoParamsCopy.ContainsKey("workspaceid"))
                aoParamsCopy.Remove("workspaceid");

            return aoParamsCopy;
        }

        public string GetParamsAsJsonString()
        {
            if (GetParams() == null)
            {
                _logger.LogError("getParamsAsJsonString: no params, returning empty JSON");
                return "{}";
            }

            return SerializationHelper.ToJson(GetParams());
        }

        public void AddParam(string sKey, string sParam)
        {
            _logger.LogDebug("SetMyProcId({0}, {1})", sKey, sParam);
            m_aoParams.Add(sKey, sParam);
        }

        public string GetParam(string sKey)
        {
            return m_aoParams.GetValueOrDefault(sKey, "");
        }

        public string GetParametersFilePath()
        {
            return m_sParametersFilePath;
        }

        public void SetParametersFilePath(string sParametersFilePath)
        {
            _logger.LogDebug("SetParametersFilePath({0})", sParametersFilePath);

            if (String.IsNullOrEmpty(sParametersFilePath) || sParametersFilePath.Contains(".."))
            {
                _logger.LogError("SetParametersFilePath: \"" + sParametersFilePath + "\" is not a valid path, aborting");
                return;
            }

            if (!File.Exists(sParametersFilePath))
            {
                _logger.LogError("SetParametersFilePath: \"" + sParametersFilePath + "\" does not exist, aborting");
                return;
            }

            this.m_sParametersFilePath = sParametersFilePath;
        }

        public string ObtainSessionId(string sUser, string sPassword)
        {

            try
            {
                LoginResponse loginResponse = _wasdiService.Authenticate(m_sBaseUrl, sUser, sPassword);

                if (loginResponse != null)
                    return loginResponse.SessionId;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return null;
        }

        public bool CheckSession(string sSessionID, string sUser)
        {

            try
            {
                LoginResponse loginResponse = _wasdiService.CheckSession(m_sBaseUrl, sSessionID);

                if (loginResponse != null)
                    return loginResponse.UserId == sUser;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return false;
        }





        public string GetWorkspaceBaseUrl()
        {
            return m_sWorkspaceBaseUrl;
        }

        public void SetWorkspaceBaseUrl(string sWorkspaceBaseUrl)
        {
            _logger.LogDebug("SetWorkspaceBaseUrl({0})", sWorkspaceBaseUrl);

            Uri uriResult;
            bool result = Uri.TryCreate(sWorkspaceBaseUrl, UriKind.Absolute, out uriResult)
                && (uriResult.Scheme == Uri.UriSchemeHttp || uriResult.Scheme == Uri.UriSchemeHttps);

            if (!result || uriResult == null)
            {
                _logger.LogError("SetWorkspaceBaseUrl: \"" + sWorkspaceBaseUrl + "\" is not a valid URL: cannot obtain URI from URL, aborting");
                return;
            }

            m_sWorkspaceBaseUrl = sWorkspaceBaseUrl;
        }



        public string HelloWasdi()
        {
            _logger.LogDebug("HelloWasdi()");

            try
            {
                PrimitiveResult primitiveResult = _wasdiService.HelloWasdi(m_sBaseUrl);

                if (primitiveResult != null)
                    return primitiveResult.StringValue;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return null;
        }

        public List<Workspace> GetWorkspaces()
        {
            _logger.LogDebug("GetWorkspaces()");

            try
            {
                List<Workspace> workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

                return workspaces;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return null;
        }

        public List<string> GetWorkspacesNames()
        {
            _logger.LogDebug("GetWorkspacesNames()");

            List<string> workspacesNames = new List<string>();

            try
            {
                List<Workspace> workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

                if (workspaces != null)
                {
                    foreach (Workspace workspace in workspaces)
                    {
                        workspacesNames.Add(workspace.WorkspaceName);
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return workspacesNames;
        }

        public string GetWorkspaceIdByName(string workspaceName)
        {
            _logger.LogDebug("GetWorkspaceIdByName({0})", workspaceName);


            string workspacesId = "";

            try
            {
                List<Workspace> workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

                if (workspaces != null)
                {
                    foreach (Workspace workspace in workspaces)
                    {
                        if (workspaceName == workspace.WorkspaceName)
                        {
                            workspacesId = workspace.WorkspaceId;
                            break;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return workspacesId;
        }

        public string GetWorkspaceNameById(string workspacesId)
        {
            _logger.LogDebug("GetWorkspaceNameById({0})", workspacesId);


            string workspaceName = "";

            try
            {
                List<Workspace> workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

                if (workspaces != null)
                {
                    foreach (Workspace workspace in workspaces)
                    {
                        if (workspacesId == workspace.WorkspaceId)
                        {
                            workspaceName = workspace.WorkspaceName;
                            break;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return workspaceName;
        }

        public string GetWorkspaceOwnerByName(string workspaceName)
        {
            _logger.LogDebug("GetWorkspaceOwnerdByName({0})", workspaceName);

            try
            {
                List<Workspace> workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

                if (workspaces != null)
                {
                    foreach (Workspace workspace in workspaces)
                    {
                        if (workspaceName == workspace.WorkspaceName)
                        {
                            return workspace.OwnerUserId;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "";
        }

        public string GetWorkspaceOwnerByWSId(string workspaceId)
        {
            _logger.LogDebug("GetWorkspaceOwnerdById({0})", workspaceId);

            try
            {
                List<Workspace> workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

                if (workspaces != null)
                {
                    foreach (Workspace workspace in workspaces)
                    {
                        if (workspaceId == workspace.WorkspaceId)
                        {
                            return workspace.OwnerUserId;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "";
        }

        public string GetWorkspaceUrlByWsId(string workspaceId)
        {
            _logger.LogDebug("getWorkspaceUrlByWsId({0})", workspaceId);

            try
            {
                WorkspaceEditorViewModel workspace = _workspaceService.GetWorkspace(m_sBaseUrl, m_sSessionId, workspaceId);

                if (workspace != null)
                {
                    return workspace.ApiUrl;
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "";
        }

        public string OpenWorkspaceById(string sWorkspaceId)
        {
            _logger.LogDebug("OpenWorkspaceById({0})", sWorkspaceId);

            if (String.IsNullOrEmpty(sWorkspaceId))
            {
                _logger.LogError("OpenWorkspaceById: invalid workspace ID, aborting");
                return "";
            }

            SetActiveWorkspace(sWorkspaceId);

            m_sWorkspaceOwner = GetWorkspaceOwnerByWSId(sWorkspaceId);
            SetWorkspaceBaseUrl(GetWorkspaceUrlByWsId(m_sActiveWorkspace));

            if (m_sWorkspaceBaseUrl == null)
                SetWorkspaceBaseUrl("");

            if (m_sWorkspaceBaseUrl == "")
                m_sWorkspaceBaseUrl = m_sBaseUrl;

            return m_sActiveWorkspace;
        }

        public string OpenWorkspace(string sWorkspaceName)
        {
            _logger.LogDebug("OpenWorkspace({0})", sWorkspaceName);

            return OpenWorkspaceById(GetWorkspaceIdByName(sWorkspaceName));
        }

        public List<string> GetProductsByWorkspace(string sWorkspaceName)
        {
            return GetProductsByWorkspaceId(GetWorkspaceIdByName(sWorkspaceName));
        }

        public List<string> GetProductsByWorkspaceId(string sWorkspaceId)
        {
            _logger.LogDebug("GetProductsByWorkspaceId({0})", sWorkspaceId);

            List<string> asProducts = new List<string>();

            try
            {
                List<Product> productList = _productService.GetProductsByWorkspaceId(m_sBaseUrl, m_sSessionId, sWorkspaceId);

                if (productList != null)
                {
                    foreach (Product product in productList)
                    {
                        asProducts.Add(product.FileName);
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return asProducts;
        }

        public List<string> GetProductsByActiveWorkspace()
        {
            return GetProductsByWorkspaceId(m_sActiveWorkspace);
        }

        public string GetPath(string sProductName)
        {
            if (String.IsNullOrEmpty(sProductName))
            {
                _logger.LogWarning("GetPath: product name is empty or null, returning save path");
                return GetSavePath();
            }

            if (FileExistsOnWasdi(sProductName))
            {
                return InternalGetFullProductPath(sProductName);
            }
            else
            {
                return GetSavePath() + sProductName;
            }
        }

        public string InternalGetFullProductPath(string sProductName)
        {
            _logger.LogDebug("GetFullProductPath( " + sProductName + " )");

            if (String.IsNullOrEmpty(sProductName))
            {
                _logger.LogError("GetFullProductPath: product name is null or empty, aborting");
                return "";
            }

            try
            {
                string sFullPath = m_sBasePath;

                if (!(sFullPath.EndsWith("\\") || sFullPath.EndsWith("/")))
                    sFullPath += Path.DirectorySeparatorChar;

                sFullPath = sFullPath + m_sWorkspaceOwner + Path.DirectorySeparatorChar + m_sActiveWorkspace + Path.DirectorySeparatorChar + sProductName;
                bool bFileExists = Directory.Exists(sFullPath);

                if (!m_bIsOnServer)
                {
                    if (m_bDownloadActive && !bFileExists)
                    {
                        _logger.LogDebug("Local file Missing. Start WASDI download. Please wait");
                        DownloadFile(sProductName);
                        _logger.LogDebug("File Downloaded on Local PC, keep on working!");

                    }
                }
                else
                {
                    if (!bFileExists)
                    {
                        if (FileExistsOnWasdi(sProductName))
                        {
                            _logger.LogDebug("Local file Missing. Start WASDI download. Please wait");
                            DownloadFile(sProductName);
                            _logger.LogDebug("File Downloaded on Local Node, keep on working!");
                        }
                    }
                }

                return sFullPath;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
                return "";
            }
        }

        private bool FileExistsOnWasdi(string sFileName)
        {
            _logger.LogDebug("FileExistsOnWasdi({0})", sFileName);

            if (String.IsNullOrEmpty(sFileName))
            {
                _logger.LogError("FileExistssOnWasdi: passed a null or empty file name, aborting");
                return false;
            }

            try
            {
                return _wasdiService.FileExistsOnServer(GetWorkspaceBaseUrl(), m_sSessionId, m_sActiveWorkspace, true, sFileName);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            //false, because in general no upload is desirable 
            return false;
        }

        private bool FileExistsOnNode(string sFileName)
        {
            _logger.LogDebug("FileExistsOnNode({0})", sFileName);

            if (String.IsNullOrEmpty(sFileName))
            {
                _logger.LogError("FileExistssOnNode: passed a null or empty file name, aborting");
                return false;
            }

            try
            {
                return _wasdiService.FileExistsOnServer(GetWorkspaceBaseUrl(), m_sSessionId, m_sActiveWorkspace, false, sFileName);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            //false, because in general no upload is desirable 
            return false;
        }

        public string GetSavePath()
        {
            string sFullPath = m_sBasePath;

            if (!(sFullPath.EndsWith("\\") || sFullPath.EndsWith("/") || !sFullPath.EndsWith(Path.DirectorySeparatorChar)))
                sFullPath += Path.DirectorySeparatorChar;

            sFullPath = sFullPath + m_sWorkspaceOwner + Path.DirectorySeparatorChar + m_sActiveWorkspace + Path.DirectorySeparatorChar;

            return sFullPath;
        }

        public List<Workflow> GetWorkflows()
        {
            _logger.LogDebug("GetWorkflows()");

            try
            {
                List<Workflow> workflows = _workflowService.GetWorkflows(m_sBaseUrl, m_sSessionId);

                return workflows;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return null;
        }

        protected string InternalExecuteWorkflow(List<string> asInputFileNames, List<string> asOutputFileNames,
            string sWorkflowName, Boolean bAsynch)
        {
            try
            {
                List<Workflow> aoWorkflows = _workflowService.GetWorkflows(m_sBaseUrl, m_sSessionId);
            

                string sProcessId = "";
                string sWorkflowId = "";

                foreach (Workflow oWorkflow in aoWorkflows)
                {
                    if (oWorkflow.Name == sWorkflowName)
                    {
                        sWorkflowId = oWorkflow.WorkflowId;
                        break;
                    }
                }

                if (sWorkflowId == "")
                    return "";

                Workflow workflow = new Workflow()
                {
                    Name = sWorkflowName,
                    Description = "",
                    WorkflowId = sWorkflowId,
                    InputNodeNames = new List<string>(),
                    InputFileNames = asInputFileNames,
                    OutputNodeNames = new List<string>(),
                    OutputFileNames = asOutputFileNames
                };

                string? sParentId = null;

                if (m_bIsOnServer)
                {
                    sParentId = m_sMyProcId;
                }

                PrimitiveResult primitiveResult = _workflowService.CreateWorkflow(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sParentId, workflow);
                if (primitiveResult != null)
                    sProcessId = primitiveResult.StringValue;

                if (bAsynch)
                    return sProcessId;
                else
                    return WaitProcess(sProcessId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
                return "";
            }
        }

        public string AsynchExecuteWorkflow(List<string> asInputFileName, List<string> asOutputFileName,
            string sWorkflowName)
        {
            return InternalExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflowName, true);
        }

        public string ExecuteWorkflow(List<string> asInputFileName, List<string> asOutputFileName,
            string sWorkflowName)
        {
            return InternalExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflowName, false);
        }

        public string GetProcessStatus(string sProcessId)
        {
            _logger.LogDebug("GetProcessStatus({0})", sProcessId);

            if (String.IsNullOrEmpty(sProcessId))
            {
                _logger.LogError("GetProcessStatus: process id null or empty, aborting");
                return "";
            }

            try
            {
                ProcessWorkspace processWorkspace = _processWorkspaceService.GetProcessWorkspaceByProcessId(GetWorkspaceBaseUrl(), m_sSessionId, sProcessId);

                if (processWorkspace != null)
                {
                    string sStatus = processWorkspace.Status;

                    if (IsThisAValidStatus(sStatus))
                        return sStatus;
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "";
        }

        public string GetProcessesStatus(List<string> asIds)
        {
            try
            {
                string sResponse = _processWorkspaceService.GetProcessesStatus(GetWorkspaceBaseUrl(), m_sSessionId, asIds);

            return sResponse;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "";
        }

        public List<string> GetProcessesStatusAsList(List<string> asIds)
        {
            string sStatus = GetProcessesStatus(asIds);

            if (sStatus.Length < 2)
                return new List<string>();

            List<string> result = sStatus
                .Substring(1, sStatus.Length - 1)
                .Replace("\"", "")
                .Split(",").ToList();

            return result;
        }

        public string UpdateStatus(string sStatus)
        {
            _logger.LogDebug("UpdateStatus({0})", sStatus);

            if (!IsThisAValidStatus(sStatus))
            {
                _logger.LogError("UpdateStatus: " + sStatus + " is not a valid status, aborting");
                return "";
            }

            if (!m_bIsOnServer)
                return sStatus;

            return UpdateStatus(sStatus, -1);
        }

        public string UpdateStatus(string sStatus, int iPerc)
        {
            _logger.LogDebug("UpdateStatus({0}, {1})", sStatus, iPerc);

            if (!IsThisAValidStatus(sStatus))
            {
                _logger.LogError("UpdateStatus( " + sStatus + ", " + iPerc + " ): " + sStatus + " is not a valid status, aborting");
                return "";
            }

            if (!m_bIsOnServer)
                return sStatus;

            return UpdateProcessStatus(GetMyProcId(), sStatus, iPerc);
        }

        public string UpdateProcessStatus(string sProcessId, string sStatus, int iPerc)
        {

            if (!IsThisAValidStatus(sStatus))
            {
                _logger.LogError("UpdateStatus: " + sStatus + " is not a valid status. It must be one of  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY");
                return "";
            }

            if (String.IsNullOrEmpty(sProcessId))
            {
                _logger.LogError("sProcessId must not be null or empty");
                return "";
            }

            try
            {
                ProcessWorkspace processWorkspace = _processWorkspaceService.UpdateProcessStatus(GetWorkspaceBaseUrl(), m_sSessionId, sProcessId, sStatus, iPerc);

                if (processWorkspace != null)
                    return processWorkspace.Status;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "";
        }

        public string UpdateProgressPerc(int iPerc)
        {
            if (!m_bIsOnServer)
                return "RUNNING";

            if (iPerc < 0 || iPerc > 100)
            {
                _logger.LogError("Percentage must be between 0 and 100 included");
                return "";
            }

            if (String.IsNullOrEmpty(m_sMyProcId))
            {
                _logger.LogError("Own process Id not available");
                return "";
            }

            string sStatus = "RUNNING";

            try
            {
                ProcessWorkspace processWorkspace = _processWorkspaceService.UpdateProcessStatus(GetWorkspaceBaseUrl(), m_sSessionId, m_sMyProcId, sStatus, iPerc);

                if (processWorkspace != null)
                    return processWorkspace.Status;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "";
        }

        public string WaitProcess(string sProcessId)
        {
            if (String.IsNullOrEmpty(sProcessId))
            {
                _logger.LogError("WaitProcess: sProcessId is null or empty");
                return "";
            }

            UpdateStatus("WAITING");

            string sStatus = "";

            while (!(sStatus == "DONE" || sStatus == "STOPPED" || sStatus == "ERROR"))
            {
                sStatus = GetProcessStatus(sProcessId);

                if (!IsThisAValidStatus(sStatus))
                {
                    _logger.LogError("WaitProcess: the returned status \"" + sStatus + "\" is not valid, please check the process ID you passed. Aborting");
                    return "";
                }

                Thread.Sleep(2000);
            }

            if (WaitForResume())
                return sStatus;
            else
                return "";
        }


        private static bool IsThisAValidStatus(string sStatus)
        {
            return sStatus == "CREATED"
                || sStatus == "RUNNING"
                || sStatus == "DONE"
                || sStatus == "STOPPED"
                || sStatus == "ERROR"
                || sStatus == "WAITING"
                || sStatus == "READY";
        }

        public List<string> WaitProcesses(List<string> asIds)
        {
            _logger.LogDebug("WaitProcesses({0})", asIds);

            if (asIds == null)
            {
                _logger.LogError("WaitProcesses: passed a null list, aborting");
                return null;
            }

            if (asIds.Count <= 0)
            {
                _logger.LogError("WaitProcesses: list is empty, returning immediately");
                return new List<string>();
            }

            UpdateStatus("WAITING");

            bool bDone = false;
            while (!bDone)
            {
                bDone = true;

                List<string> asStatus = GetProcessesStatusAsList(asIds);

                if (asStatus == null)
                {
                    _logger.LogError("WaitProcesses: status list after getProcessesStatusAsList is null, aborting");
                    WaitForResume();
                    return null;
                }

                if (asStatus.Count != asIds.Count)
                {
                    _logger.LogWarning("WaitProcesses: warning: status list after getProcessesStatusAsList has size " + asStatus.Count + " instead of " + asIds.Count + ", please check the process IDs you passed");
                }

                if (asStatus.Count <= 0)
                {
                    _logger.LogInformation("WaitProcesses: status list after getProcessesStatusAsList is empty, please check the IDs you passed. Aborting");

                    if (WaitForResume())
                        return new List<string>(asIds.Count);
                    else
                        return null;
                }

                // ok we're good, check the statuses
                foreach (string sStatus in asStatus)
                {
                    if (!IsThisAValidStatus(sStatus))
                    {
                        _logger.LogError("WaitProcesses: got \"" + sStatus + "\" which is not a valid status, skipping it (please check the IDs you passed)");


                        //canonot assign to 'sStatus' because it is a 'foreach iteration variable'
                        /*
                        //set it temporary to error
                        sStatus = "ERROR";
                        */

                    }

                    if (!(sStatus == "DONE" || sStatus == "STOPPED" || sStatus == "ERROR"))
                    {
                        bDone = false;
                        //break: there's at least one process for which we need to wait
                        break;
                    }
                }

                if (!bDone)
                {
                    //then at least one needs to be waited for
                    _logger.LogInformation("waitProcesses: sleep");
                    Thread.Sleep(2000);
                }

            }

            if (WaitForResume())
                return GetProcessesStatusAsList(asIds);
            else
                return null;
        }

        protected bool WaitForResume()
        {
            if (!m_bIsOnServer)
                return true;

            string sStatus = "READY";
            UpdateStatus(sStatus);

            while (true)
            {
                sStatus = GetProcessStatus(GetMyProcId());

                switch (sStatus)
                {
                    case "WAITING":
                    case "CREATED":
                        WasdiLog("WaitForResume: " + "found status " + sStatus + " and this should not be.");
                        sStatus = "READY";
                        UpdateStatus(sStatus);

                        //Control cannot fall through from one case label to another
                        Thread.Sleep(2000);
                        break;

                    case "READY":
                        Thread.Sleep(2000);
                        break;
                    case "RUNNING":
                        return true;
                    case "DONE":
                    case "ERROR":
                    case "STOPPED":
                        WasdiLog("WaitForResume: " + "found status " + sStatus + " and this should not be.");
                        return false;
                    default:
                        WasdiLog("waitForResume: " + "found unknown status " + sStatus + ". Please report this issue to the WASDI admin.");
                        return false;
                }

            }
        }

        public void SetPayload(string sData)
        {
            if (String.IsNullOrEmpty(sData))
            {
                _logger.LogError("SetPayload: null or empty payload, aborting");
                return;
            }

            if (GetIsOnServer())
                SetProcessPayload(GetMyProcId(), sData);
            else
                _logger.LogDebug("setPayload: " + sData);

        }

        public string SetProcessPayload(string sProcessId, string sData)
        {
            if (String.IsNullOrEmpty(sProcessId))
            {
                _logger.LogError("SetPayload: sProcessId must not be null or empty");
            }

            if (!m_bIsOnServer)
                return "RUNNING";


            try
            {
                ProcessWorkspace processWorkspace = _processWorkspaceService.UpdateProcessPayload(GetWorkspaceBaseUrl(), GetSessionId(), sProcessId, sData);

                if (processWorkspace != null)
                    return processWorkspace.Status;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "";
        }

        public void RefreshParameters()
        {
            if (File.Exists(m_sParametersFilePath))
            {
                var sParametersJson = File.ReadAllText(m_sParametersFilePath);
                m_aoParams = SerializationHelper.FromJson<Dictionary<string, string>>(sParametersJson);
            }
        }

        protected string InternalAddFileToWASDI(string sFileName, Boolean bAsynch)
        {
            return InternalAddFileToWASDI(sFileName, bAsynch, null);
        }

        protected string InternalAddFileToWASDI(string sFileName, Boolean bAsynch, string sStyle)
        {
            if (String.IsNullOrEmpty(sFileName))
            {
                _logger.LogError("InternalAddFileToWASDI: sFileName must not be null or empty");
                return "";
            }

            try
            {

                string sFilePath = GetSavePath() + sFileName;

                Boolean bFileExists = File.Exists(sFilePath);

                if (!m_bIsOnServer)
                {
                    if (GetUploadActive())
                    {
                        if (bFileExists)
                        {
                            if (!FileExistsOnWasdi(sFileName))
                            {
                                _logger.LogInformation("InternalAddFileToWASDI: Remote file Missing. Start WASDI upload. Please wait");
                                UploadFile(sFileName);
                                _logger.LogInformation("InternalAddFileToWASDI: File Uploaded on WASDI cloud, keep on working!");
                            }
                        }
                    }
                }
                else
                {
                    if (bFileExists)
                    {
                        if (!FileExistsOnNode(sFileName))
                        {
                            _logger.LogInformation("InternalAddFileToWASDI: Remote file Missing. Start WASDI upload. Please wait");
                            UploadFile(sFileName);
                            _logger.LogInformation("InternalAddFileToWASDI: File Uploaded on WASDI cloud, keep on working!");
                        }
                    }
                }

                PrimitiveResult primitiveResult = _wasdiService.CatalogUploadIngest(GetWorkspaceBaseUrl(), m_sSessionId, m_sActiveWorkspace, sFileName, sStyle);

                if (primitiveResult == null)
                {
                    return "";
                }

                string sProcessId = primitiveResult.StringValue;

                if (bAsynch)
                    return sProcessId;
                else
                    return WaitProcess(sProcessId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
                return "";
            }
        }

        public string AddFileToWASDI(string sFileName, string sStyle)
        {
            return InternalAddFileToWASDI(sFileName, false, sStyle);
        }

        public string AsynchAddFileToWASDI(string sFileName, string sStyle)
        {
            return InternalAddFileToWASDI(sFileName, true, sStyle);
        }

        public string AddFileToWASDI(string sFileName)
        {
            return InternalAddFileToWASDI(sFileName, false);
        }

        public string AsynchAddFileToWASDI(string sFileName)
        {
            return InternalAddFileToWASDI(sFileName, true);
        }

        protected string InternalMosaic(bool bAsynch, List<string> asInputFiles, string sOutputFile)
        {
            return InternalMosaic(bAsynch, asInputFiles, sOutputFile, null, null);
        }

        protected string InternalMosaic(bool bAsynch, List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue)
        {
            return InternalMosaic(bAsynch, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, -1.0, -1.0);
        }

        protected string InternalMosaic(bool bAsynch, List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue, double dPixelSizeX, double dPixelSizeY)
        {
            _logger.LogDebug("InternalMosaic()");

            if (asInputFiles == null)
            {
                _logger.LogError("InternalMosaic: asInputFiles must not be null");
                return "";
            }

            if (asInputFiles.Count == 0)
            {
                _logger.LogError("InternalMosaic: asInputFiles must not be empty");
                return "";
            }

            if (sOutputFile == null)
            {
                _logger.LogError("InternalMosaic: sOutputFile must not be null");
                return "";
            }

            if (sOutputFile == "")
            {
                _logger.LogError("InternalMosaic: sOutputFile must not empty string");
                return "";
            }

            // Build API URL
            string sUrl = m_sBaseUrl + "/processing/mosaic?name=" + sOutputFile + "&workspace=" + m_sActiveWorkspace;

            // Output Format: "GeoTIFF" and BEAM supported
            string sOutputFormat = "GeoTIFF";
            if (sOutputFile.EndsWith(".dim"))
                sOutputFormat = "BEAM-DIMAP";

            // Fill the Setting Object
            MosaicSetting oMosaicSetting = new MosaicSetting();

            int? oNoDataValue = ToInt(sNoDataValue);
            if (oNoDataValue.HasValue)
                oMosaicSetting.NoDataValue = oNoDataValue.Value;

            int? oInputIgnoreValue = ToInt(sInputIgnoreValue);
            if (oInputIgnoreValue.HasValue)
                oMosaicSetting.InputIgnoreValue = oInputIgnoreValue.Value;

            oMosaicSetting.PixelSizeX = dPixelSizeX;
            oMosaicSetting.PixelSizeY = dPixelSizeY;
            oMosaicSetting.OutputFormat = sOutputFormat;

            oMosaicSetting.Sources = asInputFiles;


            try
            {
                PrimitiveResult primitiveResult = _wasdiService.ProcessingMosaic(sUrl, m_sSessionId, oMosaicSetting);

                // Extract Process Id
                string sProcessId = String.Empty;

                if (primitiveResult != null)
                    sProcessId = primitiveResult.StringValue;

                // Return or wait
                if (bAsynch)
                    return sProcessId;
                else
                    return WaitProcess(sProcessId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
                return "";
            }
        }

        private int? ToInt(string value)
        {
            try
            {
                //return Int32.Parse(value);
                return Convert.ToInt32(value);
            }
            catch (System.ArgumentNullException)
            {
                _logger.LogError("ToInt64: the string value is null, set null");
                return null;
            }
            catch (System.FormatException)
            {
                _logger.LogError("ToInt64: the string value is not a valid integer, set null");
                return null;
            }

        }

        public string Mosaic(List<string> asInputFiles, string sOutputFile)
        {
            return InternalMosaic(false, asInputFiles, sOutputFile);
        }

        public string Mosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue)
        {
            return InternalMosaic(false, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue);
        }

        public string Mosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue, double dPixelSizeX, double dPixelSizeY)
        {
            return InternalMosaic(false, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, dPixelSizeX, dPixelSizeY);
        }

        public string AsynchMosaic(List<string> asInputFiles, string sOutputFile)
        {
            return InternalMosaic(true, asInputFiles, sOutputFile);
        }

        public string AsynchMosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue)
        {
            return InternalMosaic(true, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue);
        }

        public string AsynchMosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue, double dPixelSizeX, double dPixelSizeY)
        {
            return InternalMosaic(true, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, dPixelSizeX, dPixelSizeY);
        }

        public List<QueryResultViewModel> SearchEOImages(string sPlatform, string sDateFrom, string sDateTo, Double dULLat, Double dULLon, Double dLRLat, Double dLRLon,
            string sProductType, int? iOrbitNumber, string? sSensorOperationalMode, string? sCloudCoverage)
        {
            _logger.LogDebug("SearchEOImages()( " + sPlatform + ", [ " + sDateFrom + ", " + sDateTo + " ], " +
                "[ " + dULLat + ", " + dULLon + ", " + dLRLat + ", " + dLRLon + " ], " +
                "product type: " + sProductType + ", " +
                "orbit: " + iOrbitNumber + ", " +
                "sensor mode: " + sSensorOperationalMode + ", " +
                "cloud coverage: " + sCloudCoverage);

            List<QueryResultViewModel> aoReturnList = new List<QueryResultViewModel>();

            if (sPlatform == null)
            {
                _logger.LogError("SearchEOImages: platform cannot be null");
                return aoReturnList;
            }

            if (!(sPlatform.Equals("S1") || sPlatform.Equals("S2") || sPlatform.Equals("S3") || sPlatform.Equals("S5P") || sPlatform.Equals("ENVI") || sPlatform.Equals("VIIRS") || sPlatform.Equals("ERA5") || sPlatform.Equals("L8")))
                _logger.LogError("SearchEOImages: platform should be one of S1, S2, S3, S5P, ENVI, VIIRS, ERA5, L8. Received [" + sPlatform + "]");

            if (sPlatform.Equals("S1"))
                if (sProductType != null)
                    if (!(sProductType.Equals("SLC") || sProductType.Equals("GRD") || sProductType.Equals("OCN")))
                        _logger.LogError("SearchEOImages: Available Product Types for S1; SLC, GRD, OCN. Received [" + sProductType + "]");

            if (sPlatform.Equals("S2"))
                if (sProductType != null)
                    if (!(sProductType.Equals("S2MSI1C") || sProductType.Equals("S2MSI2Ap") || sProductType.Equals("S2MSI2A")))
                        _logger.LogError("SearchEOImages: Available Product Types for S2; S2MSI1C, S2MSI2Ap, S2MSI2A. Received [" + sProductType + "]");

            if (sPlatform.Equals("S3"))
                if (sProductType != null)
                    if (!(sProductType.Equals("SR_1_SRA___") || sProductType.Equals("SR_1_SRA_A_") || sProductType.Equals("SR_1_SRA_BS") || sProductType.Equals("SR_2_LAN___")))
                        _logger.LogError("SearchEOImages: Available Product Types for S3; SR_1_SRA___, SR_1_SRA_A_, SR_1_SRA_BS, SR_2_LAN___. Received [" + sProductType + "]");

            if (sDateFrom == null)
            {
                _logger.LogError("SearchEOImages: sDateFrom cannot be null");
                return aoReturnList;
            }

            if (sDateFrom.Length < 10)
            {
                _logger.LogError("SearchEOImages: sDateFrom must be in format YYYY-MM-DD");
                return aoReturnList;
            }

            if (sDateTo == null)
            {
                _logger.LogError("SearchEOImages: sDateTo cannot be null");
                return aoReturnList;
            }

            if (sDateTo.Length < 10)
            {
                _logger.LogError("SearchEOImages: sDateTo must be in format YYYY-MM-DD");
                return aoReturnList;
            }


            // Create Query String:

            // Platform name for sure
            string sQuery = "( platformname:";
            if (sPlatform.Equals("S1")) sQuery += "Sentinel-1";
            else if (sPlatform.Equals("S2")) sQuery += "Sentinel-2";
            else if (sPlatform.Equals("S3")) sQuery += "Sentinel-3";
            else if (sPlatform.Equals("S5P")) sQuery += "Sentinel-5P";
            else if (sPlatform.Equals("ENVI")) sQuery += "Envisat";
            else if (sPlatform.Equals("L8")) sQuery += "Landsat-*";
            else if (sPlatform.Equals("VIIRS")) sQuery += "VIIRS";
            else if (sPlatform.Equals("ERA5")) sQuery += "ERA5";
            else sQuery += sPlatform;

            // If available add product type
            if (sProductType != null)
                sQuery += " AND producttype:" + sProductType;
            else if (sPlatform.Equals("VIIRS"))
                sQuery += " AND producttype:VIIRS_1d_composite";

            // If available Sensor Operational Mode
            if (sSensorOperationalMode != null && sPlatform.Equals("S1"))
                sQuery += " AND sensoroperationalmode:" + sSensorOperationalMode;

            // If available cloud coverage
            if (sCloudCoverage != null && sCloudCoverage != "" && sPlatform.Equals("S2"))
                sQuery += " AND cloudcoverpercentage:" + sCloudCoverage;

            // If available add orbit number
            if (iOrbitNumber != null)
                sQuery += " AND relativeorbitnumber:" + iOrbitNumber;

            // Close the first block
            sQuery += ") ";

            // Date Block
            sQuery += "AND ( beginPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]";
            sQuery += "AND ( endPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]";

            // Close the second block
            sQuery += ") ";

            if (dULLat != null && dULLon != null && dLRLat != null && dLRLon != null)
            {
                string sFootPrint = "( footprint:\"intersects(POLYGON(( " + dULLon + " " + dLRLat + "," + dULLon + " " + dULLat + "," + dLRLon + " " + dULLat + "," + dLRLon + " " + dLRLat + "," + dULLon + " " + dLRLat + ")))\") AND ";
                sQuery = sFootPrint + sQuery;
            }

            String sQueryBody = "[\"" + sQuery.Replace("\"", "\\\"") + "\"]";
            sQuery = "providers=" + GetDefaultProvider();

            String sUrl = m_sBaseUrl + "/search/querylist?" + sQuery;

            try
            {
                aoReturnList = _wasdiService.SearchQueryList(sUrl, m_sSessionId, sQueryBody);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return aoReturnList;
        }

        public string GetFoundProductName(QueryResultViewModel oProduct)
        {
            if (oProduct == null)
                return "";

            return oProduct.Title;
        }

        public string GetFoundProductName(Dictionary<string, object> oProduct)
        {
            if (oProduct == null)
                return "";

            if (!oProduct.ContainsKey("title"))
                return "";

            return (string)oProduct["Title"];
        }

        public string GetFoundProductLink(QueryResultViewModel oProduct)
        {
            if (oProduct == null)
                return "";

            return oProduct.Link;
        }

        public string GetFoundProductLink(Dictionary<string, object> oProduct)
        {
            if (oProduct == null)
                return "";

            if (!oProduct.ContainsKey("link"))
                return "";

            return (string)oProduct["link"];
        }

        public string GetFoundProductFootprint(QueryResultViewModel oProduct)
        {
            if (oProduct == null)
                return "";

            return oProduct.Footprint;
        }

        public string GetFoundProductFootprint(Dictionary<string, object> oProduct)
        {
            if (oProduct == null)
                return "";

            if (!oProduct.ContainsKey("footprint"))
                return "";

            return (string)oProduct["footprint"];
        }

        public string AsynchImportProduct(Dictionary<string, object> oProduct)
        {
            _logger.LogDebug("AsynchImportProduct (with map)");

            return AsynchImportProduct(oProduct, null);
        }

        public string AsynchImportProduct(Dictionary<string, object> oProduct, string sProvider)
        {
            _logger.LogDebug("AsynchImportProduct ( oProduct, " + sProvider + " )");

            string sReturn = "ERROR";

            try
            {
                // Get URL And Bounding Box from the JSON representation
                string sFileUrl = GetFoundProductLink(oProduct);
                string sBoundingBox = GetFoundProductFootprint(oProduct);
                string sName = GetFoundProductName(oProduct);

                return AsynchImportProduct(sFileUrl, sName, sBoundingBox, sProvider);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return sReturn;
        }

        public string ImportProduct(Dictionary<string, object> oProduct)
        {
            _logger.LogDebug("ImportProduct()");

            string sReturn = "ERROR";

            try
            {
                // Get URL And Bounding Box from the JSON representation
                string sFileUrl = GetFoundProductLink(oProduct);
                string sBoundingBox = GetFoundProductFootprint(oProduct);
                string sName = GetFoundProductName(oProduct);

                return ImportProduct(sFileUrl, sName, sBoundingBox);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return sReturn;
        }

        public string ImportProduct(QueryResultViewModel oProduct)
        {
            _logger.LogDebug("ImportProduct()");

            string sReturn = "ERROR";

            try
            {
                // Get URL And Bounding Box from the JSON representation
                string sFileUrl = oProduct.Link;
                string sBoundingBox = oProduct.Footprint;
                string sName = oProduct.Title;

                return ImportProduct(sFileUrl, sName, sBoundingBox);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return sReturn;
        }

        public string ImportProduct(string sFileUrl, string sName)
        {
            return ImportProduct(sFileUrl, sName, "");
        }

        public string AsynchImportProduct(string sFileUrl, string sName)
        {
            return AsynchImportProduct(sFileUrl, sName, "");
        }

        public string AsynchImportProduct(string sFileUrl, string sName, string sBoundingBox)
        {
            return AsynchImportProduct(sFileUrl, sName, sBoundingBox, null);
        }

        public string AsynchImportProduct(string sFileUrl, string sFileName, string sBoundingBox, string sProvider)
        {
            _logger.LogDebug("AsynchImportProduct()");

            String sReturn = "ERROR";

            try
            {
                if (String.IsNullOrEmpty(sProvider))
                    sProvider = GetDefaultProvider();

                PrimitiveResult primitiveResult =
                    _wasdiService.FilebufferDownload(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sProvider, sFileUrl, sFileName, sBoundingBox);

                if (primitiveResult != null)
                    if (primitiveResult.BoolValue)
                        sReturn = primitiveResult.StringValue;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return sReturn;
        }

        public string ImportProduct(string sFileUrl, string sName, string sBoundingBox)
        {
            _logger.LogDebug("AsynchImportProduct()");

            return ImportProduct(sFileUrl, sName, sBoundingBox, null);
        }
        public string ImportProduct(string sFileUrl, string sName, string sBoundingBox, string sProvider)
        {
            _logger.LogDebug("ImportProduct()");

            string sReturn = "ERROR";

            try
            {
                string sProcessId = AsynchImportProduct(sFileUrl, sName, sBoundingBox, sProvider);
                sReturn = WaitProcess(sProcessId);

                // Return the status of the import WASDI process
                return sReturn;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return sReturn;
        }
        public List<string> AsynchImportProductListWithMaps(List<Dictionary<string, object>> aoProductsToImport)
        {
            _logger.LogDebug("AsynchImportProductListWithMaps()");

            if (aoProductsToImport == null)
            {
                return null;
            }

            List<string> asIds = new List<string>(aoProductsToImport.Count);
            foreach (Dictionary<string, object> oProduct in aoProductsToImport)
            {
                asIds.Add(AsynchImportProduct(oProduct));
            }

            return asIds;
        }

        public List<string> AsynchImportProductList(List<string> asProductsToImport, List<string> asNames)
        {
            _logger.LogDebug("AsynchImportProductList ( with list )");

            if (null == asProductsToImport)
            {
                _logger.LogError("AsynchImportProductList: list is null, aborting");
                return null;
            }

            if (asProductsToImport.Count <= 0)
            {
                _logger.LogError("AsynchImportProductList: list has no elements, aborting");
                return null;
            }

            _logger.LogDebug("AsynchImportProductList: list has " + asProductsToImport.Count + " elements");

            List<string> asIds = new List<string>(asProductsToImport.Count);

            for (int i = 0; i < asProductsToImport.Count; i++)
            {
                string sProductUrl = asProductsToImport.ElementAt(i);
                string sName = null;

                if (asNames != null)
                {
                    if (i < asNames.Count)
                    {
                        sName = asNames.ElementAt(i);
                    }
                }

                asIds.Add(AsynchImportProduct(sProductUrl, sName));
            }

            return asIds;
        }

        public List<string> ImportProductListWithMaps(List<Dictionary<string, object>> aoProductsToImport)
        {
            return WaitProcesses(AsynchImportProductListWithMaps(aoProductsToImport));
        }

        public List<string> ImportProductList(List<string> asProductsToImport, List<string> asNames)
        {
            return WaitProcesses(AsynchImportProductList(asProductsToImport, asNames));
        }

        public string Subset(string sInputFile, string sOutputFile, double dLatN, double dLonW, double dLatS, double dLonE)
        {
            _logger.LogDebug("Subset()");


            // Check minimun input values
            if (sInputFile == null)
            {
                _logger.LogError("input file must not be null");
                return "";
            }

            if (sInputFile == "")
            {
                _logger.LogError("input file must not be empty");
                return "";
            }

            if (sOutputFile == null)
            {
                _logger.LogError("sOutputFile must not be null");
                return "";
            }

            if (sOutputFile == "")
            {
                _logger.LogError("sOutputFile must not empty string");
                return "";
            }


            // Fill the Setting Object
            string sSubsetSetting = "{ \"latN\":" + dLatN + ", \"lonW\":" + dLonW + ", \"latS\":" + dLatS + ", \"lonE\":" + dLonE + " }";


            try
            {
                PrimitiveResult primitiveResult = _wasdiService.ProcessingSubset(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sInputFile, sOutputFile, sSubsetSetting);

                if (primitiveResult != null)
                {
                    string sProcessId = primitiveResult.StringValue;
                    if (sProcessId != null)
                    {
                        // Return process output status
                        return WaitProcess(sProcessId);
                    }
                }

            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "ERROR";
        }

        public string MultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE)
        {
            _logger.LogDebug("MultiSubset( " + sInputFile + ", asOutputFiles, adLatN, adLonW, adLatS, adLonE )");
            return WaitProcess(AsynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE));
        }

        public string MultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE, bool bBigTiff)
        {
            _logger.LogDebug("MultiSubset( " + sInputFile + ", asOutputFiles, adLatN, adLonW, adLatS, adLonE, " + bBigTiff + " )");
            return WaitProcess(AsynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE, bBigTiff));
        }

        public string AsynchMultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE)
        {
            _logger.LogDebug("AsynchMultiSubset( " + sInputFile + ", asOutputFiles, adLatN, adLonW, adLatS, adLonE )");
            return AsynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE, true);
        }

        public string AsynchMultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE, bool bBigTiff)
        {
            _logger.LogDebug("AsynchMultiSubset( " + sInputFile + ", asOutputFiles, adLatN, adLonW, adLatS, adLonE, " + bBigTiff + " )");


            if (String.IsNullOrEmpty(sInputFile))
            {
                _logger.LogError("MultiSubset: input file null or empty, aborting");
                return null;
            }

            if (asOutputFiles == null || asOutputFiles.Count == 0)
            {
                _logger.LogError("Multisubset: output files null or empty, aborting");
                return null;
            }

            if (adLonW == null || adLonW.Count == 0)
            {
                _logger.LogError("Multisubset: adLonW null or empty, aborting");
                return null;
            }

            if (adLonW == null || adLonW.Count == 0)
            {
                _logger.LogError("Multisubset: adLonW null or empty, aborting");
                return null;
            }

            if (adLatS == null || adLatS.Count == 0)
            {
                _logger.LogError("Multisubset: adLatS null or empty, aborting");
                return null;
            }

            if (adLonE == null || adLonE.Count == 0)
            {
                _logger.LogError("Multisubset: adLonE null or empty, aborting");
                return null;
            }

            Dictionary<string, object> aoPayload = new Dictionary<string, object>();
            aoPayload.Add("outputNames", asOutputFiles);
            aoPayload.Add("latNList", adLatN);
            aoPayload.Add("lonWList", adLonW);
            aoPayload.Add("latSList", adLatS);
            aoPayload.Add("lonEList", adLonE);

            if (bBigTiff)
                aoPayload.Add("bigTiff", true);


            bool bIsOnServer = GetIsOnServer();
            string sProcessId = GetMyProcId();

            try
            {
                PrimitiveResult primitiveResult = _wasdiService.ProcessingMultisubset(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, bIsOnServer, sInputFile, sProcessId, aoPayload);

                if (primitiveResult != null)
                    return primitiveResult.StringValue;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return null;
        }

        public string ExecuteProcessor(string sProcessorName, Dictionary<string, object> aoParams)
        {
            return WaitProcess(AsynchExecuteProcessor(sProcessorName, aoParams));
        }

        public string AsynchExecuteProcessor(string sProcessorName, Dictionary<string, object> aoParams)
        {
            _logger.LogDebug("AsynchExecuteProcessor( " + sProcessorName + ", Map<string, object> aoParams )");

            try
            {
                // Initialize
                string sParamsJson = "";

                // Convert dictionary in a JSON
                if (aoParams != null)
                    sParamsJson = SerializationHelper.ToJson(aoParams);

                // Encode the params
                sParamsJson = HttpUtility.UrlEncode(sParamsJson);

                //var formUrlEncodedContent = new FormUrlEncodedContent(aoParams);
                //sParamsJson = formUrlEncodedContent.ReadAsStringAsync().Result;

                // Use the string version
                return AsynchExecuteProcessor(sProcessorName, sParamsJson);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
                return "";
            }
        }


        public string ExecuteProcessor(string sProcessorName, string sEncodedParams)
        {
            return WaitProcess(AsynchExecuteProcessor(sProcessorName, sEncodedParams));
        }

        public string AsynchExecuteProcessor(string sProcessorName, string sEncodedParams)
        {

            _logger.LogDebug("AsynchExecuteProcessor( " + sProcessorName + ", " + sEncodedParams + " )");

            //Domain check
            if (sProcessorName == null)
            {
                _logger.LogError("ProcessorName is null, return");
                return "";
            }

            if (sProcessorName == "")
            {
                _logger.LogError("ProcessorName is empty, return");
                return "";
            }

            if (sEncodedParams == null)
            {
                _logger.LogError("EncodedParams is null, return");
                return "";
            }

            RunningProcessorViewModel primitiveResult = _wasdiService.ProcessorsRun(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sProcessorName, sEncodedParams);

            if (primitiveResult != null)
            {
                string sProcessId = primitiveResult.ProcessingIdentifier;
                return sProcessId;
            }

            return "";
        }

        public string DeleteProduct(string sProduct)
        {
            _logger.LogDebug("DeleteProduct({0})", sProduct);

            try
            {
                PrimitiveResult primitiveResult = _productService.DeleteProduct(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, sProduct);
            
                if (primitiveResult != null)
                    return primitiveResult.StringValue;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "ERROR";
        }

        public void WasdiLog(string sLogRow)
        {
            if (m_bIsOnServer)
            {
                // Check minimun input values
                if (sLogRow == null)
                {
                    _logger.LogError("Log line null, aborting");
                    return;
                }

                if (sLogRow == "")
                {
                    _logger.LogError("Log line empty, aborting");
                    return;
                }


                try
                {
                    _wasdiService.AddProcessorsLog(GetWorkspaceBaseUrl(), m_sSessionId, GetMyProcId(), sLogRow);
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex.StackTrace);
                }
            }
            else
                _logger.LogInformation(sLogRow);
        }

        public string CreateWorkspace(string workspaceName, string nodeCode = null)
        {
            _logger.LogDebug("CreateWorkspace({0}, {1})", workspaceName, nodeCode);

            if (workspaceName == null)
            {
                workspaceName = "";
            }

            if (workspaceName == "")
            {
                _logger.LogInformation("CreateWorkspace: WARNING workspace name null or empty, it will be defaulted");
            }

            String sReturn = null;

            try
            {
                PrimitiveResult primitiveResult = _workspaceService.CreateWorkspace(m_sBaseUrl, m_sSessionId, workspaceName, nodeCode);

                if (primitiveResult != null)
                    sReturn = primitiveResult.StringValue;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }


            if (sReturn != null)
                OpenWorkspaceById(sReturn);
            else
                _logger.LogError("CreateWorkspace: creation failed.");

            return sReturn;
        }

        public string DeleteWorkspace(string workspaceId)
        {
            _logger.LogDebug("DeleteWorkspace({0})", workspaceId);

            if (String.IsNullOrEmpty(workspaceId))
            {
                _logger.LogError("DeleteWorkspace: none passed, aborting");
                return null;
            }

            string sResult = null;

            try
            {
                sResult = _workspaceService.DeleteWorkspace(m_sBaseUrl, m_sSessionId, workspaceId);

                if (sResult == null)
                    _logger.LogError("DeleteWorkspace: could not delete workspace (please check the return value, it's going to be null)");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return sResult;
        }

        public List<ProcessWorkspace> GetProcessWorkspacesByWorkspaceId(string workspaceId)
        {
            _logger.LogDebug("GetProcessWorkspacesByWorkspaceId({0})", workspaceId);

            List<ProcessWorkspace> ProcessWorkspaceList = new List<ProcessWorkspace>();

            try
            {
                ProcessWorkspaceList = _processWorkspaceService.GetProcessWorkspacesByWorkspaceId(GetWorkspaceBaseUrl(), m_sSessionId, workspaceId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return ProcessWorkspaceList;
        }

        public List<string> GetProcessesByWorkspaceAsListOfJson(int iStartIndex, Int32 iEndIndex, string sStatus, string sOperationType, string sNamePattern)
        {
            _logger.LogDebug("GetProcessesByWorkspaceAsListOfJson({0}, {1}, {2}, {3}, {4})", iStartIndex, iEndIndex, sStatus, sOperationType, sNamePattern);

            List<ProcessWorkspace> ProcessWorkspaceList = GetProcessesByWorkspace(iStartIndex, iEndIndex, sStatus, sOperationType, sNamePattern);

            List<string> asJson = new List<string>(ProcessWorkspaceList.Count);

            foreach (ProcessWorkspace processWorkspace in ProcessWorkspaceList)
                asJson.Add(SerializationHelper.ToJson(processWorkspace));

            return asJson;
        }

        public List<ProcessWorkspace> GetProcessesByWorkspace(int iStartIndex, Int32 iEndIndex, string sStatus, string sOperationType, string sNamePattern)
        {
            _logger.LogDebug("GetProcessesByWorkspace({0}, {1}, {2}, {3}, {4}, {5})", m_sActiveWorkspace, iStartIndex, iEndIndex, sStatus, sOperationType, sNamePattern);

            List<ProcessWorkspace> processWorkspaceList = new List<ProcessWorkspace>();

            try
            {
                processWorkspaceList = _processWorkspaceService
                    .GetProcessWorkspacesByWorkspace(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, iStartIndex, iEndIndex, sStatus, sOperationType, sNamePattern);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return processWorkspaceList;
        }

        public Dictionary<string, object> GetProcessorPayload(string sProcessObjId)
        {
            _logger.LogDebug("GetProcessesByWorkspace({0}", sProcessObjId);

            if (String.IsNullOrEmpty(sProcessObjId))
            {
                _logger.LogError("GetProcessorPayload: the processor ID is null or empty");
                return null;
            }

            string sJsonPayload = GetProcessorPayloadAsJSON(sProcessObjId);

            if (sJsonPayload == null)
                return null;

            return SerializationHelper.FromJson<Dictionary<string, object>>(sJsonPayload);
        }

        public string GetProcessorPayloadAsJSON(string sProcessObjId)
        {
            _logger.LogDebug("GetProcessorPayloadAsJSON({0}", sProcessObjId);

            if (String.IsNullOrEmpty(sProcessObjId))
            {
                _logger.LogError("GetProcessorPayloadAsJSON: the processor ID is null or empty");
                return null;
            }

            try
            {
                return _processWorkspaceService.GetProcessPayload(m_sWorkspaceBaseUrl, m_sSessionId, sProcessObjId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
                return null;
            }
        }

        public string GetProductBbox(string sFileName)
        {
            _logger.LogDebug("GetProductBbox({0}", sFileName);

            if (String.IsNullOrEmpty(sFileName))
            {
                _logger.LogError("GetProductBBOX: file name is null or empty, aborting");
                return null;
            }

            try
            {
                Product product = _productService.GetProductByName(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sFileName);

                if (product != null)
                    return product.Bbox;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return null;
        }

        private string DownloadFile(string sFileName)
        {
            _logger.LogDebug("DownloadFile({0})", sFileName);

            if (String.IsNullOrEmpty(sFileName))
            {
                _logger.LogError("DownloadFile: fileName must not be null or empty");
                return "";
            }

            string sSavePath = GetSavePath();

            try
            {
                string sOutputFilePath = _wasdiService.CatalogDownload(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, sSavePath, sFileName);

                return sOutputFilePath;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
            }

            return "";
        }


        public bool UploadFile(string sFileName)
        {
            _logger.LogDebug("UploadFile({0})", sFileName);

            if (String.IsNullOrEmpty(sFileName))
                throw new Exception("WasdiLib.UploadFile: file name is null or empty");

            //local file
            string sSavePath = GetSavePath();
            string sFullPath = sSavePath + sFileName;

            if (!File.Exists(sFullPath))
                throw new Exception("WasdiLib.UploadFile: file not found");

            bool success = _productService.UploadFile(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, sSavePath, sFileName);
            _logger.LogDebug("UploadFile " + (success ? "succeeded" : "failed"));

            return success;
        }

        public string CopyFileToSftp(string sFileName)
        {
            _logger.LogDebug("CopyFileToSftp( " + sFileName + " )");

            return CopyFileToSftp(sFileName, null);
        }

        public string CopyFileToSftp(string sFileName, string sRelativePath)
        {
            _logger.LogDebug("CopyFileToSftp( " + sFileName + ", " + sRelativePath + " )");

            return WaitProcess(AsynchCopyFileToSftp(sFileName, sRelativePath));
        }

        public string AsynchCopyFileToSftp(string sFileName)
        {
            return AsynchCopyFileToSftp(sFileName, null);
        }

        public string AsynchCopyFileToSftp(string sFileName, string sRelativePath)
        {
            _logger.LogDebug("AsynchCopyFileToSftp({0}, {1})", sFileName, sRelativePath);

            if (String.IsNullOrEmpty(sFileName))
            {
                _logger.LogError("AsynchCopyFileToSftp: invalid file name, aborting");
                return null;
            }

            //upload file if it is not on WASDI yet
            try
            {
                if (GetUploadActive())
                {
                    if (!FileExistsOnWasdi(sFileName))
                    {
                        _logger.LogDebug("AsynchCopyFileToSftp: file " + sFileName + " is not on WASDI yet, uploading...");
                        UploadFile(sFileName);
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);
                return null;
            }

            bool bIsOnServer = GetIsOnServer();
            string sProcessId = GetMyProcId();

            try
            {
                PrimitiveResult primitiveResult = _wasdiService.AsynchCopyFileToSftp(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, bIsOnServer, sRelativePath, sFileName, sProcessId);

                if (primitiveResult != null)
                    return primitiveResult.StringValue;
            }
            catch (Exception ex)
            {
                _logger.LogError("AsynchCopyFileToSftp: could not HTTP GET to /catalog/copytosfpt due to: " + ex + ", aborting");
                return null;
            }

            return null;
        }

        public string SetSubPid(string sProcessId, int iSubPid)
        {
            _logger.LogDebug("SetSubPid({0}, {1})", sProcessId, iSubPid);

            if (String.IsNullOrEmpty(sProcessId))
            {
                _logger.LogError("SetSubPid: process ID null or empty, aborting");
                return "";
            }

            try
            {
                ProcessWorkspace processWorkspace = _processWorkspaceService.SetSubPid(m_sWorkspaceBaseUrl, m_sSessionId, sProcessId, iSubPid);

                if (processWorkspace != null)
                    return processWorkspace.Status;
            }
            catch (Exception ex)
            {
                _logger.LogError("SetSubPid: could not HTTP GET due to: " + ex + ", aborting");
                return "";
            }

            return "";
        }

    }
}

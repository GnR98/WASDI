﻿using WasdiLib.Models;


namespace WasdiLib.Services
{
    internal interface IProcessWorkspaceService
    {
        List<ProcessWorkspace> GetProcessWorkspacesByWorkspaceId(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId);
        List<ProcessWorkspace> GetProcessWorkspacesByWorkspace(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId,
            int iStartIndex, Int32 iEndIndex, string sStatus, string sOperationType, string sNamePattern);

        ProcessWorkspace GetProcessWorkspaceByProcessId(string sWorkspaceBaseUrl, string sSessionId, string sProcessId);

        string GetProcessesStatus(string sWorkspaceBaseUrl, string sSessionId, List<string> asIds);

        ProcessWorkspace UpdateProcessStatus(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sStatus, int iPerc);

        ProcessWorkspace UpdateProcessPayload(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sData);

    }
}

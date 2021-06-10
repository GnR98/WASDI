function sWorkspaceId=wCreateWorkspace(Wasdi, sWorkspaceName, sNodeCode='')%Copy file to SFTP folder, asynchronous version%Syntax%wCreateWorkspace(Wasdi, sWorkspaceName, sNodeCode)%%:param Wasdi: Wasdi object created after the wasdilib call%:param sWorkspaceName: the name of the workspace%:param sNodeCode: the code of the node, optional%%:Returns:%   :sWorkspaceId: the ID of the workspace (empty in case of error)  if exist("Wasdi") < 1     disp('Wasdi variable does not exist')    return   end      Wasdi.createWorkspace(sWorkspaceName, sNodeCode)end
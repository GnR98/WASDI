+wasdi
function sWorkspaceOwner = wGetWorkspaceOwnerByName(Wasdi, sWorkspaceName)
%
%Gets the owner of the workspace given its name
%Syntax
%sWorkspaceOwner = wGetWorkspaceOwnerByName(Wasdi, sWorkspaceName)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sWorkspaceName: the name of the workspace
%
%:Returns:
%  :sWorkspaceOwner: the owner of the workspace

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sWorkspaceOwner = Wasdi.getWorkspaceOwnerByName(sWorkspaceName);
   
end

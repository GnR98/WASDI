function sStatus = wDeleteWorkspace(Wasdi, sWorkspaceName)
% Deletes a workspace. If the user is not the workspace owner, then just the sharing is deleted
% Syntax
% sStatus = wDeleteProduct(Wasdi, sWorkspaceName)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sWorkspaceName: the name of the workspace to be deleted
%
% OUTPUT
%   sStatus: empty string if deletion was successful, null in case it did not work

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sStatus = char(Wasdi.deleteWorkspace(sWorkspaceName));
end
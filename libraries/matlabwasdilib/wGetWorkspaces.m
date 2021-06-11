% Copyright 2020 Paolo Campanella
% Copyright 2021 Cristiano Nattero
% Copyright 2021 Marco Menapace
%
% This file is part of the WASDI matlab and octave library.
% 
% The WASDI matlab and octave library is free software: you can 
% redistribute it and/or modify it under the terms of the GNU General 
% Public License as published by the Free Software Foundation, either 
% version 3 of the License, or (at your option) any later version.
% 
% The WASDI matlab and octave library is distributed in the hope that it 
% will be useful, but WITHOUT ANY WARRANTY; without even the implied 
% warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See 
% the GNU General Public License for more details.
% 
% You should have received a copy of the GNU General Public License along 
% with the WASDI matlab and octave library.  If not, see 
% <https://www.gnu.org/licenses/>.
%
function [asWorkspaceNames, asWorkspaceIds]=wGetWorkspaces(Wasdi);
%Get the List of Workspace of the actual User
%Syntax
%[asWorkspaceNames, asWorkspaceIds]=wGetWorkspaces(Wasdi);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%
%:Returns:
%  :asWorkspaceNames: array of strings that are the names of the workspaces
%  :asWorkspaceIds: array of strings that are the id of the workspaces

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   aoWorkspaces = Wasdi.getWorkspaces();
   
   iTotWs = aoWorkspaces.size();
   
   disp(['Number of Workspaces: ' sprintf("%d",iTotWs)]);

  %For each workspace
  for iWs = 0:iTotWs-1
    oWorkspace = aoWorkspaces.get(iWs);
    asWorkspaceNames{iWs+1} = oWorkspace.get("workspaceName");
    asWorkspaceIds{iWs+1} = oWorkspace.get("workspaceId");
  end

   
end

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
function sWorkspaceId=wCreateWorkspace(Wasdi, sWorkspaceName, sNodeCode='')%Copy file to SFTP folder, asynchronous version%Syntax%wCreateWorkspace(Wasdi, sWorkspaceName, sNodeCode)%%:param Wasdi: Wasdi object created after the wasdilib call%:param sWorkspaceName: the name of the workspace%:param sNodeCode: the code of the node, optional%%:Returns:%  :sWorkspaceId: the ID of the workspace (empty in case of error)  if exist("Wasdi") < 1     disp('Wasdi variable does not exist')    return   end      Wasdi.createWorkspace(sWorkspaceName, sNodeCode)end
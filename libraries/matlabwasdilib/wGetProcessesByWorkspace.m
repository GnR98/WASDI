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
function aoProcesses=wGetProcessesByWorkspace(Wasdi, iStartIndex=0, iEndIndex=[], sStatus=[], sOperationType=[], sNamePattern=[])%Get a paginated list of processes in the active workspace%Syntax%asProcesses=wgetProcessesByWorkspace(Wasdi, iStartIndex, iEndIndex=[], sStatus=[], sOperationType=[], sNamePattern=[]?)%%:param Wasdi: Wasdi object created after the wasdilib call%:param iStartIndex: first index%:param iEndIndex: last index%:param sStatus: filter by statuses%:param sOperationType: filter by operation name%:param sNamePattern: filter by name%%:Returns:%  :asProcesses: list of processes%  if exist("Wasdi") < 1     disp('Wasdi variable does not existst')    return   end      asJsonProcesses = Wasdi.getProcessesByWorkspaceAsListOfJson(iStartIndex, iEndIndex, sStatus, sOperationType, sNamePattern)      aoProcesses = []      for iProcess= 0:asJsonProcesses.size()-1    aoProcesses = [aoProcesses,loadjson(asJsonProcesses.get(iProcess))];   end   end
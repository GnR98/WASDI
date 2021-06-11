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
function sWorkspaceOwner = wGetWorkspaceOwnerByWsId(Wasdi, sWorkspaceId)
%
% Gets the owner of the workspace given its ID
% Syntax
% sWorkspaceOwner = wGetWorkspaceOwnerByName(Wasdi, sWorkspaceId)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sWorkspaceId: the Id of the workspace
%
% OUTPUT
%   sWorkspaceOwner: the owner of the workspace

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sWorkspaceOwner = Wasdi.getWorkspaceOwnerByWSId(sWorkspaceId);
   
end

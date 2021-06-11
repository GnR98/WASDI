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
function asReturns = wWaitProcesses(Wasdi, asIds)% Waits for a list of processes% Syntax% asReturns = wWaitProcesses(Wasdi, asIds)% % INPUT%    Wasdi: Wasdi object created after the wasdilib call%    asIds: a collection of WASDI process IDs%% OUTPUT%   asReturns: a collection with the end status of each input ID, in the same order  if exist("Wasdi") < 1     disp('Wasdi variable does not existst')    return   end         Wasdi.waitProcesses(asIds)end
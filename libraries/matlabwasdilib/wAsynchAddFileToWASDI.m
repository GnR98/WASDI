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
function sStatus =wAsynchAddFileToWASDI(Wasdi, sFileName)
%Ingest a new file in the Active WASDI Workspace WITHOUT waiting for the result
%The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
%If the file is not present in the WASDI cloud workpsace, it will be automatically uploaded if the config AUTOUPLOAD flag is true (default)
%Syntax
%sStatus =wAsynchAddFileToWASDI(Wasdi, sFileName);
%
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sFileName: Name of the file to add
%
%:Returns:
%  :sProcessId: Process Id of the WASDI Ingest operation on the server. Can be used as input to the wWaitProcess method or wGetProcessStatus methods to check the execution.

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = char(Wasdi.asynchAddFileToWASDI(sFileName));
   
   disp(['Ingest Process Status ' sStatus]);

end
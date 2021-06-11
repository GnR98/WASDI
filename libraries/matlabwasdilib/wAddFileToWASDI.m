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
function sStatus =wAddFileToWASDI(Wasdi, sFileName)
%Ingest a new file in the Active WASDI Workspace waiting for the result
%The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
%o work be sure that the file is on the server
%
%Syntax
%
%sStatus =wAddFileToWASDI(Wasdi, sFileName);
%
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sFileName: Name of the file to add
%
%
%:Returns:
%  :sStatus: Status of the Ingest Process as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sStatus = char(Wasdi.addFileToWASDI(sFileName));
   
   disp(['Ingest Process Status ' sStatus]);

end
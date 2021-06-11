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
function sStatus = wExecuteWorkflow(Wasdi, sWorkflow, asInputFiles, asOutputFiles)
%Executes a SNAP workflow. The workflow has to be uploaded in WASDI: it can be public or private of a user.
%If it is private, it must be triggered from the owner.
%Syntax
%sStatus = wExecuteWorkflow(Wasdi, sWorkflow, asInputFiles, asOutputFiles);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sWorkflow: Name of the workflow
%:param asInputFiles: array of strings with the name of the input files. Must be one file for each Read Node of the workflow, in the exact order
%:param asOutputFiles: array of strings with the name of the output files. Must be one file for each Write Node of the workflow, in the exact order
%
%:Returns:
%  :sStatus: Exit Workflow Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
  
   sStatus = char(Wasdi.executeWorkflow(asInputFiles, asOutputFiles, sWorkflow));
   
   disp(['Process Status ' sStatus]);

end
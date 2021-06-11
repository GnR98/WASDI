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
function [asWorkflowNames, asWorkflowIds]=wGetWorkflows(Wasdi)
%Get the List of Workflows of the actual User
%Syntax
%[asWorkflowNames, asWorkflowIds]=wGetWorkflows(Wasdi);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%
%:Returns:
%  :asWorkflowNames: array of strings that are the names of the workflows
%  :asWorkflowIds: array of strings that are the id of the workflows
  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   aoWorkflows = Wasdi.getWorkflows();
   
   iTot = aoWorkflows.size();
   
   disp(['Number of Workflows: ' sprintf("%d",iTot)]);

  %For each Workflow
  for iWf = 0:iTot-1
    oWorkflow = aoWorkflows.get(iWf);
    asWorkflowNames{iWf+1} = oWorkflow.get("name");
    asWorkflowIds{iWf+1} = oWorkflow.get("workflowId");
  end

   
end

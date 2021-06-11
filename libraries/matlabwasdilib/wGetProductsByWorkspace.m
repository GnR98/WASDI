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
function asProducts=wGetProductsByWorkspace(Wasdi, sWorkspaceName)
%Get the List of Products in a Workspace
%Syntax
%asProducts=wGetProductsByWorkspace(Wasdi, sWorkspaceName);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sWorkspaceName: name of the workspace
%
%
%:Returns:
%  :asProducts: array of strings that are the names of the products


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   aoProducts= Wasdi.getProductsByWorkspace(sWorkspaceName);
   disp('got product list');
   
   iTot = aoProducts.size();
   
   disp(['products count ' sprintf("%d",iTot )]);

  %Per tutte le centraline trovate
  for iWs = 0:iTot-1
    asProducts{iWs+1} = aoProducts.get(iWs);
  end

   
end

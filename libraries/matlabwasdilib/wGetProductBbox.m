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
function sBbox = wGetProductBbox(Wasdi, sProductName)%Gets own processor ID%Syntax%sProcId = wGetProductBbox(Wasdi, sProductName)%%:param Wasdi: Wasdi object created after the wasdilib call%:param sProductName: the product name for which the bounding box must be retrieved%%:Returns:%  :sBbox: the requested bounding box  if exist("Wasdi") < 1     disp('Wasdi variable does not exist')    return   end      sProcId = char(Wasdi.getProductBbox(sProductName));   end
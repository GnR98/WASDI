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
function asStatuses=wImportProductList(Wasdi, asProductLinks)
%Import an EO Image in WASDI. This is the asynchronous version
%Syntax
%sProcessObjId=wAsynchImportProductList(Wasdi, asProductLinks)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param asProductLinks: collection of Product Direct Link as returned by wSearchEOImage
%
%:Returns:
%  :asStatuses: list of statuses of the import processes
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   iTotFound = length(asProductLinks);
   asUrls = javaObject('java.util.ArrayList',iTotFound);
   
   for iProduct = 1:iTotFound
    asUrls.add(asProductLinks{iProduct})
   end
   
   asProcessObjId = Wasdi.asynchImportProductList(asUrls);
   
end

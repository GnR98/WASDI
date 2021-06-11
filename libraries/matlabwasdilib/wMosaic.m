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
function sStatus=wMosaic(Wasdi, asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue)
%Mosaic input images in the output file
%Syntax
%sStatus=wMosaic(Wasdi, asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%	 asInputFileNames: Array of input file names
%	 sOutputFile: Name of the output file
%	 sNoDataValue: value to use as no data in the output file
%	 sInputIgnoreValue: value used as no data in the input file
%
%:Returns:
%  :sStatus: end status of the mosaic operation
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = Wasdi.mosaic(asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue);
   
   disp(['Mosaic Status: ' sStatus]);
   
end

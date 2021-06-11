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
function sParameter = wSetParameter(Wasdi, sKey, sValue)
%Set the value of a parameter
%Syntax
%sParameter = wSetParameter(Wasdi, sKey, sValue)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sKey: The KEY of the parameter to add or update
%:param sValue: The the value of the parameter
%
%:Returns:
%  :sParameter: The value (same as sValue in input)

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.addParam(sKey, sValue);
   sParameter = sValue;
   
   disp(['Parameter ' sParameter]);

end
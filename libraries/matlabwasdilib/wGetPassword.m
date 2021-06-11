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
function sPassword = wGetPassword(Wasdi)
%Gets the password
%Syntax
%sPassword = wGetPassword(Wasdi)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%  :sPassword: WASDI user's password

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sPassword = char(Wasdi.getPassword());
   
end
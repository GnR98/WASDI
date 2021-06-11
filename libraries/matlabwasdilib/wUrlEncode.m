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
%courtesy of https://titanwolf.org/Network/Articles/Article?AID=c16b0563-60f6-46e2-9c50-092039fa86bc#gsc.tab=0
function u = wUrlEncode(s)
	u = '';
	for k = 1:length(s),
		if isalnum(s(k))
			u(end+1) = s(k);
		else
			u=[u,'%',dec2hex(s(k)+0)];
		end; 	
	end
end
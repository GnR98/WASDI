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
function wImportAndPreprocess(Wasdi, asProductLinks, asProductNames, sWorkflowName, sSuffix, sProvider=[])
%Import and preprocess a collection of EO products
%Syntax
%wImportAndPreprocess(Wasdi, asProductLinks, asProductNames, sWorkflowName, sSuffix)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param asProductLinks: collection of Product Direct Link as returned by wSearchEOImages
%:param asProductNames: collection of Product names, as returned by wSearchEOImages
%:param sWorkflowName: the name of the SNAP workflow to be applied to downloaded imagesc
%:param sSuffix: the suffix to append to the preprocessed files
%:param sProvider: optional, the provider from where data must be collected
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   asLinks = javaObject('java.util.ArrayList',length(asProductLinks))
   asNames = javaObject('java.util.ArrayList',length(asProductNames))
   for i = 1:length(asProductLinks)
     asLinks.add(asProductLinks{i})
     asNames.add(asProductNames{i})
   end
   
   Wasdi.importAndPreprocessWithLinks(asLinks, asNames, sWorkflowName, sSuffix, sProvider);
   
end

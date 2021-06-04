function wSetBasePath(Wasdi, sNewBasePath)
% Set the base path
% Syntax
% wSetBasePath(Wasdi, sNewBasePath)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sNewBasePath: the new base path
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
    Wasdi.setBasePath(sNewBasePath)
end
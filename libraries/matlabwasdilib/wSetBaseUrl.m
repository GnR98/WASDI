function wSetBaseUrl(Wasdi, sBaseUrl)%Set the base URL%Syntax%wSetBaseUrl(Wasdi, sBaseUrl)%%:param Wasdi: Wasdi object created after the wasdilib call%:param sBaseUrl: the new base URL (must be valid)  if exist("Wasdi") < 1     disp('Wasdi variable does not existst')    return   end      Wasdi.setBaseUrl(sBaseUrl)end
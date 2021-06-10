function sWorkspaceBaseUrl = wGetWorkspaceBaseUrl(Wasdi)%%Gets the workspace base URL%Syntax%sWorkspaceBaseUrl = wGetWorkspaceBaseUrl(Wasdi)%%:param Wasdi: Wasdi object created after the wasdilib call%%:Returns:%  sWorkspaceBaseUrl: the base URL for active workspace  if exist("Wasdi") < 1     disp('Wasdi variable does not exist')    return   end      sWorkspaceBaseUrl = Wasdi.getWorkspaceBaseUrl();   end
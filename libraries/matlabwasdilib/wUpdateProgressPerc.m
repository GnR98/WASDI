+wasdi
function sStatus = wUpdateProgressPerc(Wasdi, iPerc)%Updates the status of a process%Syntax%sStatus = wUpdateProgressPerc(Wasdi, iPerc)%%:param Wasdi: Wasdi object created after the wasdilib call%:param iPerc: the %of completion%:Returns:%  :sStatus: updated status as a String or '' if there was any problem  if exist("Wasdi") < 1     disp('Wasdi variable does not existst')    return   end      Wasdi.updateProgressPerc(iPerc)   end
+wasdi
function wSetMyProcId(Wasdi, sMyNewProcId)
%Set the processor ID
%Syntax
%wSetMyProcId(Wasdi, sMyNewProcId)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sMyNewProcId: the new processor ID

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.setMyProcId(sMyNewProcId);
  

end
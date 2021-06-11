+wasdi
function oProcessPayload = wGetProcessorPayload(Wasdi, sProcessObjId)
%Gets the payload of given processor
%Syntax
%oProcessPayload = wGetProcessorPayload(Wasdi, sProcessObjId)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sProcessObjId: process ID for which the payload must be retrieve
%
%:Returns:
%  :oProcessPayload: an object containing the payload

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   oProcessPayload = loadjson(Wasdi.getProcessorPayloadAsJSON(sProcessObjId));
   
end
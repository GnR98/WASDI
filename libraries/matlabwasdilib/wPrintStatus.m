+wasdi
function wPrintStatus(Wasdi)%Prints the status%Syntax:%printStatus(Wasdi)%%:param Wasdi: Wasdi object created after the wasdilib call%  if exist("Wasdi") < 1     disp('Wasdi variable does not exist')    return   end     Wasdi.printStatus();   end
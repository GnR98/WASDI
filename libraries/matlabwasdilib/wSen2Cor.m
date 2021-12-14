function sStatus=wSen2Cor(Wasdi, sProductName)
%Sen2Cor Converts a Level-1 sentinel 2 file to Level-2, improving image quality
%Syntax
%sStatus=wSen2Cor(Wasdi, sProductName)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%	 sProductName: The product, from the current active workspace, that should be converted
%:Returns:
%  :sStatus: end status of the Sen2Cor operation
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = Wasdi.sen2cor(sProductName);
   
   disp(['Sen2Cor Status: ' sStatus]);
   
end

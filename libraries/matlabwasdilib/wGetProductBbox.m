function sBbox = wGetProductBbox(Wasdi, sProductName)% Gets own processor ID% Syntax% sProcId = wGetProductBbox(Wasdi, sProductName)% % INPUT%    Wasdi: Wasdi object created after the wasdilib call%    sProductName: the product name for which the bounding box must be retrieved%% :Returns:%   sBbox: the requested bounding box   if exist("Wasdi") < 1     disp('Wasdi variable does not exist')    return   end      sProcId = char(Wasdi.getProductBbox(sProductName));   end
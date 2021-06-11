function sReturn=wAsynchMultiSubset(Wasdi, sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE)
% Extracts subsets of an image given its name and the desired bounding boxes.Asynchronous version
% Syntax
% sReturn=wAsynchMultiSubset(Wasdi, sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE)
% 
% INPUT
%   Wasdi: Wasdi object created after the wasdilib call
%   sInputFile: the input file from where subsets must be extracted
%   asOutputFiles: names to be given to output files
%   adLatN: a collection of Northernmost latitudes
%   adLonW: a collection of Westernmost longitudes
%   adLatS: a collection of Southernmost latitudes
%   adLonE: a collection of Easternnmost longitudes
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   asOut = javaObject('java.util.ArrayList',length(asOutputFiles));
   adN = javaObject('java.util.ArrayList',length(adLatN));
   adW = javaObject('java.util.ArrayList',length(adLonW));
   adS = javaObject('java.util.ArrayList',length(adLatS));
   adE = javaObject('java.util.ArrayList',length(adLonE));
   
   for i=1:length(asOutputFiles)
     asOut.add(asOutputFiles(i));
     adN.add(double(adLatN(i)));
     adW.add(double(adLonW(i)));
     adS.add(double(adLatS(i)));
     adE.add(double(adLonE(i)));
   end
   
   
   sReturn = Wasdi.multiSubset(sInputFile, asOut, adN, adW, adS, adE);
   
   
end

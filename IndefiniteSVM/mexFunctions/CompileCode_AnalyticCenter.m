% Script to compile mex file using MATLAB interface

% Run this first if you haven't done so already, choosing gcc if available.
% mex -setup

% On windows and linux, you should adjust the paths below to reflect 
mexext
switch mexext
    case {'mexmaci'} % Macintosh paths the VecLib framework
        blaspath='/System/Library/Frameworks/Accelerate.framework/Versions/A/Frameworks/vecLib.framework/Versions/A/libBLAS.dylib';
        gfortranpath='/opt/local/lib/gcc42/libgfortran.a';
    case {'mexw32'} % Win32 MATLAB after 7.
        lapackpath=[matlabroot,'\extern\lib\win32\microsoft\libmwlapack.lib'];
        blaspath=[matlabroot,'\extern\lib\win32\microsoft\libmwblas.lib'];
    case{'mexa64'}
        gfortranpath='/usr/lib/gcc/x86_64-linux-gnu/4.4/libgfortran.a';
        blaspath='/home/wcg/build/lib/libblas.so';
        lapackpath='/home/wcg/build/lib/liblapack.so';
    case{'mexglx'}
        blaspath='/usr/lib/libblas.so';
        lapackpath='/usr/lib/liblapack.so';
end

% Test for mex extension to determine matlab version and platform
switch mexext
    case {'mexw32'} % Win32 MATLAB after 7.1
        files='AnalyticCenterCG_mex.c conjugate_gradient.c AnalyticCenterCG.c utils.c';
        libs=strcat(['''',blaspath,'''',' ','''',lapackpath,'''']);
        switches='-v -O -DWIN32';
        eval(['mex ',switches,' ',files,' ',libs]);
    case {'mexmaci'}% Macintosh using the Accelerate framework
        files='AnalyticCenterCG_mex.c conjugate_gradient.c AnalyticCenterCG.c utils.c';
        switches='-v -O -Dmac';
        libs=strcat([blaspath,'  ',gfortranpath]);
        eval(['mex ',switches,' ',files,' ',libs]);
    case {'mexa64'}% a64 Linux
        files='AnalyticCenterCG_mex.c conjugate_gradient.c AnalyticCenterCG.c utils.c';
        switches='-v -O -Dlinuxp -f ./mexoptsLINUX.sh';
%         switches = '-v -O -Dlinuxp';
        libs=strcat([blaspath,' ',lapackpath]);
        disp(['mex ',switches,' ',files,' ',libs]);
        eval(['mex ',switches,' ',files,' ',libs]);
    otherwise % linux as default
        files='AnalyticCenterCG_mex.c conjugate_gradient.c AnalyticCenterCG.c utils.c';
        switches='-v -O -Dlinuxp -f ./mexoptsLINUX.sh';
        libs=strcat([blaspath,' ',lapackpath]);
        eval(['mex ',switches,' ',files]);
end
disp(' ......................... Done Compiling Source .........................')
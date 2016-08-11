% Script to compile mex file using MATLAB interface

% Run this first if you haven't done so already, choosing gcc if available.
% mex -setup

% The code also needs a precompiled version of arpack
% Warning: because of a duplicate definition of second.f in ARPACK and LAPACK, 
% you should link with ARPACK's version of LAPACK. Using vendor versions of BLAS 
% is OK and significantly improves performance.

% On windows and linux, you should adjust the paths below to reflect 
% On the mac: install ARPACK through macports and the settings below should be fine.

switch mexext
    case {'mexmaci'} % Macintosh paths the VecLib framework
        % Arpack is available through MACPORTS.
%         arpackpath='/opt/local/lib/libarpack.a';
        blaspath='/System/Library/Frameworks/Accelerate.framework/Versions/A/Frameworks/vecLib.framework/Versions/A/libBLAS.dylib';
        gfortranpath='/opt/local/lib/gcc42/libgfortran.a';
    case {'mexw32'} % Win32 MATLAB after 7.
%        arpackpath='C:\Documents and Settings\rluss\Desktop\MatlabArpack\libmwarpack.lib'; 
        lapackpath=[matlabroot,'\extern\lib\win32\microsoft\libmwlapack.lib'];
        blaspath=[matlabroot,'\extern\lib\win32\microsoft\libmwblas.lib'];
    case{'mexa64'}
        gfortranpath='/usr/lib/gcc/x86_64-linux-gnu/4.4/libgfortran.a';
        blaspath='/home/wcg/build/lib/libblas.so';
%         blaspath = '/var/lib/dpkg/alternatives/libblas.so';
 
        lapackpath='/home/wcg/build/lib/liblapack.so';
%         lapackpath = '/var/lib/dpkg/alternatives/liblapack.so';
    case{'mexglx'}
        blaspath='/usr/lib/libblas.so';
        lapackpath='/usr/lib/liblapack.so';
end

% Test for mex extension to determine matlab version and platform
switch mexext
    case {'mexw32'} % Win32 MATLAB after 7.1
        files='eigUpdateMult_mex.cpp';
        libs=strcat(['''',blaspath,'''',' ','''',lapackpath,'''']);
        switches='-v -O -DWIN32';
        eval(['mex ',switches,' ',files,' ',libs]);
    case {'mexmaci'}% Macintosh using the Accelerate framework
        files='eigUpdateMult_mex.cpp';
        switches='-v -O -Dmac';
        libs=strcat([blaspath,'  ',gfortranpath]);
        eval(['mex ',switches,' ',files,' ',libs]);
    case {'mexa64'} % Linux defaults
        files='mexFunctions/eigUpdateMult_mex.cpp';
        switches='-v -O -Dlinuxp';
        libs=strcat([gfortranpath,' ',blaspath,'  ',lapackpath]);
        disp(['mex ',switches,' ',files,' ',libs]);
        eval(['mex ',switches,' ',files,' ',libs]);
    otherwise % Linux defaults
        files='eigUpdateMult_mex.cpp';
        switches='-v -O -Dlinuxp';
        libs=strcat([blaspath,' ',lapackpath]);
        eval(['mex ',switches,' ',files,' ',libs]);
end
disp(' ......................... Done Compiling Source .........................')
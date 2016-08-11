This directory contains source code for the two required Mex functions:
1. Analytic Center Conjugate Gradient code written by Ronny Luss
2. Rank-one eigenvalue decomposition code written by Matyas Sustik.

Notes: 
1. Binaries are included here for Max, Win32, and 32-bit linux.
2. There are two Matlab codes for building binaries called CompileCode_AnalyticCenter.m and CompileCode_EigUpdate.m.  Within the compile codes, binaries can be compiled for Mac (mexext is mexmaci), Win32 (mexext is mexw32), 64-bit linux (mexext is mexa64), and 32-bit linux (mexext is mexglx).  To compile on your machine, you must set the proper paths within CompileCode for blas, lapack, and gfortran on the framework you are compiling on.  The paths are set after the first command "switch mexext" in the corresponding CompileCode files.
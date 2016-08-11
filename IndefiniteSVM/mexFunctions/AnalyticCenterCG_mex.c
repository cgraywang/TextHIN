#include "AnalyticCenterCG_mex.h" 

void mexFunction(int nlhs, mxArray *plhs[], int nrhs, const mxArray *prhs[])
{
	double *Amat,*Ahatmat,*bvec,*lvec,*uvec,*svec,*xout,*CGitersout,*Newtonitersout,*flagout;
	ptrdiff_t m,n;
	double epsilon,maxiter;

	Amat=mxGetPr(prhs[0]);
	m=mxGetM(prhs[0]);
	n=mxGetN(prhs[0]);
	bvec=mxGetPr(prhs[1]);
	lvec=mxGetPr(prhs[2]);
	uvec=mxGetPr(prhs[3]);
	maxiter=mxGetScalar(prhs[4]);
	epsilon=mxGetScalar(prhs[5]);
	svec=mxGetPr(prhs[6]);
	Ahatmat=mxGetPr(prhs[7]);

	plhs[0] = mxCreateDoubleMatrix(n, 1, mxREAL);
	xout = mxGetPr(plhs[0]);
	plhs[1] = mxCreateDoubleMatrix(1, 1, mxREAL);
	Newtonitersout = mxGetPr(plhs[1]);	
	plhs[2] = mxCreateDoubleMatrix(1, 1, mxREAL);
	CGitersout = mxGetPr(plhs[2]);	
	plhs[3] = mxCreateDoubleMatrix(1, 1, mxREAL);
	flagout = mxGetPr(plhs[3]);	
	
	AnalyticCenterCG(Amat,m,n,bvec,lvec,uvec,(int)maxiter,epsilon,svec,Ahatmat,xout,CGitersout,Newtonitersout,flagout);
  }

/* Main function
Performs Preconditioned Conjugate Gradient Method to solve Ax=b

Last Modified: Ronny Luss Nov 2007.
*/

#include "AnalyticCenterCG_mex.h"
/*
    Input parameters: 
           A : mxn matrix
		   m : number of rows of A
		   n : number of columns of A
           f : Right-hand side nx1 column vector 
           s : nx1 start vector (the initial guess)
         tol : relative residual error tolerance for break
               condition 
     maxiter : Maximum number of iterations to perform
           M : diag(M) is the inverse of the preconditioner where M is a vector

*/
void conjugate_gradient(double *A,ptrdiff_t m,ptrdiff_t n,double *D1,double *D2,double *f,double *s,double tol,int maxiter,double *M,double *xout,double *CGiters,double *flag)
{
	double *u=(double *) calloc(n,sizeof(double)); // n x 1 solution vector
	double *r=(double *) calloc(n,sizeof(double)); // n x 1 residual vector
	double *p=(double *) calloc(n,sizeof(double)); // n x 1 direction vector
	double *veca=(double *) calloc(m,sizeof(double)); // mx1 temp vector
	double *vecb=(double *) calloc(n,sizeof(double)); // nx1 temp vector
	double *vecc=(double *) calloc(n,sizeof(double)); // nx1 temp vector
	double *vecd=(double *) calloc(m*n,sizeof(double)); // mxn temp matrix
	double alpha,beta,rho,rho_new,normr,normf;
	int niter=0,i,j;
	ptrdiff_t incx = 1;
	// initializations
	for (i=0;i<n;i++)
		for (j=0;j<m;j++)
			vecd[j+i*m]=A[j+i*m]*D1[j]; // vecd stores  the matrix D1*A
	*flag=0;
	cblas_dcopy(n,s,incx,u,incx);
	alpha=1.0;beta=0.0;cblas_dgemv(CblasColMajor,CblasNoTrans,m,n,alpha,A,m,u,incx,beta,veca,incx); // veca contains A*s
	alpha=1.0;beta=0.0;cblas_dgemv(CblasColMajor,CblasTrans,m,n,alpha,vecd,m,veca,incx,beta,vecb,incx); // vecb contains A'*(D1*(A*s))
	cblas_dcopy(n,f,incx,r,incx);
	alpha=-1.0;cblas_daxpy(n,alpha,vecb,incx,r,incx); // r contains f-A'*(D1*(A*s))
	cblas_dcopy(n,s,incx,vecb,incx);
	component_mult(vecb,D2,n); // vecb contains D2*s
	alpha=-1.0;cblas_daxpy(n,alpha,vecb,incx,r,incx); // r contains f-A'*(D1*(A*s))-D2*s
	cblas_dcopy(n,r,incx,p,incx);
	component_mult(p,M,n); // p contains M*r
	rho=doubdot(p,r,n); // rho is r'*M*r

	normf=doubnorm2(f,n);  
	if (normf<.0001) 
		normf=1.0;
	normr=doubnorm2(r,n);
	while (normr/normf > tol) {    // Test break condition
		alpha=1.0;beta=0.0;cblas_dgemv(CblasColMajor,CblasNoTrans,m,n,alpha,A,m,p,incx,beta,veca,incx); // veca contains A*p
		alpha=1.0;beta=0.0;cblas_dgemv(CblasColMajor,CblasTrans,m,n,alpha,vecd,m,veca,incx,beta,vecb,incx); // vecb contains A'*(D1*(A*p))		
		cblas_dcopy(n,p,incx,vecc,incx);
		component_mult(vecc,D2,n); // vecc contains D2*p
		alpha=1.0;cblas_daxpy(n,alpha,vecc,incx,vecb,incx); // vecb contains A'*(D1*(A*p))+D2*p
		alpha=rho/doubdot(vecb,p,n);	
		cblas_daxpy(n,alpha,p,incx,u,incx); // u=u+alpha*p
		alpha=-1.0*alpha;cblas_daxpy(n,alpha,vecb,incx,r,incx); // r=r-alpha*vecb
		cblas_dcopy(n,r,incx,vecc,incx);
		component_mult(vecc,M,n); // vecc contains M*r
		rho_new=doubdot(vecc,r,n); // rho is r'*M*r
		alpha=rho_new/rho;cblas_dscal(n,alpha,p,incx); //p=rho_new/rho * p;
		alpha=1.0;cblas_daxpy(n,alpha,vecc,incx,p,incx); // p=(M*r)+p
		rho=rho_new;
		niter++;
		if (niter == maxiter) {   //if max. number of iterations is reached, break.
			normr=doubnorm2(r,n);
			*flag = 1;                  
			break;
		}
		normr=doubnorm2(r,n);
	}
	
	cblas_dcopy(n,u,incx,xout,incx);
	*CGiters=niter;
	free(u);
	free(r);
	free(p);
	free(veca);
	free(vecb);
	free(vecc);
	free(vecd);
}
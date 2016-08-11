/* Main function
Solves Analytic Center Problem using Preconditioned Conjugate Gradient Method for solving Newton System

Last Modified: Ronny Luss Nov 2007.
*/

#include "AnalyticCenterCG_mex.h" 
/*
    Input parameters: 
           A : mxn matrix
		   m : number of rows of A
		   n : number of columns of A
           b : Right-hand side nx1 column vector 
		   l : lower bounds on analytic center
		   u : upper bounds on analytic center
          x0 : nx1 start vector (the initial guess)
         tol : relative residual error tolerance for break
               condition for conjugate gradient method
     maxiter : Maximum number of iterations to perform
	 epsilon : error for analytic center problem 
        Ahat : Matrix used to create preconditioner for conjugate gradient method

*/

void AnalyticCenterCG(double *A,ptrdiff_t m,ptrdiff_t n,double *b,double *l,double *u,int maxiter,double epsilon,double *x0,double *Ahat, double *xout,double *CGitersout,double *Newtonitersout,double *flagout)
{

	double *x=(double *) calloc(n,sizeof(double)); // n x 1 solution vector
	double *D1=(double *) calloc(m,sizeof(double)); // mx1 vector for (b-A*x).^(-2)
	double *D2=(double *) calloc(n,sizeof(double)); // nx1 vector for (u-x).^(-2)+(x-l).^(-2)

	double *veca=(double *) calloc(m,sizeof(double)); // mx1 temp vector 
	double *vecb=(double *) calloc(n,sizeof(double)); // nx1 temp vector 
	double *vecc=(double *) calloc(m,sizeof(double)); // mx1 temp vector 
	double *vecd=(double *) calloc(m,sizeof(double)); // mx1 temp vector 	
	
	double *delta_x=(double *) calloc(n,sizeof(double)); // nx1 vector for newton directions
	double *gradient=(double *) calloc(n,sizeof(double)); // nx1 vector for gradient
	double *CGiters_used=(double *) calloc(1,sizeof(double));
	double alpha,beta; // parameters used for backtracking
	double mu,nu; // temp variables for LAPACK/BLAS functions
	int niter=0,maxCGiters=1,i;
	ptrdiff_t incx = 1;
	double lambda_sq; // Newton decrement
	double t,tol=.1,maxM,minM;
	double f1,f2; // temp variables for backtracking
	int sum;

	cblas_dcopy(n,x0,incx,x,incx);
	alpha=.01;
	beta=.5;
	CGitersout[0]=1;

	while (niter <= maxiter) {
		niter++;
		cblas_dcopy(m,b,incx,D1,incx);
		mu=-1.0;nu=1.0;cblas_dgemv(CblasColMajor,CblasNoTrans,m,n,mu,A,m,x,incx,nu,D1,incx); // D1 contains b-A*x
		cblas_dcopy(m,D1,incx,vecc,incx); // vecc will hold b-A*x
		for (i=0;i<m;i++){
			veca[i]=1.0/D1[i]; // veca is now (b-A*x).^(-1)
			D1[i]=veca[i]/D1[i]; // D1 is now (b-A*x).^(-2)
		}		
		for (i=0;i<n;i++) {
			gradient[i]=1.0/(u[i]-x[i])-1.0/(x[i]-l[i]);	
			D2[i]=1.0/(u[i]-x[i])/(u[i]-x[i])+1.0/(x[i]-l[i])/(x[i]-l[i]); // D2 is now (u-x).^(-2)+(x-l).^(-2)
		}
		mu=1.0;nu=1.0;cblas_dgemv(CblasColMajor,CblasTrans,m,n,mu,A,m,veca,incx,nu,gradient,incx); // gradient contains A'*(b-A*x).^(-1)+(u-x).^(-1)-(x-l).^(-1)
		cblas_dcopy(n,D2,incx,vecb,incx);
		mu=1.0;nu=1.0;cblas_dgemv(CblasColMajor,CblasTrans,m,n,mu,Ahat,m,D1,incx,nu,vecb,incx); // vecb contains preconditioner Ahat'*(b-A*x).^(-2)+(u-x).^(-2)+(x-l).^(-2)
		maxM=1.0/vecb[0];minM=maxM;
		for (i=0;i<n;i++) {
			vecb[i]=1.0/vecb[i]; // take the inverse of the hessian's diagonal for a preconditioner
			maxM=dmaxif(maxM,vecb[i]);minM=dminif(minM,vecb[i]);
		}
		if (maxM/minM>10000)
			for (i=0;i<n;i++) vecb[i]=1.0; // set the preconditioner to the identity matrix
		mu=-1.0;cblas_dscal(n,mu,gradient,incx); // gradient now holds -1*gradient
				
		// Solve the Newton System
		conjugate_gradient(A,m,n,D1,D2,gradient,delta_x,tol,ceil(.1*n),vecb,delta_x,CGiters_used,flagout);

		if (niter>1)		
			CGitersout[0]=dmaxif(CGitersout[0],CGiters_used[0]);
		lambda_sq=doubdot(gradient,delta_x,n);
		if (lambda_sq/2<=epsilon) break; // stopping condition for newton's method
				
		// Now conduct line search
		// First find how far can move while staying in feasible set using direct projection
		mu=1.0;nu=0.0;cblas_dgemv(CblasColMajor,CblasNoTrans,m,n,mu,A,m,delta_x,incx,nu,vecd,incx); // vecd contains A*delta_x
		t=1;
		for (i=0;i<m;i++) 
			if (vecd[i]>0)
				t=dminif(t,vecc[i]/vecd[i]);
		for (i=0;i<n;i++)
			if (delta_x[i]>0) 
				t=dminif(t,(u[i]-x[i])/delta_x[i]);
			else if (delta_x[i]<0)
				t=dminif(t,(l[i]-x[i])/delta_x[i]);
		t=.99*t;
		
		// backtracking to get feasibility
/*		sum=1;
		while(sum>0){
			sum=0;
			for (i=0;i<m;i++) 
				if (vecc[i]-t*vecd[i] <= -.00001)
					sum++;
			for (i=0;i<n;i++) {
				if (x[i]+t*delta_x[i]-l[i] <= -.00001) 
					sum++;
				if (u[i]-x[i]-t*delta_x[i] <= -.00001) 
					sum++;
			}
			t=t*beta;
		}
*/
		// Now do backtracking
		f1=0;f2=0;
		for (i=0;i<m;i++) {
			f1+=log(vecc[i]-t*vecd[i]);
			f2+=log(vecc[i]);
		}
		for (i=0;i<n;i++) {
			f1+=(log(x[i]+t*delta_x[i]-l[i])+log(u[i]-x[i]-t*delta_x[i]));
			f2+=(log(x[i]-l[i])+log(u[i]-x[i]));
		}
		while (-1*f1 > -1*f2-alpha*t*doubdot(gradient,delta_x,n)) {
			t=beta*t;
			f1=0;
			for (i=0;i<m;i++)
				f1+=log(vecc[i]-t*vecd[i]);
			for (i=0;i<n;i++)
				f1+=(log(x[i]+t*delta_x[i]-l[i])+log(u[i]-x[i]-t*delta_x[i]));
		}
		cblas_daxpy(n,t,delta_x,incx,x,incx); // x=x+t*delta_x
	}
	Newtonitersout[0]=niter;
	cblas_dcopy(n,x,incx,xout,incx);

	free(veca);
	free(vecb);
	free(vecc);
	free(vecd);
	free(delta_x);
	free(gradient);
	free(x);
	free(D1);
	free(D2);
	free(CGiters_used);

}
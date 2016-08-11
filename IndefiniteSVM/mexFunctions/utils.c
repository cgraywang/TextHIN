#include "AnalyticCenterCG_mex.h" 

// BLAS wrapper for win32 version
#ifdef WIN32
void cblas_dscal( int N, double alpha, double *X, int incX)
{
	dscal(&N,&alpha,X,&incX);
}

void cblas_dcopy(int N,double *X,int incX,double *Y,int incY)
{
	dcopy(&N,X,&incX,Y,&incY);
}

void cblas_dgemm(enum CBLAS_ORDER Order,enum CBLAS_TRANSPOSE transA, enum CBLAS_TRANSPOSE transB,
                 int M, int N, int K, double alpha, double *A, int lda,
                 double *B, int ldb, double beta, double *C, int ldc)
{
	char ta[1],tb[1];
	clock_t c0, c1;
	FILE *fp;
	if (transA==111)
	{
		*ta='N';
	}
	else
	{
		*ta='T';
	};
	if (transB==111)
	{
		*tb='N';
	}
	else
	{
		*tb='T';
	};
	c0 = clock();
	dgemm(ta,tb,&M,&N,&K,&alpha,A,&lda,B,&ldb,&beta,C,&ldc);
	c1 = clock();
	fp=fopen("dgemm_times.txt", "a");
	fprintf (fp,"%lf\n", (float) (c1 - c0)/CLOCKS_PER_SEC);		
	fclose(fp);
	
}

void cblas_dgemv(enum CBLAS_ORDER Order,enum CBLAS_TRANSPOSE transA,
                 int M, int N, double alpha, double *A, int lda,
                 double *B, int incB, double beta, double *C, int incC)
{
	char ta[1];
	if (transA==111)
	{
		*ta='N';
	}
	else
	{
		*ta='T';
	};
	dgemv(ta,&M,&N,&alpha,A,&lda,B,&incB,&beta,C,&(ptrdiff_t)incC);
}

void cblas_daxpy(int N,double alpha,double *X,int incX,double *Y,int incY)
{
	daxpy(&N,&alpha,X,&incX,Y,&incY);
}


#endif WIN32

#ifdef linuxp
#include "blas.h"
#include "lapack.h"
void cblas_dscal( ptrdiff_t N, double alpha, double *X, ptrdiff_t incX)
{
	dscal(&N,&alpha,X,&incX);
}

void cblas_dcopy(ptrdiff_t N,double *X,ptrdiff_t incX,double *Y,ptrdiff_t incY)
{
	dcopy(&N,X,&incX,Y,&incY);
}

void cblas_dgemm(enum CBLAS_ORDER Order,enum CBLAS_TRANSPOSE transA, enum CBLAS_TRANSPOSE transB,
                 ptrdiff_t M, ptrdiff_t N, ptrdiff_t K, double alpha, double *A, ptrdiff_t lda,
                 double *B, ptrdiff_t ldb, double beta, double *C, ptrdiff_t ldc)
{
	char ta[1],tb[1];
	clock_t c0, c1;
	FILE *fp;
	if (transA==111)
	{
		*ta='N';
	}
	else
	{
		*ta='T';
	};
	if (transB==111)
	{
		*tb='N';
	}
	else
	{
		*tb='T';
	};
	c0 = clock();
	dgemm(ta,tb,&M,&N,&K,&alpha,A,&lda,B,&ldb,&beta,C,&ldc);
	c1 = clock();
	fp=fopen("dgemm_times.txt", "a");
	fprintf (fp,"%lf\n", (float) (c1 - c0)/CLOCKS_PER_SEC);		
	fclose(fp);
	
}

void cblas_dgemv(enum CBLAS_ORDER Order,enum CBLAS_TRANSPOSE transA,
                 ptrdiff_t M, ptrdiff_t N, double alpha, double *A, ptrdiff_t lda,
                 double *B, ptrdiff_t incB, double beta, double *C, ptrdiff_t incC)
{
	char ta[1];
	if (transA==111)
	{
		*ta='N';
	}
	else
	{
		*ta='T';
	};
	dgemv(ta,&M,&N,&alpha,A,&lda,B,&incB,&beta,C,&incC);
}

void cblas_daxpy(ptrdiff_t N,double alpha,double *X,ptrdiff_t incX,double *Y,ptrdiff_t incY)
{
	daxpy(&N,&alpha,X,&incX,Y,&incY);
}


#endif linuxp


// Some useful functions ...
void component_mult(double *xmat, double *ymat, int n)
{
	int i;
	for (i=0;i<n;i++){xmat[i]=xmat[i]*ymat[i];};
}

double doubsum(double *xmat, int n)
{
	int i;
	double res=0.0;
	for (i=0;i<n;i++){res+=xmat[i];};
	return res;
}


double doubdot(double *xvec, double *yvec, int n)
{
	int i;
	double res=0.0;
	for (i=0;i<n;i++){res+=xvec[i]*yvec[i];};
	return res;
}

int idxmax(double *xmat, int n)
{
	int i;
	int res=0;
	for (i=0;i<n;i++)
	{
		if (xmat[i]>xmat[res]) {res=i;}
	}
	return res;
}


double doubasum(double *xmat, int n)
{
	int i;
	double res=0.0;
	for (i=0;i<n;i++){res+=dabsf(xmat[i]);};
	return res;
}

double doubnorm2(double *xmat, int n)
{
	int i;
	double res=0.0;
	for (i=0;i<n;i++){res+=xmat[i]*xmat[i];};
	return sqrt(res);
}

double infnorm(double *xmat, int n)
{
	int i,j;
	double res=0.0,sum;

	for (j=0;j<n;j++){
		sum=0.0;
		for(i=0;i<n;i++)
			sum+=dabsf(xmat[j+i*n]);
		if(sum>=res) res=sum;
	}	
	return res;
}

double dsignf(double x)
{
	if (x>=0)
		return 1.0;
	else
		return -1.0;
}

double dminif(double x, double y)
{
	if (x>=y)
		return y;
	else
		return x;
}

double dmaxif(double x, double y)
{
	if (x>=y)
		return x;
	else
		return y;
}

int imaxf(int x, int y)
{
	if (x>=y)
		return x;
	else
		return y;
}

double dabsf(double x)
{
	if (x>=0)
		return x;
	else
		return -x;
}

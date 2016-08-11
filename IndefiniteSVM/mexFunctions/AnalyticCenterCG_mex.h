//#define WIN32
//#define mac

#include <stdio.h>
#include <math.h>
#include <time.h>
#include <mex.h>
#include <matrix.h>

#ifdef WIN32
#include <malloc.h>
#endif

#ifdef linuxp
#include <malloc.h>
#endif

#ifdef mac
#include "/System/Library/Frameworks/vecLib.framework/Headers/cblas.h"
#include "/System/Library/Frameworks/vecLib.framework/Headers/clapack.h"
#endif

// Calling LAPACK on the mac
#ifdef mac
#define dsyev dsyev_ 
#define dgesvd dgesvd_
#endif

#ifdef linuxp
#define dsyev dsyev_ 
#define dgesvd dgesvd_
enum CBLAS_ORDER 	{CblasRowMajor=101, CblasColMajor=102};
enum CBLAS_TRANSPOSE 	{CblasNoTrans=111, CblasTrans=112, CblasConjTrans=113};
enum CBLAS_UPLO		{CblasUpper=121, CblasLower=122};
enum CBLAS_DIAG		{CblasNonUnit=131, CblasUnit=132};
enum CBLAS_SIDE		{CblasLeft=141, CblasRight=142};

void cblas_dscal(ptrdiff_t N,double alpha, double *X,ptrdiff_t incX);
void cblas_dcopy(ptrdiff_t N,double *X,ptrdiff_t incX,double *Y,ptrdiff_t incY);
void cblas_dgemm(enum CBLAS_ORDER Order,enum CBLAS_TRANSPOSE transA, enum CBLAS_TRANSPOSE transB, ptrdiff_t M, ptrdiff_t N, ptrdiff_t K, double alpha, double *A, ptrdiff_t lda, double *B, ptrdiff_t ldb, double beta, double *C, ptrdiff_t ldc);
void cblas_dgemv(enum CBLAS_ORDER Order,enum CBLAS_TRANSPOSE transA, ptrdiff_t M, ptrdiff_t N, double alpha, double *A, ptrdiff_t lda, double *B, ptrdiff_t incB, double beta, double *C, ptrdiff_t incC);
void cblas_daxpy(ptrdiff_t N,double alpha,double *X,ptrdiff_t incX,double *Y,ptrdiff_t incY);
double cblas_ddot(int n, double *x, int incx, double *y, int incy); 
#endif

#ifdef WIN32
enum CBLAS_ORDER 	{CblasRowMajor=101, CblasColMajor=102};
enum CBLAS_TRANSPOSE 	{CblasNoTrans=111, CblasTrans=112, CblasConjTrans=113};
enum CBLAS_UPLO		{CblasUpper=121, CblasLower=122};
enum CBLAS_DIAG		{CblasNonUnit=131, CblasUnit=132};
enum CBLAS_SIDE		{CblasLeft=141, CblasRight=142};

void cblas_dscal(int N,double alpha, double *X,int incX);
void cblas_dcopy(int N,double *X,int incX,double *Y,int incY);
void cblas_dgemm(enum CBLAS_ORDER Order,enum CBLAS_TRANSPOSE transA, enum CBLAS_TRANSPOSE transB, int M, int N, int K, double alpha, double *A, int lda, double *B, int ldb, double beta, double *C, int ldc);
void cblas_dgemv(enum CBLAS_ORDER Order,enum CBLAS_TRANSPOSE transA, int M, int N, double alpha, double *A, int lda, double *B, int incB, double beta, double *C, int incC);
void cblas_daxpy(int N,double alpha,double *X,int incX,double *Y,int incY);
double cblas_ddot(int n, double *x, int incx, double *y, int incy); 
#endif

// Local functions
void component_mult(double *xmat, double *ymat, int n);

double doubsum(double *xmat, int n);

double doubdot(double *xvec, double *yvec, int n);

double doubasum(double *xmat, int n);

double doubnorm2(double *xmat, int n);

double infnorm(double *xmat, int n);

int idxmax(double *xmat, int n);

void conjugate_gradient(double *A,ptrdiff_t m,ptrdiff_t n,double *D1,double *D2,double *f,double *s,double tol,int maxiter,double *M,double *x,double *CGiters,double *flag);

void AnalyticCenterCG(double *A,ptrdiff_t m,ptrdiff_t n,double *b,double *l,double *u,int maxiter,double epsilon,double *x0,double *Ahat, double *xout,double *CGitersout,double *Newtonitersout,double *flagout);

double dsignf(double x);

double dmaxif(double x, double y);

double dminif(double x, double y);

int imaxf(int x, int y);

double dabsf(double x);




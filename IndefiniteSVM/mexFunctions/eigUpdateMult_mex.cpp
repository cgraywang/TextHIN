// Released under GNU General Public License Version 3
// Author: Matyas Sustik 


// $Id: eigUpdateMult.C,v 1.14 2006/12/01 12:35:29 sustik Exp $
#include "mex.h"
#include "blas.h"
#include "lapack.h"
#include <math.h>
#include <string.h>
//#define INVOCATION_NAME startGdb
//#include "startgdb.h"
#ifdef WIN32
  #define for if (0); else for
  #define dgemm_ dgemm 
  #define dnrm2_ dnrm2
  #define dlaed4_ dlaed4
  #define dscal_ dscal
#endif

#define EPS (double(2.22E-16))
//#define EPS ((double)0x3cb0000000000000)


#define ARRAY_ENTRY(A_, N_, I_, J_) (A_[(I_ - 1)*N_ + J_])


// The entry point searched for and called by Matlab.  See also:
// www.mathworks.com/access/helpdesk/help/techdoc/apiref/mexfunction_c.html
void mexFunction(int nlhs, mxArray* plhs[],
		 int nrhs, const mxArray* prhs[])
{
//    startGdb(0);
    double* Vin = mxGetPr(prhs[0]);
    double* din = mxGetPr(prhs[1]);
    double* z = mxGetPr(prhs[2]);
    double rho = mxGetScalar(prhs[3]);

    ptrdiff_t N = mxGetN(prhs[0]);
    plhs[0] = mxCreateDoubleMatrix(N, N, mxREAL);
    double* Vout = mxGetPr(plhs[0]);
    plhs[1] = mxCreateDoubleMatrix(N, 1, mxREAL);
    double* dout = mxGetPr(plhs[1]);

    double TOL = 8.0*EPS;

    if (fabs(rho) < EPS) {
        memcpy(Vout, Vin, N*N*sizeof(double));
        memcpy(dout, din, N*sizeof(double));
        return;
    }

    // We perform deflation first.  There are two ways deflation can
    // occur.  Either there is a tiny entry in z or neighbouring
    // entries in din are sufficiently close.  In the second case we
    // employ a reflection that transforms one of the z entries to 0,
    // thereby reducing the case to the first one.  The reflection is
    // also applied to the original (non-perturbed) eigenvector
    // matrix.  Since we do not want to modify the argument we make a
    // copy of Vin.  We could do better, but that will be a future
    // effort.

    // Copy of Vin:

    double* Vin_defl = (double*) mxMalloc(N*N*sizeof(double));
    memcpy(Vin_defl, Vin, N*N*sizeof(double));

    // Copy of din:
    double* din_defl = (double*) mxMalloc(N*sizeof(double));
    
    // Copy of z:
    double* z_defl = (double*) mxMalloc(N*sizeof(double));
    memcpy(z_defl, z, N*sizeof(double));
    ptrdiff_t inc = 1;
    
    double normz = dnrm2_(&N,z_defl,&inc);
    
	if (normz != 1) {
		rho=rho*normz*normz;
		normz=1/normz;
		dscal_(&N,&normz,z_defl,&inc);
	}
  
    // Deflation marker:
    int* defl = (int*) mxMalloc((N+1)*sizeof(int));

    for (int i = 0; i < N-1; i++) {
        if (fabs((din[i] - din[i+1])) < TOL) {
            double t = sqrt(z_defl[i]*z_defl[i] + z_defl[i+1]*z_defl[i+1]);
            if (t == 0)
                continue;
            double c = z_defl[i+1]/t;
            double s = -z_defl[i]/t;
            z_defl[i] = 0;
            z_defl[i + 1] = t;
            for (int j = 0; j < N; j++) {
                double v1 = Vin_defl[N*i+j];
                double v2 = Vin_defl[N*(i+1)+j];
                Vin_defl[N*i+j]     = v1*c+v2*s;
                Vin_defl[N*(i+1)+j] = -v1*s+v2*c;
            }
        }
    }
    ptrdiff_t k = 0;
    for (int i = 0; i < N; i++) {
        if (fabs(z_defl[i]) < TOL)
            continue;
        din_defl[k] = din[i];
        z_defl[k] = z_defl[i];
        defl[k] = i;
        k++;
    }
    defl[k] = N; // Sentinel.

    double* U = (double*) mxMalloc(N*N*sizeof(double));
    double* U_defl = (double*) mxMalloc(k*k*sizeof(double));
    double* dout_defl = (double*) mxMalloc(k*sizeof(double));
    ptrdiff_t INFO = 0;

    for (ptrdiff_t i = 1; i <= k; i++) {
        // Calculate the ith eigenvalue of the deflated problem:
        dlaed4_(&k, &i, din_defl, z_defl, U_defl + k*(i-1), &rho,
                dout_defl + i - 1, &INFO);
    }

    if (k == 1)
        U_defl[0] = 1;
    if (k <= 2) {
        int i = 0;
        for (int col = 0; col < N; col++) {
            if (col < defl[i]) {
                // This eigenvector belongs to a deflated eigenvalue.
                memset(U+N*col, 0, N*sizeof(double));
                U[N*col + col] = 1;
                dout[col] = din[col];
                continue;
            }
            if (col < N-1 && dout_defl[i] > din[col+1]) {
                // Note that this can happen only if defl[i+1] > col + 1.
                memset(U+N*col, 0, N*sizeof(double));
                U[N*col + col + 1] = 1;
                dout[col] = din[col+1];
                continue;
            }

            // Calculate the nontrivial eigenvector:
            memset(U+N*col, 0, N*sizeof(double));
            for (int j = 0; j < k; j++)
                U[N*col + defl[j]] = U_defl[k*i+j];
            dout[col] = dout_defl[i];
            i++;
        }
        // Multiplication, Vout = Vin*U:
        double alpha = 1;
        double beta = 0;
        dgemm_("N", "N", &N, &N, &N, &alpha, Vin_defl, &N, U, &N, &beta,
               Vout, &N);
        return;
    }

    // Modify z_defl according to Ming Gu, this is needed for correct
    // eigenvectors!
    for (int i = 0; i < k; i++) {
        double di = din_defl[i];
        double s = U_defl[k*i+i];
        for (int j = 0; j < i; j++)
            s *= U_defl[k*j+i]/(di - din_defl[j]);
        for (int j = i+1; j < k; j++)
            s *= U_defl[k*j+i]/(di - din_defl[j]);
        s = sqrt(-s);
        if (z_defl[i] < 0)
            s = -s;
        z_defl[i] = s;
    }
    
    int i = 0;
    for (int col = 0; col < N; col++) {
        if (col < defl[i]) {
            // This eigenvector belongs to a deflated eigenvalue.
            memset(U+N*col, 0, N*sizeof(double));
            U[N*col + col] = 1;
            dout[col] = din[col];
            continue;
        }
        if (col < N-1 && dout_defl[i] > din[col+1]) {
            // Note that this can happen only if defl[i+1] > col + 1.
            memset(U+N*col, 0, N*sizeof(double));
            U[N*col + col + 1] = 1;
            dout[col] = din[col+1];
            continue;
        }

        // Calculate the nontrivial eigenvector:
        memset(U+N*col, 0, N*sizeof(double));
        ptrdiff_t inc = 1;
        for (int j = 0; j < k; j++)
            U_defl[k*i + j] = z_defl[j]/U_defl[k*i+j];
        double s = dnrm2_(&k, U_defl + k*i, &inc);
        for (int j = 0; j < k; j++)
            U[N*col + defl[j]] = U_defl[k*i+j]/s;
        dout[col] = dout_defl[i];
        i++;
    }

    // Multiplication, Vout = Vin*U:
    double alpha = 1;
    double beta = 0;
    dgemm_("N", "N", &N, &N, &N, &alpha, Vin_defl, &N, U, &N, &beta,
           Vout, &N);
    return;
}

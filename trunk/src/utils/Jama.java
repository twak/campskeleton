
package utils;

import Jama.Matrix;
import javax.vecmath.Matrix3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

/**
 *
 * @author twak
 */
public class Jama 
{
    /**
     * 
     * @param A Each rows of A is a cartesian point
     * @param b the offset of the matrix (normally 0?)
     * @return 
     */
    public static Vector3d solve (Matrix3d A, Tuple3d offset)
    {
        double[][] as = new double [][] { 
            { A.m00, A.m01, A.m02 },
                { A.m10, A.m11, A.m12 },
                    { A.m20, A.m21, A.m22 }
        };
         
        Matrix am = new Matrix(as);
        
        double[] bs = new double[] { offset.x, offset.y, offset.z }; 
        Matrix bm = new Matrix(bs, 3);
        
        double[][] out = am.solve(bm).getArray();
        
        // use one point on the plain to determine the offset
        double d = 
                offset.x * out[0][0] + 
                offset.y * out[1][0] + 
                offset.z * out[2][0];
        
        if (am.rank() != 3)
            return null; // not quite right, but could return a line
        
        return new Vector3d ( out[0][0], out[1][0], out[2][0]);
    }
    
    public static void main(String[] args)
    {
    }
}

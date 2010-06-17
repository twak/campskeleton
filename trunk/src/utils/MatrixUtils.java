
package utils;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Tuple3d;

/**
 *
 * @author twak
 */
public class MatrixUtils {
    
    
    public static Matrix4d fromColumns(Tuple3d a, Tuple3d b, Tuple3d c, Tuple3d d)
    {
        return new Matrix4d( 
                a.x, b.x, c.x, d.x, 
                a.y, b.y, c.y, d.y, 
                a.z, b.z, c.z, d.z,
                  0,   0,   0,   1 );
    }
    
    public static Matrix3d fromRows(Tuple3d a, Tuple3d b, Tuple3d c)
    {
        return new Matrix3d (
                a.x, a.y, a.z,
                b.x, b.y, b.z,
                c.x, c.y, c.z
                );
    }
}

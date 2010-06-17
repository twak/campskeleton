package utils;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

/**
 * Left handed, y-up coord system converter
 * @author twak
 */
public class yLeftObjDump extends ObjDump
{

    @Override
    public Tuple3d convertVertex( Tuple3d pt )
    {
        return new Point3d( pt.x, pt.z, pt.y );
    }
}

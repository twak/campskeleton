
package utils.results;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

/**
 * A marker for a result that is out of bounds.
 * @author twak
 */
public class OOB extends Point3d
{
    public OOB(Tuple3d wrap)
    {
        super(wrap);
    }
}

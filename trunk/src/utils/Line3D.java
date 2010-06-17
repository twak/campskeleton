
package utils;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

/**
 * Is this needed? very similar to linearform3d
 * @author twak
 */
public class Line3D
{
    public Point3d origin;
    public Vector3d direction;
    
    public Line3D(Tuple3d origin, Tuple3d direction)
    {
        this.origin = new Point3d( origin );
        this.direction = new Vector3d( direction );
    }

    /**
     * Project point onto line
     * @param ept
     * @return
     */
    public Point3d projectLine( Point3d ept )
    {
        return project( ept, false );
    }
    public Point3d projectSegment( Point3d ept )
    {
        return project( ept, true );
    }
    private Point3d project( Point3d ept, boolean segment )
    {
        double factor = projectParam( ept );
        if ( segment && ( factor < 0 || factor > 1 ) )
            return null;
        Point3d dest = new Point3d (direction);
        dest.scale( factor );
        dest.add( origin );
        return dest;
    }
    /**
     * The multiple of direction that must be added to origin to
     * arrive at the point where the specified point meets the
     * edge
     */
    public double projectParam (Tuple3d ept )
    {
        Vector3d b = new Vector3d( ept );
        b.sub( origin );
        double factor = direction.dot( b ) / direction.dot( direction );
        return factor;
    }
    
    public void transform (Matrix4d transform)
    {
        Point3d other = new Point3d(origin);
        other.add( direction ); // another point on the line
        
        transform.transform( origin );
        transform.transform( other  );
        
        other.sub( origin ); // convert back to a direction
        
        direction = new Vector3d ( other );
    }
    
    /**
     * Angle with plane
     */
    public double angleWith (LinearForm3D plane)
    {
        return plane.createNormalVector().angle( direction );
    }
    
    @Override
    public boolean equals( Object obj )
    {
        if (!(obj instanceof Line3D))
            return false;
        
        Line3D other = (Line3D)obj;
        return origin.equals( other.origin ) && direction.equals( other.direction );
    }

    @Override
    public String toString()
    {
        return "["+origin +"."+direction+"]";
    }

    public Point3d fromParam( double param )
    {
        Vector3d delta= new Vector3d(direction);
        delta.scale( param );
        delta.add( origin );
        return new Point3d( delta );
    }

    public static Line3D fromStartEnd( Tuple3d start, Tuple3d end )
    {
        Vector3d delta= new Vector3d(end);
        delta.sub( start );
        return new Line3D (start, delta);
    }
}

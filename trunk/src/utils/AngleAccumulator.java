package utils;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 *  Keeps track of the angles of a polygon to decide on clockwise or counterclockwise determination.
 * Should assume anti-clockwise traversal
 * @author twak
 */
public class AngleAccumulator
{
    boolean targetPositive = true;
    double angle = 0;
    Point3d firstPoint = null, lastPoint = null;
    Vector3d firstVector, lastVector = null;
    Vector3d normal = null;


    public AngleAccumulator( boolean positive, Vector3d normal )
    {
        this.normal = normal;
        this.targetPositive = positive;
    }

    public void add (Point3d pt)
    {
//        System.err.println ("adding point "+pt);
        if (lastPoint == null)
        {
            firstPoint = lastPoint = pt;
            return;
        }

        Vector3d v = new Vector3d (pt);
        v.sub( lastPoint );
        add (v);

        lastPoint = pt;
    }
    private void add (Vector3d v)
    {
        if (lastVector != null)
        {
//            System.err.println ("adding angle between "+lastVector +" and "+v);
            double dA = v.angle( lastVector );
            
            Vector3d cross = new Vector3d();
            cross.cross( lastVector, v );

            if (Math.abs( cross.length()) < 0.01)// parallel
            {
//                assert (dA < 0.001); // allow straight lines, but not very spikey things
            }
            else
            {
                if (cross.angle( normal ) > Math.PI/2 )
                    dA = -dA;
            }

            angle += dA;
        }
        else
        {
            firstVector = v;
        }

        // rotate angle around origin so normal is up
        lastVector = v;
    }

    public boolean correctAngle()
    {
        if (firstVector != null)
        {
            add( firstPoint );
            add( firstVector );
            firstVector = null; // allow method to be called 1+ time (can't add mroe points tho ;))
        }

        if ( targetPositive )
            return Math.abs( angle - Math.PI * 2 ) < 0.1;
        else
            return Math.abs( angle + Math.PI * 2 ) < 0.1;
    }

    public static void main (String[] args)
    {
        AngleAccumulator aa = new AngleAccumulator( true, new Vector3d (0,0,1));
        aa.add( new Point3d (0,0,0));
        aa.add( new Point3d (5,-5,0));
        aa.add( new Point3d (10,0,0));
        aa.add( new Point3d (10,10,0));
        aa.add( new Point3d (0,10,0));
        System.out.println (aa.correctAngle());
    }

    public static boolean sign( Loop<Point2d> loop )
    {
        AngleAccumulator aa = new AngleAccumulator( true, new Vector3d( 0, 0, 1 ) );
        for ( Point2d p : loop )
            aa.add( new Point3d( p.x, p.y, 0 ) );
        return aa.correctAngle();
    }

}

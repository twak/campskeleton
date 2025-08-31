
package org.twak.camp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import org.twak.camp.ui.Bar;
import org.twak.utils.Cache;
import org.twak.utils.Line;
import org.twak.utils.Pair;
import org.twak.utils.collections.ConsecutivePairs;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.Loopable;
import org.twak.utils.geom.Ray3d;
import org.twak.utils.geom.Line3d;
import org.twak.utils.geom.LinearForm3D;

/**
 * note: the defn of weight here is a little strange, can
 * alter it later as long as the uphill vector calculation
 * is updated too.
 * 
 * @author twak
 */
public class Edge
{
	
	private static final double COLLINEAR_THRESHOLD = 0.01; // angle in radians
	private static final double COS_THRESHOLD = Math.cos(COLLINEAR_THRESHOLD*COLLINEAR_THRESHOLD);
	
    public Corner start, end;

    // 0 is straight up, positive/-ve is inwards/outwards, absolute value must be less than Math.PI/2
    private double angle = Math.PI/4;
    
    // orthogonal vector pointing uphill
    public Vector3d uphill;
    public LinearForm3D linearForm;
    
    // corners that currently reference this edge in prevL or nextL
    public List<Corner> currentCorners = new ArrayList<>();

    public Machine machine;

    // features that this edge has been tagged with
    public Set<Tag> profileFeatures = new LinkedHashSet<Tag>();


    public Edge (Corner start, Corner end, double angle)
    {
        this (start, end);
        this.angle = angle;

        calculateLinearForm();
    }

    /**
     * adds an output side from start to end => Edges are immutable!
     */
    public Edge (Corner start, Corner end)
    {
        this.start = start;
        this.end = end;
//        addOutputSide ( start, end );
    }

    public Edge (Point3d start, Point3d end, double angle)
    {
        this (new Corner (start), new Corner (end), angle );
    }

    /**
     * The perpendicular unit vector pointing up the slope of the side
     */
    private void calculateUphill() {
        // Compute the horizontal direction components directly
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        // Compute the inverse length (normalization factor)
        double normInv = 1.0 / Math.sqrt(dx * dx + dy * dy);
        
        double sinA = Math.sin(angle);
        double cosA = Math.cos(angle);
        
        // The perpendicular (in x,y) of (dx, dy) normalized is (-dy/length, dx/length).
        // Multiply by sin(angle) and add vertical component cos(angle)
        uphill = new Vector3d(-dy * sinA * normInv,
                              dx * sinA * normInv,
                              cosA);
    }
    
    public double[] getBBox() {
        double minX = Math.min(start.x, end.x);
        double minY = Math.min(start.y, end.y);
        double maxX = Math.max(start.x, end.x);
        double maxY = Math.max(start.y, end.y);
        return new double[]{minX, minY, maxX, maxY};
    }
    
    /**
     * finds the Ax + By + Cz = D form of the edge
     * Called when the the weight of the edge changes
     */
    public void calculateLinearForm()
    {
        calculateUphill();
        
        // find normal from uphill and edge
        Vector3d norm = getPlaneNormal();
        
        linearForm = new LinearForm3D( norm, new Vector3d(start.x, start.y, start.z)  );
    }
    
    /**
     * The normal the edge
     */
    public Vector3d getPlaneNormal()
    {
        Vector3d a = direction();
        a.normalize();
        a.cross(a, uphill);
        return a;
    }
    
    public double length() {
    	return start.distance( end );
    }
    
    public Vector3d direction() {
        return new Vector3d(end.x - start.x, end.y - start.y, 0);
    }


    public Line projectDown()
    {
        return new Line( start.x, start.y, end.x, end.y );
    }

    @Override
    public String toString()
    {
        return "["+start+","+end+"]";
    }

    public double distance( Point3d ept )
    {	
        Vector3d e = new Vector3d( end );
        e.sub( start );
        Point3d p = new Ray3d( start, e ).projectSegment( ept );

        if (p == null)
            return Double.MAX_VALUE;

        return p.distance( ept );
    }

    public boolean isCollisionNearHoriz(Edge other)
    {
    	Ray3d r = collide( other.linearForm );
    	
    	if (r == null)
    		return false;
    	
         return Math.abs( r.direction.z ) < 0.001;
    }
    
    private Ray3d collide(LinearForm3D other) {
        // Plane 1: A*x + B*y + C*z + D = 0, so d1 = -D, n1 = (A, B, C)
        // Plane 2: other.A*x + other.B*y + other.C*z + other.D = 0, so d2 = -other.D, n2 = (other.A, other.B, other.C)
        double n1x = linearForm.A, n1y = linearForm.B, n1z = linearForm.C;
        double n2x = other.A, n2y = other.B, n2z = other.C;
        
        // Compute the cross product: r = n1 x n2, the direction of the intersection line.
        double rx = n1y * n2z - n1z * n2y;
        double ry = n1z * n2x - n1x * n2z;
        double rz = n1x * n2y - n1y * n2x;
        
        // If the two plane normals are (nearly) parallel, then r will be near 0.
        double rnormSq = rx * rx + ry * ry + rz * rz;
        if (rnormSq == 0) {
            return null;
        }
        
        // Compute d1 and d2 from the plane equations.
        double d1 = -linearForm.D;
        double d2 = -other.D;
        
        // Compute the vector w = d1*n2 - d2*n1.
        double wx = d1 * n2x - d2 * n1x;
        double wy = d1 * n2y - d2 * n1y;
        double wz = d1 * n2z - d2 * n1z;
        
        // Now compute the particular solution point p = w x r / ||r||².
        double px = wy * rz - wz * ry;
        double py = wz * rx - wx * rz;
        double pz = wx * ry - wy * rx;
        
        double invRnormSq = 1.0 / rnormSq;
        px *= invRnormSq;
        py *= invRnormSq;
        pz *= invRnormSq;
        
        Point3d point = new Point3d(px, py, pz);
        Vector3d direction = new Vector3d(rx, ry, rz);
        return new Ray3d(point, direction);
    }


	public boolean isParallel(Edge other) {
		return isAligned(uphill, other.uphill) && isAligned(direction(), other.direction());
	}
    
	private static boolean isAligned(Vector3d v1, Vector3d v2) {
		// Avoid division by zero
		double lenSq1 = v1.lengthSquared();
		double lenSq2 = v2.lengthSquared();
		if (lenSq1 == 0.0 || lenSq2 == 0.0) {
			return false;
		}

		// Compare squared quantities to avoid square root
		double dotProduct = v1.dot(v2);
		double squaredDot = dotProduct * dotProduct;
		double threshold = COS_THRESHOLD * COS_THRESHOLD * lenSq1 * lenSq2;

		return squaredDot >= threshold;
	}
    
    /**
     * Do these two edges go in the same direction and are they collinear?
     * (Are they parallel and go through the same point?)
     */
    public boolean sameDirectedLine( Edge nextL )
    {
    	return nextL.direction().angle( direction() ) < COLLINEAR_THRESHOLD && 
    			Math.abs( getAngle() - nextL.getAngle() ) < COLLINEAR_THRESHOLD;
    }

    /**
     * reverses the start & ends of each edge in a loop, but doesn't change the
     * corner-chain order (this is done by skeleton as it sets up)
     * 
     */
    public static void reverse (LoopL<Edge> loopl)
    {
        for (Edge e : loopl.eIterator())
        {
            Corner tmp = e.start;
            e.start = e.end;
            e.end = tmp;

            tmp = e.start.nextC;
            e.start.nextC = e.start.prevC;
            e.start.prevC = tmp;

            Edge et = e.start.nextL;
            e.start.nextL = e.start.prevL;
            e.start.prevL = et;
        }

        for (Loop<Edge> l : loopl)
            l.reverse();
    }

    @Override
    public int hashCode()
    {
        /**
         * Memo to self, we rely on the hashcode not changing when we edit the point's locations in wiggleui
         */
        return super.hashCode();
    }


    public static Loop<Edge> fromPoints( List<Point2d> ribbon )
    {
        Loop<Edge> loop = new Loop();
        Cache<Point2d, Corner> cache = new Cache<Point2d, Corner>()
        {
            @Override
            public Corner create( Point2d i )
            {
                return new Corner (i.x, i.y);
            }
        };
        for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( ribbon, true ) )
        {
            loop.append( new Edge( cache.get( pair.first() ), cache.get( pair.second() ) ) );
        }

        return loop;
    }


    /**
     * UNTESTED!
     * @param ribbon
     * @return
     */
    public static LoopL<Edge> fromPoints2d( LoopL<Point2d> ribbon )
    {
        LoopL<Edge> loopl = new LoopL();


        Cache<Point2d, Corner> cache = new Cache<Point2d, Corner>()
        {
            @Override
            public Corner create( Point2d i )
            {
                return new Corner (i.x, i.y);
            }
        };

        for (Loop<Point2d> pLoop : ribbon)
        {
            Loop<Edge> loop = new Loop();
            loopl.add( loop );
            for ( Loopable<Point2d> pair : pLoop.loopableIterator() )
                loop.append( new Edge( cache.get( pair.get() ), cache.get( pair.getNext().get() ) ) );

        }

        return loopl;
    }

     public static LoopL<Edge> dupe( LoopL<Edge> ribbon )
    {
        LoopL<Edge> loopl = new LoopL();


        Cache<Corner, Corner> cache = new Cache<Corner, Corner>()
        {
            @Override
            public Corner create( Corner i )
            {
                return new Corner (i.x, i.y);
            }
        };

        for (Loop<Edge> pLoop : ribbon)
        {
            Loop<Edge> loop = new Loop();
            loopl.add( loop );
            for ( Edge bar : pLoop )
                loop.append( new Edge( cache.get( bar.start ), cache.get( bar.end ) ) );

        }

        return loopl;
    }

    public static LoopL<Edge> fromBar( LoopL<Bar> ribbon )
    {
        LoopL<Edge> loopl = new LoopL();


        Cache<Point2d, Corner> cache = new Cache<Point2d, Corner>()
        {
            @Override
            public Corner create( Point2d i )
            {
                return new Corner (i.x, i.y);
            }
        };

        for (Loop<Bar> pLoop : ribbon)
        {
            Loop<Edge> loop = new Loop();
            loopl.add( loop );
            for ( Bar bar : pLoop )
                loop.append( new Edge( cache.get( bar.start ), cache.get( bar.end ) ) );

        }

        return loopl;
    }


    public double getAngle()
    {
        return angle;
    }

    public void setAngle( double angle )
    {
        this.angle = angle;
        calculateLinearForm();
    }

    public static Iterable<Edge> uniqueEdges (LoopL<Corner> corners)
    {
        Set<Edge> cs = new HashSet();
        for (Corner c : corners.eIterator())
            cs.add(c.nextL);
        return cs;
    }


    /**
     *
     * This is a robust collision of a's adjacent edges with a horizontal plane at the given height
     * When Two parallel edges are given, we can assume that the
     */
    public static Tuple3d collide (Corner a, double height)
    {
        LinearForm3D ceiling = new LinearForm3D( 0, 0, 1, -height );
            try {
                return ceiling.collide(a.prevL.linearForm, a.nextL.linearForm);
            } catch (RuntimeException e) {
                assert (a.prevL.sameDirectedLine(a.nextL));

                // a vector in the direction of uphill from a
                Vector3d dir = new Vector3d ( a.prevL.uphill );
                dir.normalize();
                // via similar triangle (pyramids)
                dir.scale(height - a.z);
                dir.add(a);

                //assume, they're all coincident?
                return new Point3d (dir.x, dir.y, height);
            }
    }

    /**
     * Collides a's two adjacent edges against the other given edge.
     *
     * Currently this handles the case that a.next, a.prev is
     *
     * @return
     */
    static Tuple3d collide(Corner a, Edge edge)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Set<Corner> findLeadingCorners()
    {
        Set<Corner> out = new HashSet();
        for (Corner c : currentCorners)
            if (c.nextL == this)
                out.add(c);
        return out;
    }

	public Line3d line() {
		return new Line3d( start, end );
	}

}


package org.twak.camp;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
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
    public Corner start, end;

    // 0 is straight up, positive/-ve is inwards/outwards, absolute value must be less than Math.PI/2
    private double angle = Math.PI/4;
    
    // orthogonal vector pointing uphill
    public Vector3d uphill;
    public LinearForm3D linearForm;
    
    // corners that currently reference this edge in prevL or nextL
    public Set<Corner> currentCorners = new LinkedHashSet();

    public Machine machine;

    // features that this edge has been tagged with
    public Set<Tag> profileFeatures = new LinkedHashSet();


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
    private void calculateUphill()
    {
        Vector3d vec = direction();
        
        // perpendicular in x,y plane
        vec = new Vector3d ( -vec.y, vec.x, 0 );
        vec.normalize();

        // horizontal component
        vec.scale( Math.sin(getAngle()) );

        // vertical component
        vec.add( new Vector3d (0,0,Math.cos( getAngle()) ) );
        
        uphill = vec;
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
    
    public Vector3d direction()
    {
        Vector3d vec = new Vector3d ( this.end.x, this.end.y, 0 );
        vec.sub( new Vector3d ( this.start.x, this.start.y, 0 ));
        return vec;
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
    	Ray3d r = linearForm.collide( other.linearForm );
    	
    	if (r == null)
    		return false;
    	
         return Math.abs( r.direction.z ) < 0.001;
    }

//    public boolean isParallel(Edge other)
//    {
//        Ray3d r = linearForm.collide( other.linearForm );
//
//        if (r == null)
//            return true;
//
//        return Math.abs( r.direction.z ) < 0.001;
//    }
    
    /**
     * Do these two edges go in the same direction and are they coliniear.
     * (Are they parallel and go through the same point?)
     */
    public boolean sameDirectedLine( Edge nextL )
    {
        return nextL.direction().angle( direction() ) < 0.01 && Math.abs( getAngle() - nextL.getAngle() ) < 0.01;
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

        // this can cause Jama not to return...
        if ( a.prevL.linearForm.hasNaN() || a.nextL.linearForm.hasNaN() )
                    throw new Error();

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

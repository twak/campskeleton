
package utils;

import java.awt.Point;
import java.util.Comparator;
import javax.swing.text.Segment;
import javax.vecmath.Point2d;
import javax.vecmath.Tuple2d;
import javax.vecmath.Vector2d;

/**
 *
 * @author twak
 */
public class Line 
{
    public Point2d start, end;
    
    public Line() {};
    
    public Line (Point2d start, Point2d end)
    {
        this.start = start;
        this.end = end;
    }
    
    public Line (Point start, Point end)
    {
        this.start = new Point2d(start.x, start.y);
        this.end = new Point2d(end.x, end.y);
    }
    
    public Line ( double x1, double y1, double x2, double y2)
    {
        this (new Point2d(x1,y1), new Point2d (x2, y2));
    }

    /**
     * Line intersecting a line segment
     */
    public Point2d intersects (LinearForm other)
    {
        Point2d location = new LinearForm( this ).intersect( other );

        if (location == null) // parallel
            return null;

        if ( location.x >= Math.min (start.x, end.x) &&
                location.x <= Math.max (start.x, end.x) &&
                location.y >= Math.min (start.y, end.y) &&
                location.y <= Math.max (start.y, end.y) )
            return location;

        return null;
    }

    public Point2d intersects (Line other)
    {
        return this.intersects( other, true );
    }

    public Point2d intersects (Line other, boolean doClip)
    {
        // drop down to matrix form  - could refac w linear form?
        double a1 = end.y - start.y;
        double b1 = start.x-end.x;
        double c1 = a1 * start.x + b1 * start.y;
        
        double a2 = other.end.y - other.start.y;
        double b2 = other.start.x-other.end.x;
        double c2 = a2 * other.start.x + b2 * other.start.y;
        
        double det = a1 * b2 - a2 * b1;
        
        if (det == 0)
            return null; // parallel lines!
        
        double x = (b2 * c1 - b1 * c2) / det;
        double y = (a1 * c2 - a2 * c1) / det;

        if (!doClip)
            return new Point2d(x,y);

        double tol = 0.000001;
        // lines cross...now check segments ( this is the place to add and remove tolerances from)
        if ( x >= Math.min( start.x, end.x ) - tol &&
                x <= Math.max( start.x, end.x ) + tol &&
                y >= Math.min( start.y, end.y ) - tol &&
                y <= Math.max( start.y, end.y ) + tol &&
                
                x >= Math.min( other.start.x, other.end.x ) - tol &&
                x <= Math.max( other.start.x, other.end.x ) + tol &&
                y >= Math.min( other.start.y, other.end.y ) - tol &&
                y <= Math.max( other.start.y, other.end.y ) + tol )
            return new Point2d(x,y);
        
        // no intersect :(
        return null;
    }

    @Override
    public String toString()
    {
        return start +" to "+ end;
    }
    
    public double xAtY( double y )
    {
        double a1 = end.y - start.y;
        double b1 = start.x-end.x;
        double c1 = a1 * end.x + b1 * end.y;
        
        if (a1 == 0) // parallel to x axis
            return start.x;
                
        return (c1 - b1 * y)/a1;
    }
    
    public boolean isHoriz()
    {
        return start.y == end.y;
    }
    
    public boolean isVert()
    {
        return start.x == end.x;
    }

    @Override
    public boolean equals( Object obj )
    {
        if (!(obj instanceof Line))
            return false;
        
        Line line = (Line)obj;
        
        return start.equals( line.start ) && end.equals( line.end );
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 53 * hash + ( this.start != null ? this.start.hashCode() : 0 );
        hash = 53 * hash + ( this.end != null ? this.end.hashCode() : 0 );
        return hash;
    }
    
    public Point2d getOther (Point2d p)
    {
        if (start.equals(p))
            return end;
        
        if (end.equals(p))
            return start;
        
        if (start.equals( end ))
            return start;
        
        throw new Error("There is no other");
    }
    
    public double lengthSquared()
    {
        return Math.pow(end.x- start.x, 2) + Math.pow (end.y - start.y,2);
    }
    
    public double length()
    {
        return Math.sqrt( lengthSquared() );
    }
    
    /**
     * Distance to infinite line
     */
    public double distance (Tuple2d p)
    {
        double num = Math.abs((end.x - start.x) * ( start.y - p.y) - (start.x -p.x)*(end.y - start.y));
        double den = length();
        if (den == 0)
            return new Point2d(p).distance(start);
        return num/den;
    }

    public double findPPram(Point2d pt)
    {
        Vector2d v1 = new Vector2d( end );
        v1.sub( start );
        Vector2d v2 = new Vector2d ( pt );
        v2.sub( start );
        return v1.dot( v2 ) / v1.dot( v1);
    }

    public Point2d project( Point2d pt, boolean clamp )
    {
        Vector2d v1 = new Vector2d( end );
        v1.sub( start );
        Vector2d v2 = new Vector2d ( pt );
        v2.sub( start );
        double param = v2.dot( v1 ) / v1.length();

        if (clamp)
            param = MUtils.clamp( param, 0, v1.length() );

        v1.normalize();
        v1.scale( param );
        v1.add( start );
        
        return new Point2d (v1);
    }

    /**
     * returns a new line clipped to the given infinite lines. Slightly broken
     * in the treatment of inifinte lines (intersepcsP...)
     */
    public Line clip (Line line, LinearForm a, LinearForm b)
    {
        if (line == null)
            return line;

        Point2d start = line.start;
        Vector2d dir = new Vector2d( line.end );
        dir.sub( line.start );

        double aC = a.intersectsP(start, dir);
        double bC = b.intersectsP(start, dir);

        if (aC < 0 && bC < 0)
            return null;
        if (aC > 1 && bC > 1)
            return null;

        // fixme!
        double s = 0, e = 1;
        if (aC < bC)
        {
            if (aC < Double.POSITIVE_INFINITY)
                s = Math.max( aC, s );
            if (bC < Double.POSITIVE_INFINITY)
                e = Math.min( bC, e );
        }
        else
        {
            if (bC < Double.POSITIVE_INFINITY)
                s = Math.max( bC, s );
            if (aC < Double.POSITIVE_INFINITY)
                e = Math.min( aC, e );
        }

        Vector2d s2 = new Vector2d( start );
        Vector2d d2 = new Vector2d( dir );
        d2.scale( s );
        s2.add( d2 );

        Vector2d s3 = new Vector2d( start );
        Vector2d d3 = new Vector2d( dir );
        d3.scale( e );
        s3.add( d3 );

        return new Line( new Point2d( s2 ), new Point2d( s3 ) );
    }

    public static class AlongLineComparator implements Comparator<Point2d>
    {
        Line line;

        public AlongLineComparator( Point2d start, Point2d end )
        {
            line = new Line( start, end );
        }

        @Override
        public int compare( Point2d o1, Point2d o2 )
        {
            return Double.compare( line.findPPram( o2 ), line.findPPram( o2 ) );
        }
    }
}

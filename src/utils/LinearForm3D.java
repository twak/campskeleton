
package utils;

import Jama.Matrix;
import javax.vecmath.Matrix3d;
import utils.results.OOB;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import utils.results.LineOnPlane;

/**
 * Definition of a plane in form
 * Ax + By + Cz + D = 0
 * @author twak
 */
public class LinearForm3D implements Cloneable
{
    double 
            A, //x
            B, //y
            C, //z
            D; //offset

    public LinearForm3D ( LinearForm3D l )
    {
        this.A = l.A;
        this.B = l.B;
        this.C = l.C;
        this.D = l.D;
    }

    /**
     * @param normal normal to the plane
     * @param offset a point the plane passes through
     */
    public LinearForm3D (Vector3d normal, Tuple3d offset)
    {
        A = normal.x;
        B = normal.y;
        C = normal.z;
        D = -normal.dot( new Vector3d( offset) );
    }

    public LinearForm3D (double A,double B,double C,double D )
    {
        this.A = A;
        this.B = B;
        this.C = C;
        this.D = D;
    }

    public double pointDistance (Tuple3d point)
    {
        double den = Math.sqrt( A*A + B*B + C*C );
        if (den == 0)
            throw new Error("I'm not a plane " + this +"!");
        double num = A * point.x + B * point.y + C * point.z + D;
        return num/den;
    }   
    
    /**
     * @return a Point3d on success, 
     * an OOB on a on-line but out of range, 
     * a LineOnPlane for co-incident (to a tolerance) line/plane
     * and null for "they're fuppin parallel and miles apart"
     */
    
    public Point3d collide (Tuple3d rayOrigin, Tuple3d rayDirection)
    { return collide(rayOrigin, rayDirection, null); }
    
    public Point3d collide (Tuple3d rayOrigin, Tuple3d rayDirection, Double distance)
    {
        Vector3d direction = new Vector3d(rayDirection);
//        direction.normalize();
        
        // erm... dot product?
        double den = A * direction.x + B * direction.y + C *direction.z;
        
        // ray is parallel to plane, check for co-incidence
        if (den == 0)
        {
            if (pointDistance(rayOrigin) < 0.0001) // haven't tested this yet
            {
                LineOnPlane out = new LineOnPlane(rayOrigin, rayDirection, distance);
                return out;
            }
            
            return null; // not going to collide
        }
        
        double num =-D - A * rayOrigin.x - B * rayOrigin.y - C * rayOrigin.z;
        
        // parameter n is multiple of direction vector away from origin
        double n = num/den;
        
        direction.scale(n);
        direction.add(rayOrigin);
        
        if (n < 0)
            return new OOB(direction); // plane too early
        
        if (distance != null && distance != Double.POSITIVE_INFINITY)
        {
            if (distance < n)
                return new OOB(direction); // plane too late
        }
        
        return new Point3d(direction);
    }
    
    /**
     * Not a complete collision, compares normals to determine
     * direction of line-intersection
     * 
     * @param other the plane to collide with this one
     * @return null if planes are parallel, direction of collision line otherwise
     */
    public Vector3d collideToVector (LinearForm3D other)
    {
        // find the vector that occurs when both planes collide
        Vector3d n = createNormalVector();
        n.cross(n, other.createNormalVector());
        
        return n;
    }
    
    /**
     * Returns a line or null if the planes are parallel
     * @param other
     * @return
     */
    public Line3D collide (LinearForm3D other)
    {
        // special solution is cross product of normals
        Vector3d spec = new Vector3d();
        spec.cross( createNormalVector(), other.createNormalVector() );
        
        if (spec.length() == 0) // planes are parallel
        {
            return null;
        }
        
        // particular solution can be found by solving the equation set of the two planes, and another perpendicular plane
        Matrix matrixA = new Matrix  ( new double[][] { { A, B, C }, {other.A, other.B, other.C}, {spec.x, spec.y, spec.z } } );
        // offset of perp plane can be 0, goes through the origin
        Matrix matrixB = new Matrix ( new double[][] {{-D},{-other.D}, {0} } );
        
        Matrix res = matrixA.solve(matrixB);
        return new Line3D (new Point3d(res.get(0, 0), res.get(1, 0), res.get(2, 0) ), spec);
    }
    
    /**
     * Collide three planes to a point
     */
    public Tuple3d collide (LinearForm3D b, LinearForm3D c)
    {
        LinearForm3D a = this;
        
        Matrix3d three = new Matrix3d (
                a.A, a.B, a.C, 
                b.A, b.B, b.C,
                c.A, c.B, c.C );
        
        Point3d offset = new Point3d (-a.D, -b.D, -c.D);
        
        return Jama.solve(three, offset);
    }
    
    public Vector3d createNormalVector()
    {
        Vector3d out = new Vector3d (A,B,C);
        out.normalize();
        return out;
    }
    
    @Override
    public String toString()
    {
        return A+","+B+","+C+","+D;
    }

    @Override
    public boolean equals( Object obj )
    {
        if (!(obj instanceof LinearForm3D))
            return false;
        
        LinearForm3D other = (LinearForm3D)obj;
        return A == other.A && B == other.B && C == other.C && D == other.D;
    }
    
    public static void main (String[] args)
    {
        LinearForm3D l1 = new LinearForm3D( 
                new Vector3d(0,1,0), 
                new Point3d( 0, 0, 0 )
                );
        
        LinearForm3D l2 = new LinearForm3D( 
                new Vector3d(1,0,0), 
                new Point3d( 0, 0, 0 )
                );
        
        System.out.println( "result: " + l1.collide( l2 ) );
    }

    public Vector3d normal()
    {
        return new Vector3d (A,B,C);
    }

    /**
     * Keeps plane in the same place, but flips the normal
     */
    public void flipNormal()
    {
        A = -A;
        B = -B;
        C = -C;
        D = -D;
    }

    @Override
    public LinearForm3D clone()
    {
        return new LinearForm3D( A,B,C,D );
    }

    public boolean inFront( Tuple3d p )
    {
        return A * p.x + B * p.y + C * p.z + D > 0;
    }

    public boolean hasNaN()
    {
        return
                Double.isNaN( A ) ||
                Double.isNaN( B ) ||
                Double.isNaN( C ) ||
                Double.isNaN( D );
    }
}

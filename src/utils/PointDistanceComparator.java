package utils;

import java.util.Comparator;
import javax.vecmath.Point2d;

/**
 *
 * @author twak
 */
public class PointDistanceComparator implements Comparator<Point2d>
{
    Point2d start2d;
    
    public PointDistanceComparator(Point2d origin)
    {
        this.start2d = origin;
    }
    
    public int compare( Point2d o1, Point2d o2 )
    {
        return Double.compare( start2d.distanceSquared( o1 ), start2d.distanceSquared( o2 ) );
    }
}

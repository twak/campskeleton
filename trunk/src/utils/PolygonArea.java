package utils;

import javax.vecmath.Point2d;

/**
 *
 * @author twak
 */
public class PolygonArea
{
    double area = 0;

    Point2d trailing = null, start = null;

    public void add (Point2d pt)
    {
        if (start == null)
            start = pt;

        if (trailing == null)
            trailing = pt;
        else
        {
            area += (trailing.x * pt.y) - (trailing.y * pt.x);

            trailing = pt;
        }
    }

    public double area()
    {
        add (start);
        return area/2;
    }
}

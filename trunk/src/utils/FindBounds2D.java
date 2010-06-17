package utils;

import javax.vecmath.Tuple2d;

/**
 *
 * @author twak
 */
public class FindBounds2D
{
    public double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE,
            minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

    public void add(Tuple2d tuple)
    {
        minX = Math.min( tuple.x, minX );
        maxX = Math.max( tuple.x, maxX );
        minY = Math.min( tuple.y, minY );
        maxY = Math.max( tuple.y, maxY );
    }

    public double getWidth()
    {
        return maxX - minX;
    }
    public double getHeight()
    {
        return maxY - minY;
    }

    public void move( double x, double y )
    {
        minX += x;
        maxX += x;
        minY += y;
        maxY += y;
    }

    public void increaseMax( double x, double y )
    {
        maxX += x;
        maxY += y;
    }
}

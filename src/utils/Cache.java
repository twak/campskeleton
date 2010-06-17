package utils;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.vecmath.Point2d;

/**
 *
 * @author twak
 */
public abstract class Cache<I,O>
{
    public Map<I,O> cache = new LinkedHashMap();

    public O get( I in )
    {
        O o = cache.get( in );
        if ( o == null )
            cache.put( in, o = create( in ) );
        return o;
    }

    public abstract O create(I i);

    public void put( I start, O start0 )
    {
        cache.put( start, start0);
    }
}

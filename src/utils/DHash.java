
package utils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Two directional hash map
 * @author twak
 */
public class DHash<A,B>
{
    public Map <A,B> ab = new LinkedHashMap();
    public Map <B,A> ba = new LinkedHashMap();

    public void clear()
    {
        ab.clear();
        ba.clear();
    }
    
    public void put (A a, B b)
    {
        ab.put(a,b);
        ba.put(b,a);
    }
    
    public B get (A a)
    {
        return ab.get(a);
    }
    
    public A teg (B b)
    {
        return ba.get(b);
    }

    public boolean containsA(A a)
    {
        return ab.containsKey( a );
    }
    
    public boolean containsB(B b)
    {
        return ba.containsKey( b );
    }

    @Override
    public int hashCode()
    {
        return ab.hashCode() + ba.hashCode();
    }

    @Override
    public boolean equals( Object obj )
    {
        if (obj instanceof DHash)
        {
            DHash o = (DHash)obj;
            return o.ab.equals( ab ) && o.ba.equals( ba );
        }
        return false;
    }

    public void removeA(A a)
    {
        ba.remove( get ( a ) );
        ab.remove( a );
    }

    public void removeB (B b)
    {
        ba.remove( b );
        ab.remove( teg ( b ) );
    }

    public ManyManyMap<A,B> toManyMany()
    {
        ManyManyMap<A,B> out = new ManyManyMap<A, B>();

        for (A a : ab.keySet())
            out.addForwards(a, ab.get(a));

        for (B b : ba.keySet())
            assert (out.getPrev(b) == ba.get( b ));

        return out;
    }

    public DHash<A, B> shallowDupe() {
        DHash<A, B> out = new DHash();
        out.ab.putAll( ab );
        out.ba.putAll( ba );
        return out;
    }

    /**
     * replaces all occurances of B, with the value given by the cache.
     *
     * if cache response is null, entry is removed from both maps
     *
     */
    public void remapB( Cache<B, B> cache )
    {
        Map<B,A> newBA = new LinkedHashMap<B, A>();
        Map<A,B> newAB = new LinkedHashMap<A, B>();

        for (Map.Entry<A,B> e : ab.entrySet())
        {
            B newB = cache.get( e.getValue());
            
            if (newB != null)
            {
                newBA.put( newB, e.getKey() );
                newAB.put( e.getKey(), newB );
            }
        }

        ba = newBA;
        ab = newAB;
    }

    public Cache<A,B> asCache()
    {
        return new Cache<A,B>() {

            @Override
            public B create( A i )
            {
                throw new Error(); // shouldn't get here
            }

            @Override
            public B get( A in )
            {
                return ab.get( in );
            }
        };
    }
}

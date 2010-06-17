package utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * HashMap backed mutli-item hash. eg a list for every entry in the hash table
 * @author twak
 */
public class MultiMap <A,B> //implements Map<A,List<B>>
{
    public Map <A, List<B>> map = new LinkedHashMap();

    public void addEmpty( A a )
    {
        if (!map.containsKey( a ))
            map.put ( a, new ArrayList());
    }

    public int size()
    {
        return map.size();
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean containsKey( Object key )
    {
        return map.containsKey( key );
    }

    public boolean containsValue( Object value )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public List<B> get( A key )
    {
        List<B> out = map.get( key );
        if (out == null)
            return new ArrayList();
        return out;
    }

    public List<B> getOrAdd( A key )
    {
        List<B> out = map.get( key );
        if ( out == null )
        {
            List<B> b = new ArrayList();
            map.put( key, b );
            return b;
        }
        return out;
    }

    public boolean remove (A key, B value)
    {
        List<B> out = map.get( key );
        if (out == null)
            return false;

        return out.remove( value );
    }

    public void put ( A key, B value )
    {
        List<B> out = map.get( key );
        if (out == null)
        {
            out = new ArrayList();
            map.put( key, out );
        }
        out.add( value );
    }

    public void put ( A key, B value, boolean dupeCheck )
    {
        List<B> out = map.get( key );
        if ( out == null )
        {
            out = new ArrayList();
            map.put( key, out );
        }
        if (!dupeCheck || !out.contains( value) )
            out.add( value );
    }

    /**
     * Removes entire list indexed by key
     * @param key
     * @return
     */
    public List<B> remove( A key )
    {
        return map.remove( key );
    }

    public void putAll( Map<? extends A, ? extends B> m )
    {
        for (Map.Entry<? extends A, ? extends B> entry : m.entrySet())
            put( entry.getKey(), entry.getValue() );
    }

    public void clear()
    {
        map.clear();
    }

    public Set<A> keySet()
    {
        return map.keySet();
    }

    public Collection<List<B>> values()
    {
        return map.values();
    }

    public Set<Entry<A, List<B>>> entrySet()
    {
        return map.entrySet();
    }

    public void putAll( A a, Iterable<B> bs, boolean dupeCheck )
    {
        for (B b : bs)
            put( a, b, dupeCheck);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("[\n");
        for (A a : map.keySet())
        {
            sb.append( a.toString() +" || ");
            for (B b : map.get( a ))
                sb.append( b +", ");
            sb.append( "\n" );
        }
        sb.append("]\n");
        return sb.toString();
    }

    public void removeAll( A...a )
    {
        for (A aa : a)
            remove( aa );
    }
}

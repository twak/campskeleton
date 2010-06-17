package utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Given a function to run on a set (getObject) and a iterable set, we only return
 * unique results (.equals()) from that function applied over the set.
 * @author twak
 */
public abstract class ToStrictSetIerable<A, O> implements Iterable<O>, Iterator<O>
{
    Set<Object> seen = new HashSet();
    Iterable<A> in;
    Iterator<A> inIt;
    O next = null;

    
    public ToStrictSetIerable (Iterable<A> in)
    {
        this.in = in;
        inIt = in.iterator();
        findNext();
    }

    @Override
    public Iterator<O> iterator()
    {
        return this;
    }

    void findNext()
    {
        while (inIt.hasNext())
        {
            O key = getObject( inIt.next() );
            if (!seen.contains( key ))
            {
                seen.add( key );
                next = key;
                return;
            }
        }
        next = null;
    }

    @Override
    public boolean hasNext()
    {
        return next != null;
    }

    @Override
    public O next()
    {
        O out = next;
        findNext();
        return out;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException( "Not supported evar." );
    }

    public abstract O getObject(A a);
}

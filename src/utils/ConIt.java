package utils;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * concurrent iterator
 */
public class ConIt <A> implements Iterator<List<A>>
{
    Iterator<A>[] its;

    List<A> b;

    public ConIt (Iterable<A> ... a)
    {
        its = new Iterator[a.length];
        for (int i = 0; i < a.length; i++)
        {
            its[i] = a[i].iterator();
        }
    }

    public boolean hasNext()
    {
        for (Iterator<A> ita : its)
            assert (ita.hasNext() == its[0].hasNext());
        
        return its[0].hasNext();
    }

    public List<A> next()
    {
        b = new ArrayList<A>();

        for ( int i = 0; i < its.length; i++ )
            b.add( its[i].next() );

        return b;
    }

    public A get( int i )
    {
        return b.get(i);
    }

    public void remove()
    {
        for (Iterator<A> ita : its)
            ita.remove();
    }
}

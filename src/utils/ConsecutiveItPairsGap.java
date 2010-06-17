package utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator that returns all consecutive pairs with a gap between - eg each E is
 * returned in only one pair.
 *
 * @author twak
 */
public class ConsecutiveItPairsGap <E> implements Iterator<Pair<E,E>>, Iterable<Pair<E,E>>
{
    Iterator<E> it;

    E prev;

    Pair <E,E> next = null;

    /**
     *
     * @param input the list to returns triples from
     * @param loop is it cyclic? (do we return {end-1, end, start} etc...))
     */
    public ConsecutiveItPairsGap (Iterable<E> input)
    {
        this.it = input.iterator();
        next = findNext();
    }

    private Pair<E,E> findNext()
    {
        Pair<E,E> out = new Pair();
        if (it.hasNext())
        {
            out.set1( it.next() );
            if (it.hasNext())
            {
                out.set2( it.next());
                return out;
            }
            else
                return null;
        }
        else return null;


    }

    public boolean hasNext()
    {
        return next != null;
    }

    public Pair<E,E> next()
    {
        Pair<E,E> out = next;
        next = findNext();
        return out;
    }

    public void remove()
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Iterator<Pair<E, E>> iterator()
    {
        return this;
    }
    
    public static void main (String[] args)
    {
        List<Integer> list = new ArrayList();
        list.add( 1 );
        list.add( 2 );
        list.add( 3 );
        list.add( 4 );
//        list.add( 5 );
        for ( Pair <Integer, Integer> t : new ConsecutiveItPairsGap<Integer>( list ))
        {
            System.out.println(t);
        }
    }

}

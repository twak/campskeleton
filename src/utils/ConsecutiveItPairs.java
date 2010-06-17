package utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator that returns all consecutive triples in a list.
 *
 * If size is 1 and we're looping we return one pair of (elment 1, element 1)
 *
 * @author twak
 */
public class ConsecutiveItPairs <E> implements Iterator<Pair<E,E>>, Iterable<Pair<E,E>>
{
    Iterator<E> it;

    E prev;

    /**
     *
     * @param input the list to returns triples from
     * @param loop is it cyclic? (do we return {end-1, end, start} etc...))
     */
    public ConsecutiveItPairs (Iterable<E> input)
    {
        this.it = input.iterator();
        prev = it.next();
    }

    public boolean hasNext()
    {
        return prev != null && it.hasNext();
    }

    public Pair<E,E> next()
    {
        return new Pair<E, E>(
                prev,
                prev = it.next() );
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
//        list.add( 4 );
//        list.add( 5 );
        for ( Pair <Integer, Integer> t : new ConsecutiveItPairs<Integer>( list ))
        {
            System.out.println(t);
        }
    }

}

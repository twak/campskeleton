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
public class ConsecutivePairs <E> implements Iterator<Pair<E,E>>, Iterable<Pair<E,E>>
{
    List<E> input;
    int a = 0, size;
    boolean loop;

    /**
     *
     * @param input the list to returns triples from
     * @param loop is it cyclic? (do we return {end-1, end, start} etc...))
     */
    public ConsecutivePairs (List<E> input, boolean loop)
    {
        this.input = input;
        this.loop = loop;
        this.size = input.size();
    }

    public boolean hasNext()
    {
        if (size == 1 && !loop)
            return false;
        return a >= 0;
    }

    public Pair<E,E> next()
    {
        Pair<E, E> out = new Pair<E, E>(
                input.get( a % size ),
                input.get( ( a + 1 ) % size ) );

        a++;
        if (!loop && a == size - 1)
            a = -1; // end
        else if (loop && a == size )
            a = -1; // end

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
//        list.add( 4 );
//        list.add( 5 );
        for ( Pair <Integer, Integer> t : new ConsecutivePairs<Integer>( list, true ))
        {
            System.out.println(t);
        }
    }

}

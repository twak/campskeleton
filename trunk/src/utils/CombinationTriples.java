package utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Iterator that returns all tri-combinations of things in a list
 * @author twak
 */
public class CombinationTriples <E> implements Iterator<Triple<E,E,E>>, Iterable<Triple<E,E,E>>
{
    List<E> input;
    int a = 0,b = 1,c = 2, size;

    public CombinationTriples (Set<E> set)
    {
        this( new ArrayList<E>(set) );
    }

    public CombinationTriples (List<E> input)
    {
        this.input = input;
        this.size = input.size();
        assert(size >= 3);
    }

    public boolean hasNext()
    {
        return a >= 0;
    }

    public Triple<E,E,E> next()
    {
        Triple<E, E, E> out = new Triple<E, E, E>( input.get(a), input.get(b), input.get(c) );

        c++;
        if (c == size )
        {
            b++;
            if (b == size - 1)
            {
                a++;
                if (a == size - 2)
                {
                    a = -1; // end!
                }
                b = a+1;
            }
            c = b+1;
        }

        return out;
    }

    public void remove()
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Iterator<Triple<E, E, E>> iterator()
    {
        return this;
    }
    
//    public static void main (String[] args)
//    {
//        List<Integer> list = new ArrayList();
//        list.add( 1 );
//        list.add( 2 );
//        list.add( 3 );
//        list.add( 4 );
//        list.add( 5 );
//        for ( Triple <Integer, Integer, Integer> t : new AllTriples <Integer>( list ))
//        {
//            System.out.println(t);
//        }
//    }

}

package utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * CAUTION - half finished - mostly for interator use
 * @author twak
 */
public class ReverseList <T> implements List<T>
{
    List<T> l;
    public ReverseList( List<T> l )
    {
        this.l = l;
    }

    public int size()
    {
        return l.size();
    }

    public boolean isEmpty()
    {
        return l.isEmpty();
    }

    public boolean contains( Object o )
    {
        return l.contains( o );
    }

    public Iterator<T> iterator()
    {
        return new Iterator<T>()
        {
            int index = l.size()-1;
            public boolean hasNext()
            {
                return index >= 0;
            }

            public T next()
            {
                index--;
                return l.get( index+1 );
            }

            public void remove()
            {
                throw new UnsupportedOperationException( "Not supported yet." );
            }
        };
    }

    public static void main (String[] args)
    {
        List<Integer> is = new ArrayList();
        for (int i = 0; i < 10; i++)
            is.add( i);

        for (Integer i : new ReverseList<Integer>( is ))
            System.out.println(i);
    }

    public Object[] toArray()
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public <T> T[] toArray( T[] a )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public boolean add( T e )
    {
        int size = l.size();
        l.add( 0, e);
        return l.size() != size;
    }

    public boolean remove( Object o )
    {
        return l.remove( o );
    }

    public boolean containsAll( Collection<?> c )
    {
        return l.containsAll( c );
    }

    public boolean addAll( Collection<? extends T> c )
    {
        boolean out = false;
        for (T t : c)
        {
            add (t);
            out = true;
        }
        return out;
    }

    public boolean addAll( int index, Collection<? extends T> c )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public boolean removeAll( Collection<?> c )
    {
        return l.removeAll(c);
    }

    public boolean retainAll( Collection<?> c )
    {
        return l.retainAll(c);
    }

    public void clear()
    {
        l.clear();
    }

    public T get( int index )
    {
        return l.get( l.size() -1 - index );
    }

    public T set( int index, T element )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void add( int index, T element )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public T remove( int index )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public int indexOf( Object o )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public int lastIndexOf( Object o )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public ListIterator<T> listIterator()
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public ListIterator<T> listIterator( int index )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public List<T> subList( int fromIndex, int toIndex )
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}

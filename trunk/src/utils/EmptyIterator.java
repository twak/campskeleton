
package utils;

import java.util.Iterator;

/**
 *
 * @author twak
 */
public class EmptyIterator <E> implements Iterator<E>
{
    public boolean hasNext()
    {
        return false;
    }

    public E next()
    {
        return null;
    }

    public void remove()
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
    
}

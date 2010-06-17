
package utils;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An iterator that wraps a set to give an interator that
 * checks for item removal from the original set before returning it.
 * Note that this does not check for addition to the set (you will only get
 * a subset of the objects in the original set, nothing that is added
 * after this iterator is constructed)
 * 
 * @author twak
 */
public class CloneConfirmIterator <E> implements Iterator<E>, Iterable<E>
{
    Set original;
    Iterator<E> it;
    E next;
    
    public CloneConfirmIterator (Set<E> in)
    {
        original = in;
        it = new LinkedHashSet<E>( in ).iterator();
        findNext();
    }

    public boolean hasNext()
    {
        return next != null;
    }

    public E next()
    {
        E out = next;
        findNext();
        return out;
    }

    private void findNext()
    {
        do
        {
            if (!it.hasNext())
            {
                next = null;
                return;
            }
            next = it.next();
        }
        while ( !original.contains( next ) );
    }

    public void remove()
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Iterator<E> iterator()
    {
        return this;
    }
}

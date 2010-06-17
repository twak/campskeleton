
package utils;

import java.util.Iterator;

/**
 * Munge two interators to one...effect of remove() quite undefined.
 * @author twak
 */
public class ItComb <E> implements Iterable<E>, Iterator<E>
{
    Iterator<E> a, b;
    int stream = 0;
    
    public ItComb (Iterable<E> a, Iterable<E> b)
    {
        this.a = a.iterator();
        this.b = b.iterator();
    }
            
    public boolean hasNext()
    {
        return a.hasNext() || b.hasNext();
    }

    public E next()
    {
        if (a.hasNext())
        {
            stream = 1;
            return a.next();
        }
        if (b.hasNext())
        {
            stream = 2;
            return b.next();
        }
        return null;
    }

    public void remove()
    {
        if (stream == 1)
            a.remove();
        if (stream ==2 )
            b.remove();
        
        throw new Error();
    }

    public Iterator<E> iterator()
    {
        return this;
    }

}

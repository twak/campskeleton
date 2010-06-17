
package utils;

import java.util.Iterator;
import java.util.List;

/**
 * Given a list full of iterable things present an iterable object that 
 * presents those things.
 * 
 * Because I'm too lazy to learn a functional language properly
 * 
 * @author twak
 */
public class ItIt <E> implements Iterable<E>, Iterator<E>
{
    List<? extends Iterable<E>> list;
    
    Iterator <? extends Iterable<E>> it;
    Iterator <E> it2;
    Iterator <E> currentFrom;
    
    E next;
    
    public ItIt( List <? extends Iterable<E>> list )
    {
        this.list = list;
        it = list.iterator();
        findNext();
    }
    
    private void findNext()
    {
        if (it2 != null && it2.hasNext())
        {
            next = it2.next();
        }
        else if (it.hasNext())
        {
            it2 = it.next().iterator();
            findNext();
        }
        else
        {
            next = null;
        }
    }
    
    public Iterator<E> iterator()
    {
        return this;
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

    public void remove()
    {
        
    }

}

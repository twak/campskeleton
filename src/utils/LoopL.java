package utils;

/**
 *
 * @author twak
 */

import java.util.ArrayList;
import java.util.Iterator;

/**
 * a loop of loops, with an iterator for the contained primitive (corners!)
 * @author twak
 */
public class LoopL<E> extends ArrayList<Loop<E>>
{
    public LoopL(){}
    public LoopL( Loop<E> fromPoints )
    {
        this();
        add( fromPoints );
    }
    
    public Iterable<E> eIterator()
    {
        return new EIterable();
    }
    
    public class EIterable implements Iterable<E>
    {
        public Iterator<E> iterator()
        {
            return new ItIt( LoopL.this );
        }
    }

    public void addLoopL( LoopL<E> e )
    {
        addAll( e );
    }

    public int count()
    {
        int i = 0;
        
        for ( Loop<E> l : this )
            for ( E e : l)
                i++;
            
        return i;
    }

    public Iterable<LContext<E>> getCIterable()
    {
        return new Iterable<LContext<E>>()
        {
            public Iterator<LContext<E>> iterator()
            {
                return getCIterator();
            }

        };
    }

    public void reverseEachLoop()
    {
        for (Loop<E> loop : this)
            loop.reverse();
    }

    public Iterator<LContext<E>> getCIterator()
    {
        return new ContextIt();
    }

    public class ContextIt implements Iterator <LContext<E>>
    {
        Iterator<Loop<E>> loopIt  = null;
        Iterator<Loopable<E>> loopableIt = null;

        // next values to return
        Loopable<E> loopable;
        Loop<E> loop;

        public ContextIt()
        {
            loopIt = LoopL.this.iterator();
            findNext();
        }

        private void findNext()
        {
            if (loopIt == null)
            {
                return; // finished!
            }
            else if (loopableIt != null && // start
                    loopableIt.hasNext())
            {
                loopable = loopableIt.next();
            }
            else if (loopIt.hasNext())
            {
                loopableIt = (loop = loopIt.next()).loopableIterator().iterator();
                findNext();
            }
            else
            {
                loopIt = null;
            }
        }

        public boolean hasNext()
        {
            return loopIt != null;
        }

        public LContext next()
        {
            LContext out = new LContext( loopable, loop );
            findNext();
            return out;
        }

        public void remove()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }
    }
}


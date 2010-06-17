package utils;

import java.util.Iterator;

public class Loopable<E> implements Iterable<Loopable<E>>
{
    E me;
    Loopable<E> next, prev;
    
    public Loopable (E me)
    {
        this.me = me;
    }
    
    public E get()
    {
        return me;
    }
    
    public Loopable<E> getNext()
    {
        return next;
    }

    public Loopable<E> getPrev()
    {
        return prev;
    }
    
    public void setNext(Loopable<E> s)
    {
        next = s;
    }
    
    public void setPrev(Loopable<E> s)
    {
        prev = s;
    }

    public Iterator<Loopable<E>> iterator()
    {
        return new LoopableIterator(this);
    }

    public int count()
    {
        int count = 0;
        
        for ( Loopable<E> e : this )
            count++;
        
        return count;
    }

    public class LoopableIterator implements Iterator<Loopable<E>>
    {
        Loopable<E> s, n, start;

        public LoopableIterator( Loopable<E> start )
        {
            s = start;
            n = null;
        }
        public boolean hasNext()
        {
            if (s == null)
                return false;
            if (n == null)
                return true;
            return n != s;
        }

        public Loopable<E> next()
        {
            if (n == null)
                n = s;

            Loopable<E> out = n;
            n = n.getNext();
            return out;
        }

        public void remove()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }
    }

    public class LoopIterator implements Iterator<E>
    {
        public LoopableIterator lit;

        public LoopIterator (Loopable<E> start)
        {
            lit = new LoopableIterator(start);
        }

        public boolean hasNext()
        {
            return lit.hasNext();
        }

        public E next()
        {
            return lit.next().me;
        }

        public void remove()
        {
            lit.remove();
        }
    }

}

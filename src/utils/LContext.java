package utils;

/**
 * Loopl context - an item and it's location in a lopp of loops
 * @author twak
 * @param <E> the type of item
 */
public class LContext<E>
{

    public Loopable<E> loopable;
    public Loop<E> loop;
    public Object hook; // attachement for misc extensions

    public LContext( Loopable<E> loopable, Loop<E> loop )
    {
        this.loopable = loopable;
        this.loop = loop;
    }

    public E get()
    {
        return loopable.get();
    }
}

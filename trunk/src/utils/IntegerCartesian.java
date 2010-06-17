package utils;

/**
 *
 * @author twak
 */
public class IntegerCartesian
{
    public int x, y;

    public IntegerCartesian (int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final IntegerCartesian other = (IntegerCartesian) obj;
        if ( this.x != other.x )
            return false;
        if ( this.y != other.y )
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 83 * hash + this.x;
        hash = 83 * hash + this.y;
        return hash;
    }
}

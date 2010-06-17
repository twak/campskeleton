package utils;

import edu.wlu.cs.levy.CG.KDTree;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author twak
 */
public abstract class Cluster1D <D>
{
    KDTree kd = new KDTree( 1 );
    MultiMap<Double, D> vals = new MultiMap();

    public Cluster1D(Iterable<D> stuff)
    {
        for (D d : stuff)
        {
            try
            {
                double val = getVal( d );
                kd.insert( new double[]
                        {
                            val
                        }, d );
                vals.put( val, d );
            }
            catch ( Throwable ex )
            {
                ex.printStackTrace();
            }
        }
    }

    public Set<D> getStuffBetween( double min, double max )
    {
        Set<D> out = new HashSet();
        try
        {

            Object[] res = kd.range( new double[]
                    {
                        min
                    }, new double[]
                    {
                        max
                    } );
            for ( Object o : res )
            {
//                double[] val = (double[]) o;
                out.add( (D) o);
            }
        }
        catch ( Throwable ex )
        {
            ex.printStackTrace();
        }
        return out;
    }

    public abstract double getVal (D d);

    public Set<D> getNear( double val, double delta )
    {
        Set<D> out= new HashSet();
        try
        {
            Object[] found = kd.range( new double[]{val - delta}, new double[]{val + delta} );
            for ( Object o : found )
            {
                out.add((D)o);
            }
        }
        catch ( Throwable ex )
        {
            ex.printStackTrace();
        }
        return out;
    }
}

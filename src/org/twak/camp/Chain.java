package org.twak.camp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A Chain is a set of edges colliding at a point. Described by the first
 * corner of each face. 
 * 
 * @author twak
 */
public class Chain
{

    List<Corner> chain = new ArrayList();
    boolean loop;
    
    // marker for having degraided from a loop to a list
    private final static Chain DELOOP = new Chain ( new ArrayList(), false );

    public Chain( List<Corner> chain )
    {
        this (chain, chain.get( chain.size() - 1 ).nextC == chain.get( 0 ));  //chain.get( chain.size() - 1 ).nextC.nextL
    }
    
    public Chain( List<Corner> chain, boolean loop )
    {
        this.loop = loop;
        this.chain = chain;
    }

    /**
     * @param index first element of the split
     */
    private Chain split( int index )
    {
        // decompose a loop a the given index
        if ( loop )
        {
            List<Corner> nc = new ArrayList();
            nc.addAll( chain.subList( index, chain.size() ) );
            nc.addAll( chain.subList( 0, index ) );
            loop = false;
            chain = nc;
            return DELOOP;
        }
        // first element already split
        if ( index == 0 )
            return null;

        List<Corner> nc = new ArrayList();
        nc.addAll( chain.subList( 0, index ) );
        chain = chain.subList( index, chain.size() );
        return new Chain( nc );
    }

    interface Condition<E>
    {
        public boolean isTrue ();
    }

    /**
     *
     * @param liveEdges
     * @return list of new chains, in order, that should be appended to the
     * master list before this chain.
     */
    public List<Chain> removeCornersWithoutEdges( Set<Edge> liveEdges )
    {
        List<Chain> newChains = new ArrayList();

        for(;;)
        {
            for ( int i = 0; i < chain.size(); i++ )
            {
                Corner c = chain.get( i );
                if ( ! liveEdges.contains( c.nextL ) )
                {
                    Chain n = split( i );

                    chain.remove( 0 ); // removed the specified element

                    if ( n == DELOOP )
                    {
                        //restart, next time first element will split with no effect
                        newChains.addAll( removeCornersWithoutEdges( liveEdges ) );
                        return newChains;
                    }

                    if (n != null)
                        newChains.add( n );

                    i = -1; // process element 0 next time
                }
            }
            // iterated entire chain - done!
            break;
        }
        return newChains;
    }

    /**
     * @return a list of additional chains after split has been performed
     */
    public List<Chain> splitChainsIfHorizontal( Set<Corner> horizontals )
    {
        List<Chain> newChains = new ArrayList();
        
        for(;;)
        {
            for ( int i = 0; i < chain.size(); i++ )
            {
                Corner c = chain.get( i );
                if ( horizontals.contains( c ) )
                {
                    Chain n = split( i );

                    if ( n == DELOOP )
                    {
                        //restart, next time first element will split with no effect
                        newChains.addAll( splitChainsIfHorizontal( horizontals ) );
                        return newChains;
                    }

                    if (n != null)
                        newChains.add( n );

                    i = 0; // process element 1 next time
                }
            }
            // iterated entire chain - done!
            break;
        }
        return newChains;
    }

    @Override
    public String toString()
    {
        return chain.toString();
    }



//    public static void main (String[] args)
//    {
//        List<Corner> lc = new ArrayList();
//        for (int i = 0; i < 20; i++)
//            lc.add( new Corner( i,i,i ));
//
//        Chain ch = new Chain (lc);
//        ch.loop = true;
//
//        List<Chain> chains = new ArrayList();
//        chains.add(ch);
//
//        Set<Corner> horz = new HashSet();
//
//        horz.add( lc.get( 6));
//        horz.add( lc.get( 19));
//
//
//        chains.addAll( ch.splitChainsIfHorizontal( horz ) );
//
//        for (Chain chain : chains )
//        {
//            System.out.println("===");
//
//        for (Corner c : chain.chain )
//            System.out.println(c);
//        }
//    }
}

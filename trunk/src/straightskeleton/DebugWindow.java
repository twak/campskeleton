package straightskeleton;

import straightskeleton.ui.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.vecmath.Point2d;
import utils.Cache;
import utils.Loop;
import utils.LoopL;

/**
 *
 * @author twak
 */
public class DebugWindow
{
    static boolean showing = false;

//    public static void showIfNotShown (LoopL<Corner> corners)
//    {
//
//    }
//
//    public static void showIfNotShown (LoopL<Edge> edges)
//    {
//        if (!showing)
//        {
//            showing = true;
//            new DebugWindow( edges );
//        }
//    }

//    public static void showIfNotShown( Set<Edge> liveEdges )
//    {
//        LoopL<Edge> loopl = new LoopL();
//
//        addEdges (loopl, liveEdges);
//
//        showIfNotShown( loopl );
//    }
//
//    public static void showIfNotShown( Set<Edge> liveEdges, Set<Corner> liveCorners )
//    {
//        Map<Corner, Corner> map = new HashMap();
//        for ( Corner c : liveCorners )
//            map.put( c, c.nextC);
//
//        LoopL<Edge> loopl = new LoopL();
//
//        while (map.size() > 0)
//        {
//            Loop<Edge> loop = new Loop();
//            loopl.add( loop );
//            Corner current = map.keySet().iterator().next();
//            do
//            {
//                map.remove( current );
//                loop.append( new Edge (current, current.nextC, 3) );
//                current = map.get( current );
//            }
//            while ( current != null );
//        }
//
//        addEdges( loopl, liveEdges );
//
//        showIfNotShown( loopl );
//    }

    private static void addEdges( LoopL<Edge> loopl, Set<Edge> liveEdges )
    {
        Map<Corner, Edge> map = new HashMap();
        for (Edge e : liveEdges)
            map.put( e.start, e);


        while (map.size() > 0)
        {
            Loop<Edge> loop = new Loop();
            loopl.add( loop );
            Edge current = map.values().iterator().next();
            do
            {
                map.remove( current.start);
                loop.append( current );
                current = map.get(current.end);
            }
            while ( current != null );
        }
    }

//    public DebugWindow (LoopL<Edge> edges)
//    {
//        LoopL<Corner> = Corner.fromEdges(Corner);
//        debugWindow( edges );
//    }

//    public DebugWindow (List<Edge> edges)
//    {
//        LoopL<Edge> loopl = new Loop
//        debugWindow( edges );
//    }

    private void debugWindow( final LoopL<Corner> edges )
    {
        final LoopL<Corner> blah = edges;

        PointEditor pe = new PointEditor()
        {
            Map<Bar, Edge> barEdge = new HashMap();
            @Override
            protected void createInitial()
            {
                Loop<Bar> loop2 = new Loop();

                Cache<Corner, Point2d> cache = new Cache<Corner, Point2d>()
                {
                    @Override
                    public Point2d create( Corner i )
                    {
                        return new Point2d (i.x, i.y);
                    }  
                };

                for ( Loop<Corner> loop : blah )
                    for (Corner c : loop)
                    {
                        Bar b;
                        loop2.append( b = new Bar( cache.get (c),
                            cache.get (c.nextC) ) );
                        barEdge.put( b, c.nextL );
                    }

                edges.add( loop2 );
            }

            @Override
            public void paintPointEditor( Graphics2D g2 )
            {
                int i = 0;

                g2.setColor( Color.red );
                for ( Loop<Bar> loop : edges )
                    for ( Bar bar : loop )
                    {
                        Point2d p = new Point2d (bar.start);
                        p.add( bar.end );
                        p.scale (0.5);
                        g2.drawString( ""+barEdge.get( bar ).getAngle() , ma.toX( p.x ),ma.toY( p.y )+30);

                        g2.drawString( (i++)+"" , ma.toX( bar.start.x ),ma.toY( bar.start.y )+10);
                        g2.drawString( ""+bar.start,  ma.toX( bar.start.x ),ma.toY( bar.start.y )+70);
                        g2.drawLine(
                                ma.toX( bar.start.x ),
                                ma.toY( bar.start.y ),
                                ma.toX( bar.end.x ),
                                ma.toY( bar.end.y ) );
                    }

                g2.setColor( Color.orange );
                for ( Bar bar : edges.eIterator() )
                    drawPixel( g2, bar.start );

                LoopL<Edge> out = new LoopL();
            }
        };

        pe.setup();

        JFrame frame = new JFrame();
        frame.addWindowListener( new WindowAdapter() {

            @Override
            public void windowClosing( WindowEvent e )
            {
                showing = false;
            }
        });
        frame.setContentPane( pe );
        frame.setSize( 400, 400 );
        frame.setVisible( true );
    }
}

package straightskeleton.debug;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;

import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.Machine;
import straightskeleton.Output;
import straightskeleton.Output.Face;
import straightskeleton.Skeleton;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import straightskeleton.ui.PointEditor;
import utils.ConsecutivePairs;
import utils.LContext;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;
import utils.MUtils;
import utils.Pair;

/**
 *
 * @author twak
 */
public class WeightedPointEditor extends PointEditor
{
     public Map <Marker, Bar> markerBar = new LinkedHashMap();

    public void positionMarker (Marker m, Bar oldBar)
    {
        oldBar.markers.remove( m );
        oldBar.addMarker( m );

        ((WeightedMarker)m).val = MUtils.clamp( m.distance( oldBar.start)/oldBar.start.distance( oldBar.end ), 0.1, 0.9);

        markerBar.put( m, oldBar );
    }

     public static class WeightedMarker extends Marker
     {
         public double val;
         public WeightedMarker()
         {
             super (null);
         }
     }

    protected void createInitial()
    {
//        createCircularPoints( 5, 200, 200, 150);

        // cross shape:
        Loop<Bar> loop = new Loop();
        edges.add( loop );

        for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( Arrays.asList(
                new Point2d( 250, 100 ),
                new Point2d( 350, 100 ),
                new Point2d( 350, 250 ),
                new Point2d( 500, 250 ),
                new Point2d( 500, 350 ),
                new Point2d( 350, 350 ),
                new Point2d( 350, 500 ),
                new Point2d( 250, 500 ),
                new Point2d( 250, 350 ),
                new Point2d( 100, 350 ),
                new Point2d( 100, 250 ),
                new Point2d( 250, 250 ) ), true ) )
        {
            Bar b = new Bar( pair.first(), pair.second() );
            loop.append( b );
            setup(b);
        }
    }

     Point pressed = new Point();

    @Override
    public void movePoint( LContext<Bar> ctx, Point2d pt, javax.vecmath.Point2d location, MouseEvent evt )
    {
        if ( pt instanceof WeightedMarker )
        {
            if ( pressed == null ) // when we start moving
                pressed = evt.getPoint();

            WeightedMarker m = (WeightedMarker) pt;

            m.x = location.x; // coordinates to project
            m.y = location.y;

            positionMarker( m, m.bar );

        }
        else
        {
            pt.x = location.x;
            pt.y = location.y;


            for ( Bar b : ctx.loop )
                updateMarkers( b );
        }
        changed = true;
    }

    public void updateMarkers( Bar b )
    {
        for (Marker m_ : b.markers)
        {
            WeightedMarker m = (WeightedMarker)m_;
            Vector2d dir = new Vector2d (b.end);
            dir.sub(b.start);
            double length = dir.length();
            dir.scale( MUtils.clamp( m.val, 0.1, 0.9 ) );
            dir.add(b.start);
            m.set( dir );

            m.bar = b;
        }

    }


    boolean changed = true;

    @Override
    public void remove(LContext<Bar> ctx, Point2d dragged) {

        changed = true;

        super.remove(ctx, dragged);
    }


    public void addBetween( LContext<Bar> ctx, Point l )
    {
        changed = true;

        Point2d n = new Point2d( l.x, l.y );
        Bar b = new Bar( n, ctx.get().end );
        setup(b);
        Loopable<Bar> loopable = ctx.loop.addAfter( ctx.loopable, b );
        ctx.get().end = n;

        dragged = new LContext<Bar>( loopable, ctx.loop );
        dragged.hook = n;

        loopable.get().tags.addAll( ctx.get().tags );

        edgeAdded( dragged );

        for ( Bar bb : ctx.loop )
            bb.updateMarkers();
    }

    public void setup (Bar b)
    {
                    WeightedMarker wm = new WeightedMarker();
            wm.val = 0.8;
            b.addMarker( wm );
            updateMarkers( b );
    }


    boolean busy = false;
    Output output;
    public void paintPointEditor(Graphics2D g2)
    {
        // override me!

        g2.setColor( Color.black );
        g2.setStroke( new BasicStroke( 4f ));
        for (Loop<Bar> loop : edges)
        {
            for (Bar bar : loop)
                g2.drawLine(
                        ma.toX( bar.start.x ),
                        ma.toY( bar.start.y ),
                        ma.toX( bar.end.x ),
                        ma.toY( bar.end.y ) );
            g2.setColor( Color.orange );
        }

        for (Bar bar : edges.eIterator())
            drawPixel( g2, bar.start );

        


        /**
         * Start of skeleton code
         */

        // controls the gradient of the edge

        final LoopL <Edge> out = new LoopL();
        Loop<Edge> loop1 = new Loop();
        out.add(loop1);
        
//        Corner 
//        	c1 = new Corner ( 0,0), 
//        	c2 = new Corner (100,-100 ), 
//        	c3 = new Corner (100,0 );
//        
//        Machine directionMachine = new Machine ();
//        
//        loop1.append(new Edge ( c1,c2 ) );
//        loop1.append(new Edge ( c2, c3 ) );
//        loop1.append(new Edge ( c3, c1 ) );
//        
//        for (Edge e : loop1)
//        	e.machine = directionMachine;
//        
//        
//        Skeleton skel = new Skeleton (out, true);
//        skel.skeleton();
//        
//        for ( Face face : skel.output.faces.values() )
//        {
//        	System.out.println("face:");
//            for (Loop <Point3d> lp3 : face.points)
//            	for (Point3d pt : lp3)
//            		System.out.println(pt);
//        }
        
        for ( Loop<Bar> lb : edges )
        {
            Loop<Edge> loop = new Loop();
            out.add( loop );

            for ( Bar bar : lb )
            {
                double val = 1;
                if ( bar.markers.size() > 0 && bar.markers.get( 0 ) instanceof WeightedMarker )
                {
                    val = ((WeightedMarker)bar.markers.get(0)).val;
                }
                
                Machine machine = new Machine (val * Math.PI - Math.PI/2);
                
                // 3D representation of 2D ui input
                Edge e = new Edge(
                        new Point3d( bar.start.x, bar.start.y, 0 ),
                        new Point3d( bar.end.x, bar.end.y, 0 ),
                        Math.PI / 4 );

                e.machine = machine;

                loop.append( e );
            }

            // the points defining the start and end of a loop must be the same object
            for ( Loopable<Edge> le : loop.loopableIterator() )
                le.get().end = le.getNext().get().start;
        }

        if (!busy && changed) {
            busy = true;
            changed = false;

            new Thread() {
                @Override
                public void run() {
                    try {

                        DebugDevice.reset();
                        Skeleton skeleton = new Skeleton(out, true);
                        skeleton.skeleton();
                        WeightedPointEditor.this.output = skeleton.output;
                    } finally {
                        busy = false;
                        WeightedPointEditor.this.repaint(); // erk
                    }
                };
            }.start();
        }

        /**
         * End of skeleton code...well we just have to paint it:
         */

         paintMultiColourOutput( g2);

        for ( Bar b : edges.eIterator() )
            for ( Marker mark_ : b.markers )
            {
                WeightedMarker mark = (WeightedMarker) mark_;

                Color mc = Color.orange;
                g2.setColor( new Color( mc.getRed(), mc.getGreen(), mc.getBlue(), 190 ) );
                int r = 6;
                
                g2.fillOval( ma.toX( mark.x ) - r, ma.toY( mark.y ) - r, r * 2, r * 2 );
            }
    }

    public void paintMultiColourOutput( Graphics2D g2 )
    {

        if ( output != null && output.faces != null )
        {
            int faceCount = output.faces.size();
            g2.setStroke( new BasicStroke( 1 ) );

            int faceIndex = 0;

            // back facing edges
            for ( Face face : output.faces.values() )
            {
                faceIndex++;
                if ( face.edge.getAngle() < 0 )
                    paintFace( faceCount, faceIndex, face, g2 );
            }

            // forwards facing edges
            faceIndex = 0;
            for ( Face face : output.faces.values() )
            {
                faceIndex++;
                if ( face.edge.getAngle() >= 0 )
                    paintFace( faceCount, faceIndex, face, g2 );
            }

            // outline
            for ( Face face : output.faces.values() )
                for ( Loop<Point3d> loop : face.getLoopL() )
                {
                    Polygon pg = new Polygon();
                    for ( Point3d p : loop )
                        pg.addPoint( ma.toX( p.x ), ma.toY( p.y ) );

                    if ( pg.npoints > 2 )
                    {

                        g2.setColor( Color.black );
                        g2.drawPolygon( pg );
                    }
                }
        }
    }


    private void paintFace( int faceCount, int faceIndex, Face face, Graphics2D g2 )
    {
        LoopL<Point3d> loopl = face.getLoopL();
        int loopIndex = 0;

        /**
         * First loop is the outer. Most skeleton faces will only have this.
         * Second+ loops are the holes in the face (if you need this, you're
         * a long way down a rabbit hole)
         */
        for ( Loop<Point3d> loop : loopl )
        {
            Polygon pg = new Polygon();
            for ( Point3d p : loop )
                pg.addPoint( ma.toX( p.x ), ma.toY( p.y ) );

            if ( pg.npoints > 2 )
            {
                if ( loopIndex == 0 ) // outer loop
                    g2.setColor( new Color( Color.HSBtoRGB( (faceIndex / (float) faceCount), 0.5f, 1f ) ) ); // new Color ( (33 * faceIndex) % 255,200,150)
                else
                    g2.setColor( Color.white ); // hole..?

                g2.fillPolygon( pg );
            }
            loopIndex++;
        }
    }
}

package org.twak.camp.debug;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;

import org.twak.camp.*;
import org.twak.camp.Output.Face;
import org.twak.camp.offset.OffsetSkeleton;
import org.twak.camp.ui.Bar;
import org.twak.camp.ui.Marker;
import org.twak.camp.ui.PointEditor;
import org.twak.utils.*;

/**
 *
 * @author twak
 */
public class WeightedPointEditor extends PointEditor
{
//    public Map <Marker, Bar> markerBar = new LinkedHashMap();
    
    public Map<Bar, Double> weights = new HashMap<Bar, Double>();

    
    public void positionMarker (Marker m, Bar oldBar)
    {
        oldBar.mould.remove( m );
        oldBar.mould.create( m, null ); // <-- this needs fixing up? hasn't been run

        weights.put (oldBar, MUtils.clamp( m.distance( oldBar.start)/oldBar.start.distance( oldBar.end ), 0.1, 0.9) );
    }

    public WeightedPointEditor( BarSelected es )
    {
        super( es );
    }
        
    WeightedPointEditor()
    {
    }
    
    @Override
    protected void createInitial()
    {
        giggidyStar( 0 );
        
        // cross shape:
//        Loop<Bar> loop = new Loop();
//        edges.add( loop );
//
//        for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( Arrays.asList(
//                new Point2d( 250, 100 ),
//                new Point2d( 350, 100 ),
//                new Point2d( 350, 250 ),
//                new Point2d( 500, 250 ),
//                new Point2d( 500, 350 ),
//                new Point2d( 350, 350 ),
//                new Point2d( 350, 500 ),
//                new Point2d( 250, 500 ),
//                new Point2d( 250, 350 ),
//                new Point2d( 100, 350 ),
//                new Point2d( 100, 250 ),
//                new Point2d( 250, 250 ) ), true ) )
//        {
//            Bar b = new Bar( pair.first(), pair.second() );
//            loop.append( b );
//            setup(b, 0.7);
//        }
    }

     Point pressed = new Point();

    @Override
    public void movePoint( LContext<Bar> ctx, Point2d pt, javax.vecmath.Point2d location, MouseEvent evt )
    {
        if ( pt instanceof Marker )
        {
            if ( pressed == null ) // when we start moving
                pressed = evt.getPoint();

            Marker m = (Marker) pt;

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
        for (Marker m : b.mould.getAnchorsReadOnly( b.start, b.end ) )
        {
            Vector2d dir = new Vector2d (b.end);
            dir.sub(b.start);
            
            dir.scale( MUtils.clamp( weights.get (b), 0.1, 0.9 ) );
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
        setup(b, 0.7);
        Loopable<Bar> loopable = ctx.loop.addAfter( ctx.loopable, b );
        ctx.get().end = n;

        dragged = new LContext<Bar>( loopable, ctx.loop );
        dragged.hook = n;

        loopable.get().tags.addAll( ctx.get().tags );

        edgeAdded( dragged );

        for ( Bar bb : ctx.loop )
            refreshMarkersOn(bb);
    }

    public void setup( Bar b, double weight )
    {
        Marker wm = new Marker();
        wm.bar = b;
        weights.put( b, weight );
        
        Point2d loc = new Point2d (b.end);
        loc.sub( b.start );
        loc.scale( weight );
        loc.add(b.start);
        wm.set (loc);
        
        b.mould.create( wm, null );
        positionMarker( wm, b );
    }


    boolean busy = false;
    Output output;
    static int count = 0;
    
    public void paintPointEditor(Graphics2D g2)
    {
//        giggidyStar( count++ );
//        changed = true;

        g2.setColor( new Color (0,50,0 ) );
        g2.setStroke( new BasicStroke( 4f ));
        for (Loop<Bar> loop : edges)
        {
            for (Bar bar : loop)
                g2.drawLine(
                        ma.toX( bar.start.x ),
                        ma.toY( bar.start.y ),
                        ma.toX( bar.end.x ),
                        ma.toY( bar.end.y ) );
//            g2.setColor( Color.orange );
        }

        for (Bar bar : edges.eIterator())
            drawPixel( g2, bar.start );


        /**
         * Start of skeleton code
         */


        if (changed) 
        	recalculate();

        /**
         * End of skeleton code...well we just have to paint it:
         */

         paintMultiColourOutput( g2);

//         if (false)
//         for (Loop<Corner> ls : offset)
//             for (Loopable<Corner> c: ls.loopableIterator() )
//             {
//                 Point2d s = new Point2d (ma.toX (c.get().x), ma.toY ( c.get().y));
//                 Point2d e = new Point2d (ma.toX (c.getNext().get().x), ma.toY ( c.getNext().get().y));
//                 Line2D l = new Line2D.Double( s.x, s.y, e.x, e.y );
//                 g2.setColor( Color.white );
//                 g2.setStroke( new BasicStroke (3f) );
//                 g2.draw(l);
//             }
         
        for ( Bar b : edges.eIterator() )
            for ( Marker mark : b.mould.markersOn( b ) )       {
                g2.setColor( Color.orange );
                int r = 5;
                g2.fillOval( ma.toX( mark.x ) - r, ma.toY( mark.y ) - r, r * 2, r * 2 );
            }
    }

	private void recalculate( ) {
		final LoopL<Edge> out = new LoopL<>();
		
		for ( Loop<Bar> lb : edges )
        {
            Loop<Edge> loop = new Loop();
            out.add( loop );

            for ( Bar bar : lb )
            {
                double val = 1;
                List<Marker> lm = bar.mould.markersOn( bar );
                if ( lm.size() > 0 && lm.get( 0 ) instanceof Marker )
                {
                    val = weights.get (bar);
                }
                
                Machine machine = new Machine (val);// * Math.PI );// - Math.PI/2);
                
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
            LoopL<Corner> offset = null;
            
            try
            {
            	DebugDevice.reset();
            	Skeleton skeleton = new Skeleton( out, true );
            	skeleton.skeleton();
            	WeightedPointEditor.this.output = skeleton.output;
//            offset = OffsetSkeleton.shrink( out, count % 200 );
            }
            finally
            {
            	busy = false;
            	WeightedPointEditor.this.repaint(); // erk
            }
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

                        g2.setColor( new Color (0,50,0 ) );
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
                {
//                    Color c = faceIndex % 2 == 0 ?  
//                            new Color( Color.HSBtoRGB( 0.32f, 1f, 0.2f+(faceIndex / (float) faceCount * 0.4f) ) ) :
//                            new Color( Color.HSBtoRGB( 0.32f, 1f, 0.6f+(faceIndex / (float) faceCount * 0.4f) ) );
                    g2.setColor( Rainbow.getColour( faceIndex ) );
//                    g2.setColor( new Color( ( faceIndex * 255 / (float) faceCount), 0.5f, 1f ) ); // new Color ( (33 * faceIndex) % 255,200,150)
                }
                else
                    g2.setColor( Color.white ); // hole..?

                g2.fillPolygon( pg );
            }
            loopIndex++;
        }
    }
    
    
    
    void giggidyStar( int frame )
    {
        edges.clear();
        Loop<Bar> loop = new Loop();
        edges.add( loop );
        
        double[] factors = new double[] {0.5,0.2,0.12,0.7,0.58,0.48, 0.44, 0.23, 0.98, 0.12, 0.1};
        int points = 9;
        
        List<Point2d> vals = new ArrayList();
        
        for (int i = 0; i < points; i++)
        {
            double tau = Math.PI*2 * i /points;
            double param = frame /  (600 * factors[i] );
            double a = 
                    factors[factors.length - i -1] * 100, 
                    b = 2 * Math.PI * 50 / points;
            
            double yCen = 200 * Math.cos( tau ), xCen = 200 * Math.sin (tau);
            
            double x = xCen + a * Math.cos (param) + Math.cos (tau) - b * Math.sin (param) * Math.sin (tau);
            double y = yCen + a * Math.cos (param) + Math.sin (tau) + b * Math.sin (param) * Math.cos (tau);
            
            vals.add( new Point2d (x,y));
        }
        
        int c = 0;
        for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( new ReverseList<Point2d>  ( vals ), true ) )
        {
            Bar b = new Bar( pair.first(), pair.second() );
            loop.append( b );
            setup( b, 0.8 );//0.4 + 0.6 * (0.5 + 0.5 * Math.sin ( frame / (factors [c++]*500) ) ));
        }    
    }
    
    public void paintUnthreaded( Graphics2D g2 )
    {
        giggidyStar( count++ );
        changed = true;
        // override me!

        g2.setColor( new Color( 0, 50, 0 ) );
        g2.setStroke( new BasicStroke( 4f ) );
        for ( Loop<Bar> loop : edges )
        {
            for ( Bar bar : loop )
                g2.drawLine(
                        ma.toX( bar.start.x ),
                        ma.toY( bar.start.y ),
                        ma.toX( bar.end.x ),
                        ma.toY( bar.end.y ) );
//            g2.setColor( Color.orange );
        }

        for ( Bar bar : edges.eIterator() )
            drawPixel( g2, bar.start );

        final LoopL<Edge> out = new LoopL();


        /**
         * Start
         * of
         * skeleton
         * code
         */
        // controls the gradient of the edge
        for ( Loop<Bar> lb : edges )
        {
            Loop<Edge> loop = new Loop();
            out.add( loop );

            for ( Bar bar : lb )
            {
                double val = 1;
                List<Marker> lm = bar.mould.markersOn( bar );
                if ( lm.size() > 0 && lm.get( 0 ) instanceof Marker )
                {
                    val = weights.get( bar );
                }

                Machine machine = new Machine( val );// * Math.PI );// - Math.PI/2);

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

            LoopL<Corner> offset;
            
            try
            {

                DebugDevice.reset();
                Skeleton skeleton = new Skeleton( out, true );
                skeleton.skeleton();
                WeightedPointEditor.this.output = skeleton.output;
                offset = OffsetSkeleton.shrink( out, count % 200 );
            }
            finally
            {
                busy = false;
                WeightedPointEditor.this.repaint(); // erk
            }

            paintMultiColourOutput( g2 );
            
            for ( Loop<Corner> ls : offset )
                for ( Loopable<Corner> c : ls.loopableIterator() )
                {
                    Point2d s = new Point2d( ma.toX( c.get().x ), ma.toY( c.get().y ) );
                    Point2d e = new Point2d( ma.toX( c.getNext().get().x ), ma.toY( c.getNext().get().y ) );
                    Line2D l = new Line2D.Double( s.x, s.y, e.x, e.y );
                    g2.setColor( Color.white );
                    g2.setStroke( new BasicStroke( 3f ) );
                    g2.draw( l );
                }
        }
    }

    
    /*
    public static void main (String[] args)
    {
        WeightedPointEditor wp = new WeightedPointEditor();
        wp.setup();
        
        wp.ma.setZoom( 1 );
        wp.ma.center( new Point2d( -320, -256) );
//        wp.ma.center( new Point2d( -640, -512) );
        
        File dir = new File ("/media/ubuntu_disk/raw_stills_renders/opal3/");
        dir.mkdirs();
        
        for (int i = 0; i < 10E3; i++ )
        {
            BufferedImage bi = new BufferedImage( 1280, 1024, BufferedImage.TYPE_INT_ARGB );
            Graphics2D g = bi.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor( Color.white );
            g.fillRect (0,0, 1280, 1024);
            wp.paintUnthreaded( g );
            g.dispose();
            try
            {
                ImageIO.write( bi, "PNG", new File (dir, String.format ("%04d.png", i)));
            } catch ( IOException ex )
            {
                ex.printStackTrace();
            }
        }
    }*/
    
}


package straightskeleton.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple2d;
import straightskeleton.Edge;
import org.twak.utils.Cache;
import org.twak.utils.ConsecutivePairs;
import org.twak.utils.LContext;
import org.twak.utils.Line;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Loopable;
import org.twak.utils.Pair;
import org.twak.utils.PanMouseAdaptor;

/**
 *
 * @author twak
 */
public class PointEditor extends JComponent
{
    public LoopL<Bar> edges = new LoopL();
    public List<Point2d> handles = new ArrayList();

    public LContext<Bar> currentBar = null;

    public PanMouseAdaptor ma;

    boolean paintGrid = false;
    
    public class Handle extends Point
    {
        public Edge edge;
        public Handle ( Edge edge )
        {
            this.edge = edge;
        }
    }

    public PointEditor ()
    {
        setBackground( Color.white );
//        setToolTipText( "" );
    }
    
    public PointEditor( BarSelected es )
    {
        barSelected = es;
//        setToolTipText( "" );
    }
    
    protected void createInitial()
    {
//        createCircularPoints( 5, 200, 200, 150);

        // cross shape:
        Loop<Bar> loop = new Loop();
        edges.add( loop );

        for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( Arrays.asList(
            new Point2d (250,100),
            new Point2d (350,100),
            new Point2d (350,250),
            new Point2d (500, 250),
            new Point2d (500, 350),
            new Point2d (350, 350),
            new Point2d (350, 500),
            new Point2d (250, 500),
            new Point2d (250, 350),
            new Point2d (100, 350),
            new Point2d (100, 250),
            new Point2d (250,250)
                ), true ))
        {
            loop.append( new Bar( pair.first(), pair.second() ) );
        }

// F-shape
//        for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( Arrays.asList(
//            new Point2d (250,100),
//            new Point2d (350,100),
//            new Point2d (350,250),
//            new Point2d (500, 250),
//            new Point2d (500, 100),
//            new Point2d (600, 100),
//            new Point2d (600, 250),
//            new Point2d (700, 250),
//            new Point2d (700, 350),
//            new Point2d (100, 350),
//            new Point2d (100, 250),
//            new Point2d (250,250)
//                ), true ))
//        {
//            loop.append( new Bar( pair.first(), pair.second() ) );
//        }
    }

    private void viewCenter()
    {
        Point2d mean = new Point2d();
        int count = 0;

        for ( Loop<Bar> e2 : edges )
            for ( Bar e : e2 )
            {
                mean.x += e.start.x;
                mean.y += e.start.y;
                count++;
            } // ignores final point :P

        mean.scale( 1/(double)count );
        centerView( new Point( (int) mean.x, (int) mean.y ) );
    }


    public void centerView(Point o)
    {
        ma.center(new Point2d (o.x, o.y));
    }


    public void setup()
    {

        MouseAdapter ap = new EditorMouseAdapter(); // hard works happens in here

         ma = new PanMouseAdaptor( this ); // pan/scan convertor
         ma.button = MouseEvent.BUTTON3;

        createInitial();

        addMouseListener( ap );
        addMouseMotionListener( ap );
        addMouseWheelListener( ap );

        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
            }
        });

        viewCenter();
    }

    protected boolean allowRemove( LContext<Bar> ctx, Point2d corner )
    {
        return true;
    }

    protected boolean allowDrag (Loop<Bar> loop)
    {
        return false;
    }

    protected boolean allowDrag(Point2d dragged) {
        return true;
    }


    public void remove( LContext<Bar> ctx, Point2d dragged )
    {
        if (!allowRemove( ctx, dragged ))
            return;

        if ( handles.remove( dragged ) )
        {
            repaint();
            return;
        }

        // maybe the start of the loop if we're dealing with lines!
        Bar bar = ctx.get();
        Loopable<Bar> loopable = ctx.loopable;
        ctx.loop.remove( loopable );

        if ( bar.start == dragged )
        {
            if ( loopable.getPrev().get().end == dragged )
                loopable.getPrev().get().end = loopable.get().end;
        }
        else if ( bar.end == dragged )
        {
            if ( loopable.getNext().get().start == dragged )
                loopable.getNext().get().start = loopable.get().start;
        }
        else
            throw new Error( "something fishy going on here" );

        for ( Bar b : edges.eIterator() )
            refreshMarkersOn(b);

        repaint();
    }

    protected void edgeAdded(LContext<Bar> ctx)
    {
        // to be overridden
    }

    public void addBetween( LContext<Bar> ctx, Point l )
    {
        Point2d n = new Point2d( l.x, l.y );
        Loopable<Bar> loopable = ctx.loop.addAfter( ctx.loopable, new Bar( n, ctx.get().end ) );
        ctx.get().end = n;

        dragged = new LContext<Bar>( loopable, ctx.loop );
        dragged.hook = n;

        loopable.get().tags.addAll( ctx.get().tags );

        edgeAdded( dragged );

        for ( Bar b : ctx.loop )
            refreshMarkersOn(b);
    }

    /**
     * Someone is dragging inside-of-a-loop!
     * @param draggedLoop
     * @param offset
     */
    public void moveLoop( Loop<Bar> draggedLoop, Tuple2d offset )
    {
        if (draggedLoop == null)
            return; // released
        for ( Bar b : draggedLoop )
            b.start.add( offset );
        
        for ( Bar b : draggedLoop )
            refreshMarkersOn(b);
    }


    /**
     * Someone is dragging, but didn't start inside a loop...
     */
    public void painting( Point2d location, Point2d offset , MouseEvent evt)
    {
    }

    @Override
    public void paint( Graphics g )
    {
        Graphics2D g2 = (Graphics2D)g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor( getBackground() );
        g2.fillRect( 0,0,getWidth(), getHeight());

        AffineTransform at = g2.getTransform(), old = new AffineTransform(at);
        g2.setTransform( at );

        paintPointEditor( g2 );

        g2.setTransform( old );
        
        paintChildren( g );
    }


    public void paintPointEditor(Graphics2D g2) {
        // override me!

        g2.setColor(Color.red);
        for (Loop<Bar> loop : edges) {
            for (Bar bar : loop) {
                g2.drawLine(
                        ma.toX(bar.start.x),
                        ma.toY(bar.start.y),
                        ma.toX(bar.end.x),
                        ma.toY(bar.end.y));
            }
        }

        g2.setColor(Color.orange);
        for (Bar bar : edges.eIterator())
            drawPixel( g2, bar.start );
    }

    /**
     * Called when some once double clicks on something
     * @param dragged
     */
    public void showMenu( LContext<Bar> dragged, MouseEvent evt )
    {
        // override me
    }

    double gridSize = 1;

    public void setGridSize(double gridSize)
    {
        this.gridSize = gridSize;
        repaint();
    }

    private Point2d doSnap( LContext<Bar> ctx, Point2d pt, Point2d loc )
    {
        return new Point2d ( 
                Math.round ( loc.x / gridSize ) * gridSize,
                Math.round ( loc.y / gridSize ) * gridSize);
    }

    protected void drawGrid(Graphics2D g2)
    {
        if (gridSize <= 0)
            return;
        
        g2.setColor(Color.green);
        int xOff = ma.toX( Math.floor( ma.fromX(0) / gridSize ) *gridSize );
        int xCount = (int) (Math.ceil( ma.fromZoom(getWidth())/gridSize ));

        if (xCount > 300) // don't kill graphics system trying to draw the grid
            return;

        for (int x = 0; x < xCount; x++)
        {
            int xc = (int)(x*gridSize * ma.getZoom())+xOff;
            g2.drawLine(xc, 0, xc, getHeight());
        }
    

        int yOff = ma.toY( Math.floor( ma.fromY(0) / gridSize ) *gridSize );
        int yCount = (int) (Math.ceil( ma.fromZoom(getHeight())/gridSize ));

        for (int y = 0; y < yCount; y++)
        {
            int yc = (int)(y*gridSize * ma.getZoom())+yOff;
            g2.drawLine(0, yc, getWidth(), yc );
        }
    }

    /**
     *
     * @param loc is the location in world space.
     * @param inside did they click inside another bar-loop?
     */
    public void createSection (Point loc, boolean inside)
    {
        // override me! - event when user adds a line segment
//        createCircularPoints( 3, loc.x, loc.y, 30 );
    }

    public void movePoint(  LContext<Bar> ctx, Point2d pt, Point2d location,MouseEvent evt)
    {
        // override me! - request to move a point
        pt.x = location.x;
        pt.y = location.y;
    }

    public boolean doSnap()
    {
        return true;
    }

    public void releasePoint( Point2d pt, LContext<Bar> ctx, MouseEvent evt)
    {
        //override! we've stopped dragging a point, update something!
    }

    protected void drawPixel( Graphics g, double i, double j )
    {
        g.fillRect( ma.toX( i ) - 2, ma.toY( j ) - 2, 5, 5 );
    }

    protected void drawPixel( Graphics g, Point p )
    {
        drawPixel( g, p.x , p.y );
    }

    protected void drawPixel( Graphics g, Point3d p )
    {
        drawPixel( g, p.x, p.y );
    }

    protected void drawPixel( Graphics g, Point2d p)
    {
        drawPixel( g, p.x, p.y );
    }

    protected void drawLine (Graphics g, Line line )
    {
            g.drawLine( ma.toX( line.start.x ), ma.toY( line.start.y ), ma.toX( line.end.x), ma.toY( line.end.y ) );
    }

    protected void drawLine (Graphics g, double x, double y, double x2, double y2)
    {
        g.drawLine( ma.toX( x ), ma.toY( y ), ma.toX( x2 ), ma.toY( y2 ) );
    }

    protected void drawLine (Graphics g, Point2d start, Point2d end )
    {
            g.drawLine(
                ma.toX( start.x ),
                ma.toY( start.y ),
                ma.toX( end.x ),
                ma.toY( end.y ) );
    }

    protected void drawLine (Graphics g, Point3d start, Point3d end )
    {
            g.drawLine(
                ma.toX( start.x ),
                ma.toY( start.y ),
                ma.toX( end.x ),
                ma.toY( end.y ) );
    }

    protected void drawPoly (Graphics g, int[] xes, int[] yes)
    {
        g.fillPolygon( xes, yes, xes.length );
    }


    public void createCircularPoints( int count, int x, int y, int rad )
    {
        createCircularPoints( count, x, y, rad, false );
    }
    public void createCircularPoints( int count, int x, int y, int rad, boolean backwards )
    {
        double delta = Math.PI * 2 / count;
        if (backwards)
            delta = -delta;

        Loop<Bar> circularEdges = new Loop();

        Point2d prev = null;
        for (int i = 0; i < count; i++)
        {
            Point2d c= new Point2d (
                    x+(int)(Math.cos (i* delta )*rad),
                    y+(int)(Math.sin (i* delta )*rad)
                    );
            if ( prev != null )
            {
                Bar e = new Bar (prev, c);
                edgeAdded( new LContext<Bar>( circularEdges.append( e ), circularEdges ) );
            }
            prev = c;
        }
        
        Bar e = new Bar (prev, circularEdges.start.get().start);
        edgeAdded( new LContext<Bar>( circularEdges.append( e ), circularEdges ) );

        edges.add ( circularEdges );
    }


//    public Point toView( Point in )
//    {
//            Point loc;
//            viewI.transform( in, loc = new Point() );
//            return loc;
//    }


    /**
     * @param destination point we're testing against
     * @param max maximum distance - else reports null
     * @return
     */
    public LContext<Bar> getNearest( Point destination, double max )
    { return getNearest (new Point2d (destination.x, destination.y), max); }
    public LContext<Bar> getNearest( Point2d destination, double max )
    {
        Iterator<LContext<Bar>> it = edges.getCIterator();
        LContext<Bar> out = null;
        double best = max;
        
        while ( it.hasNext() )
        {
            LContext<Bar> ctx = it.next();

            double dist = ctx.get().distance( destination );

            if ( dist < best )
            {
                out = ctx;
                best = dist;
            }
        }
        
        return out;
    }


//    protected Point2d draggedHandle = null;
    protected LContext<Bar> dragged = null;

    Loop<Bar> draggedBarSet = null;
    Point2d dragStartPoint = new Point2d();

    private class EditorMouseAdapter extends MouseAdapter
    {
        @Override
        public void mousePressed( MouseEvent e )
        {
            if ( e.getButton() == ma.button )
                return;

            // this just lets us return, and always repaint
            mousePressed_( e );

            repaint();
        }

        public void mousePressed_( MouseEvent e )
        {
            dragged = null;
            draggedBarSet = null;

            Point loc = new Point( (int) ma.fromX( e.getPoint().x ), (int) ma.fromY( e.getPoint().y ) );

            Point2d ept = new Point2d( ma.fromX( e.getPoint().x ), ma.fromY( e.getPoint().y ) );

            for ( Point2d point : handles )
            {
                if (!allowDrag(point))
                    continue;
                
                double dist = ma.to( point ).distanceSq( e.getPoint() );
                if ( dist < 200 )
                {
                    if ( dragged != null )
                        if ( dist > ((Point2d) dragged.hook).distance( ept ) )
                            continue;
                    dragged = new LContext<Bar>( null, null );
                    dragged.hook = point;
                }
            }

            Iterator<LContext<Bar>> bit = edges.getCIterator();
            while ( bit.hasNext() )
            {
                LContext<Bar> ctx = bit.next();
                Bar b = ctx.get();

                List<Point2d> pts = new ArrayList( Arrays.asList( new Point2d[]
                        {
                            b.start, b.end
                        } ) );

                pts.addAll( b.mould.getAnchorsForEditing(b, b.start, b.end ) );

                for ( Point2d point : pts )
                {
                    if (!allowDrag(point))
                        continue;
                    // to screenspace!
                    double dist = ma.to( point ).distanceSq( e.getPoint() );
                    if ( dist < 100 )
                    {
                        if ( dragged != null )
                            if ( dist > ((Point2d) dragged.hook).distance( ept ) )
                                continue;
                        dragged = ctx;
                        dragged.hook = point;
                    }
                }
            }
            
            if ( dragged != null )
            {

                if ( (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0 )
                {
                    remove( dragged, (Point2d) dragged.hook );
                    dragged = null;
                }
                else if (e.getClickCount() == 2)
                {
                    showMenu(dragged, e);
                    return;
                }
                else
                {
                    repeat( ept, e );
                }

                return;
            }


            // no point selected, how about edges?
            LContext<Bar> selected = null;

            selected = getNearest( ept, ma.fromZoom( 10 ) );

            if ( selected != null )
            {
                if (e.getClickCount() == 2)
                {
                    showMenu(selected, e);
                    return;
                }
                // an edge has been selected - are we adding a point or selecting the edge
                else if ( (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) > 0 )
                {
                    addBetween( selected, loc );
                    return;
                }
                else if( (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0 )
                {
                    remove( selected, ept );
                }
                else
                    barSelected( selected );
            }

            dragStartPoint.set( ept );
            draggedBarSet = null;
            
            // no edge selected, how about entire loops
            for ( Loop<Bar> loop : edges )
                if ( allowDrag(loop) && contains( loop, ept ) )
                {
                    draggedBarSet = loop;
                    continue;
                }

            if ( draggedBarSet != null )
                return;


            if ( e.getButton() == MouseEvent.BUTTON1 && e.isControlDown() )
            {
                createSection( loc, contains( edges, ept ) );
                repaint();
                return;
            }

            if (e.getClickCount() == 2)
            {
                showMenu(null, e);
                return;
            }

            painting( ept, new Point2d( 0, 0 ), e ); // this is a new-ish call!
            
        }

        public void repeat( Point2d e, MouseEvent evt )
        {
            if ( dragged != null )
            {
//                Point2d loc = new Point2d(e.getX(), e.getY());
                if ( doSnap() && (evt.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0)
                {
                    e = doSnap( dragged, (Point2d) dragged.hook, e);
                }

                movePoint( dragged, (Point2d) dragged.hook, e, evt );
                repaint();
            }
        }

        @Override
        public void mouseDragged( MouseEvent e )
        {
            if (e.getButton() == ma.button)
                return;

            Point2d loc = ma.from( e.getPoint() );

            if (dragged == null)
            {
                Point2d l2 = new Point2d(loc);
                l2.sub( dragStartPoint );

                if (draggedBarSet != null)
                    moveLoop (draggedBarSet, l2 );
                else
                    painting (loc, l2, e);


                dragStartPoint.set( loc );
            }

            repeat( loc, e );
            repaint();
        }

        @Override
        public void mouseReleased( MouseEvent e )
        {
            painting(null,null, e);

            if (e.getButton() == ma.button)
                return;
            
            if (dragged != null)
                releasePoint ( (Point2d)dragged.hook, dragged, e );

            if (draggedBarSet != null)
                    moveLoop (null, null );
            draggedBarSet = null;

            dragged = null;
        }
    }
    
    public boolean isDragging()
    {
        return dragged != null;
    }

    protected void barSelected(LContext<Bar> selected)
    {
        barSelected.barSelected(currentBar = selected);
    }

    public interface BarSelected
    {
        public void barSelected(LContext<Bar> ctx);
    }
    
    public BarSelected barSelected = new BarSelected()
    {
        public void barSelected( LContext<Bar> ctx)
        {
            return;
        }
    };

    @Override
    public String getToolTipText( MouseEvent event )
    {
        return ( ma.fromX( event.getX() ) + "," + ma.fromY( event.getY() ) );
    }


    public boolean containsLoop (LoopL<Bar> loop, Point2d ept)
    {
        return contains (loop, ept);
    }

    /**
     * Line-crossing (winding) contains algorithm.
     */
    private static boolean contains( LoopL<Bar> loop, Point2d ept )
    {
        for ( Loop<Bar> bar : loop )
            if ( contains( bar, ept ) )
                return true;
        
        return false;
    }
    
    private static boolean contains( Loop<Bar> loop, Point2d ept )
    {
        boolean in = false;

        // ten thousand is the biggest number there is!
        Line l = new Line (ept, new Point2d(ept.x+ 10000, ept.y) );

        for (Bar b : loop)
        {
            Line bl = new Line( b.start, b.end );
            if (l.intersects( bl ) != null)
                in = !in;
        }
        return in;
    }


    // map contains the markers that each bar has contributed at the current time
    Cache<Bar, Set<Marker>> barMarkerCache = new Cache<Bar, Set<Marker>>()
    {
        @Override
        public Set<Marker> create( Bar i )
        {
            return new LinkedHashSet();
        }
    };
    
    public void refreshMarkersOn(Bar b)
    {
        Set<Marker> sm = new HashSet ( barMarkerCache.get( b ) );
        barMarkerCache.cache.remove(b);
        handles.removeAll(sm); // handles.size()
        sm.clear();
        sm.addAll( b.mould.getAnchorsForEditing(b, b.start, b.end ) );
        handles.addAll( sm );
        barMarkerCache.put(b, sm);
    }

    public void removeMarkersFromBar( Bar bar )
    {
        Set<Marker> sm = barMarkerCache.get( bar );
        handles.removeAll(sm);
        sm.clear();
        barMarkerCache.cache.remove( bar );
    }

    public boolean canExport() { return false; }
    public boolean canImport() { return false; }

    public void exportt(File f)
    {
        JOptionPane.showMessageDialog( this, "Cannot export from this editor", "Editor not configured for export", JOptionPane.ERROR_MESSAGE);
    }

    public void importt(File f)
    {
        JOptionPane.showMessageDialog( this, "Cannot import from this editor", "Editor not configured for import", JOptionPane.ERROR_MESSAGE);
    }
}

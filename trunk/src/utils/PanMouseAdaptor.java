package utils;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point2d;

public class PanMouseAdaptor extends MouseAdapter
{
    int zoomInt = 0;
    double cenX;
    double cenY;
    double zoom = 1;
    Integer startX;
    Integer startY;
    Component comp;
    public int button = MouseEvent.BUTTON2;

    public List<RangeListener> listeners = new ArrayList();


//    public int button = MouseEvent.BUTTON2;

    public PanMouseAdaptor( Component comp )
    {
        super();
        this.comp = comp;
        comp.addMouseListener( this );
        comp.addMouseWheelListener( this );
        comp.addMouseMotionListener( this );

//        SwingUtilities.invokeLater( new Runnable()
//        {
//
//            public void run()
//            {
//
//                cenX = PanMouseAdaptor.this.comp.getWidth() / 2;
//                cenY = PanMouseAdaptor.this.comp.getHeight() / 2;
//            }
//
//        });

        comp.addComponentListener( new ComponentAdapter()
        {

            @Override
            public void componentResized( ComponentEvent e )
            {
                PanMouseAdaptor.this.comp.repaint();
                fireListeners();
            }
        } );
    }

    @Override
    public void mousePressed( MouseEvent e )
    {
        if ( e.getButton() == button )
        {
            startX = e.getPoint().x;
            startY = e.getPoint().y;
        }
    }

    @Override
    public void mouseDragged( MouseEvent e )
    {
        if ( startX != null )
        {
            int endX = e.getPoint().x;
            int endY = e.getPoint().y;
            cenX -= (endX - startX)/ zoom;
            cenY -= (endY - startY)/zoom;
            startX = endX;
            startY = endY;
            comp.repaint();
            fireListeners();

//            System.out.println("cen x,y are "+cenX +", "+cenY);
        }
    }

    @Override
    public void mouseReleased( MouseEvent e )
    {
        startX = null;
    }

    @Override
    public void mouseWheelMoved( MouseWheelEvent e )
    {
        setZoom( -e.getWheelRotation() );
    }

    private void setZoom( int direction )
    {
        zoomInt += direction;
        zoomInt = MUtils.clamp( zoomInt, -10, 10 );
        zoom = Math.exp( zoomInt/2. );
        comp.repaint();
        fireListeners();

//        System.out.println("zoom is "+zoom+" .. "+zoomInt);
    }

    public double fromX( int val )
    {
        return ( val - comp.getWidth() / 2 )/zoom + cenX;
    }

    public double fromY( int val )
    {
        return ( val - comp.getHeight() / 2 )/zoom + cenY;
    }

    public double fromZoom( double width )
    {
        return width/zoom;
    }

    public int toX( double val )
    {
        return (int) (( val - cenX) * zoom  + comp.getWidth()/2 );
    }

    public int toY( double val )
    {
        return (int) (( val - cenY ) * zoom + comp.getHeight()/2 );
    }

    public int toZoom( double width )
    {
        return (int) ( width * zoom );
    }


    public Point to( Point2d end )
    {
        return new Point( toX( end.x ), toY( end.y ) );
    }

    public Point2d from( Point point )
    {
        return new Point2d( fromX( point.x ), fromY( point.y ) );
    }

    public void center( Point2d point2d )
    {
        cenX = point2d.x;
        cenY = point2d.y;
    }

    public Point2d getCenter()
    {
        return new Point2d(cenX, cenY);
    }

    public double getMaxRange()
    {
        return fromZoom( Math.max (  comp.getWidth(), comp.getHeight() ) );
    }
    
    public Point2d from (MouseEvent e)
    {
        return new Point2d( fromX( e.getX() ), fromY( e.getY() ) );
    }

    public Rectangle to (DRectangle r)
    {
        return new Rectangle ( toX( r.x ),toY( r.y ), toZoom( r.width), toZoom( r.height));
    }

    public boolean isDragging()
    {
        return startX != null;
    }

    public void addListener(RangeListener ra)
    {
        listeners.add(ra);
    }

    public static abstract class RangeListener
    {
        public abstract void changed(PanMouseAdaptor ma);
    }

    private void fireListeners()
    {
        for (RangeListener ra : listeners)
            ra.changed(this);
    }
}

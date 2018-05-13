/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.twak.camp.debug;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.twak.camp.Corner;
import org.twak.camp.Edge;
import org.twak.camp.Output;
import org.twak.camp.Output.Face;
import org.twak.camp.Output.SharedEdge;
import org.twak.camp.ui.Bar;
import org.twak.camp.ui.PointEditor;
import org.twak.utils.Cache;
import org.twak.utils.LContext;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.Loopable;
import org.twak.utils.ui.Colourz;
import org.twak.utils.ui.Rainbow;

/**
 *
 * @author twak
 */
public abstract class DebugPointEditor extends PointEditor 
{
    LoopL<Corner> corners = null;
    Output output;

    Map<Point2d, Corner> cornerMap = new HashMap();
    Map<Bar, Edge> edgeMap = new HashMap();

    Map<Corner, Color> highlights = new HashMap();

    Map<Edge, Color> highlightEdges = new HashMap();

    public DebugPointEditor (LoopL<Corner> c, Output output)
    {
        this.output = output;
        setupCorners (c);
        addMouseMotionListener( new HoverListener () );
    }

    @Override
    protected void createInitial() {

        edges.clear();
        cornerMap.clear();
        edgeMap.clear();


        Cache<Corner, Point2d> cache = new Cache<Corner, Point2d>() {

            @Override
            public Point2d create(Corner i) {
                Point2d out = new Point2d(i.x, i.y);
                cornerMap.put(out, i);
                return out;

            }
        };

        for (Loop<Corner> lc : corners)
        {
            Loop<Bar> loop = new Loop();
            edges.add(loop);
            for (Corner c : lc)
            {
                Bar b = new Bar (cache.get(c), cache.get(c.nextC));
                edgeMap.put(b, c.nextL);
                loop.append(b);
            }
        }
    }

    @Override
    public void paintPointEditor( Graphics2D g2 )
    {
        if ( output != null && output.faces != null )
        {
            g2.setColor(new Color(0, 100, 100));
            g2.setStroke( new BasicStroke( 4 ) );
//            for ( Face face : output.nonSkelFaces)
//            {
//                for (Point3d p1 : face.results.map.keySet())
//                {
//                    for (Point3d p2 : face.results.map.get(p1))
//                        drawLine(g2, p1, p2);
//                }
//            }



//            int i = 0;
            for ( Face face : output.faces.values() )
            {
//                i++;
                g2.setColor( new Color( 50,50,50) );
                g2.setStroke( new BasicStroke( 3 ) );
                for ( SharedEdge se : face.definingSE )
                {
                    Point3d s = se.getStart( face );
                    Point3d e = se.getEnd( face );
                    drawLine( g2, s, e );
                }

                g2.setColor( new Color( 100, 100, 100 ) );
                g2.setStroke( new BasicStroke( 1 ) );
//                double offset = i * 10;
                for ( Point3d p1 : face.results.map.keySet() )
                    for ( Point3d p2 : face.results.map.get( p1 ) )
                        drawLine( g2, p1, p2 );
//                        drawLine( g2, p1.x + offset, p1.y + offset, p2.x + offset, p2.y + offset );
            }
        }


        g2.setStroke( new BasicStroke( 5 ) );
        for (Edge e : highlightEdges.keySet())
        {
            g2.setColor(highlightEdges.get(e));
            drawLine(g2, e.start.x, e.start.y, e.end.x, e.end.y );
        }

            g2.setStroke( new BasicStroke( 1 ) );


//        for ( Loop<Bar> e2 : edges )
//        {
//            Polygon pg = new Polygon();
//            for ( Bar e : e2 )
//                pg.addPoint( ma.toX( e.start.x ), ma.toY( e.start.y ) );
//            g2.setColor( Color.white );
//            g2.fillPolygon( pg );
//        }

        g2.setColor( Colourz.transparent( Color.green, 140 ) );
        
        for ( Loop<Bar> e2 : edges )
            for ( Bar e : e2 )
            {
                g2.setStroke( new BasicStroke( currentBar == null ? 2f : currentBar.get() == e ? 4f : 2f ) );
                g2.drawLine( ma.toX( e.start.x ), ma.toY( e.start.y ), ma.toX( e.end.x ), ma.toY( e.end.y ) );
            }

        Map<Point3d, Integer> vCount = new HashMap();

        g2.setColor(Colourz.transparent(Color.red, 140));
        for (Loop<Corner> loop : corners)
            for (Loopable<Corner> lc : loop.loopableIterator())
            {
                Corner c1 = lc.get(), c2 = lc.getNext().get();
                drawLine(g2, c1.x, c1.y, c2.x, c2.y);

                for (Corner c : new Corner[]
                        {
                            c1
                        })
                {
                    Integer res = vCount.get(new Point3d(c));
                    if (res == null)
                        res = new Integer(0);
                    vCount.put(new Point3d(c), new Integer ( res+1));
                }
            }

        g2.setColor(Color.black);
        g2.setBackground(Color.orange);
        for (Point3d pt : vCount.keySet())
            g2.drawString(vCount.get(pt).toString(), ma.toX(pt.x)+5, ma.toY(pt.y)+5);


        g2.setColor( Color.green.darker().darker() );
        for ( Loop<Bar> e2 : edges )
            for ( Bar e : e2 )
                drawPixel( g2, e.start );


        for (Corner c : highlights.keySet())
        {
            g2.setColor(highlights.get(c));
            drawPixel(g2, new Point2d(c.x, c.y));
        }

    }

    private void setupCorners(LoopL<Corner> c) {
        this.corners = c;
        super.setup();
    }

    @Override
    public void movePoint(LContext<Bar> ctx, Point2d pt, Point2d location, MouseEvent evt) {
        return;
    }

    @Override
    protected boolean allowRemove(LContext<Bar> ctx, Point2d corner) {
        return false;
    }

    @Override
    public void addBetween(LContext<Bar> ctx, Point l) {
        return;
    }

    public abstract void hoverOver (Corner c);

    public abstract void hoverOver (Edge e);

    public abstract void hoverOver (Face f, Output output);

    public class HoverListener extends MouseAdapter
    {
        @Override
        public void mouseMoved(MouseEvent e) {
            highlightEdges.clear();
            double tol = ma.fromZoom( 10 );

            Point2d from = ma.from(e);
            LContext<Bar> bar = getNearest(from, tol);

            if (bar != null) {
                for (Point2d p : new Point2d[]{bar.get().start, bar.get().end}) {
                    if (p.distance(from) < tol) {
                        Corner corner = cornerMap.get(p);
                        highlightEdges.put(corner.nextL, Color.green.darker());
                        highlightEdges.put(corner.prevL, Color.red.darker());
                        hoverOver(corner);
                        return;
                    }
                }

                hoverOver(edgeMap.get(bar.get()));
            }
            else if (output != null)// bar == null
            {
                if (corners.isEmpty()) // debug - face - mode - engage
                    for (Face f : output.faces.values()) {
                        Corner s = f.edge.start;
                        Corner ee = f.edge.end;
                        Bar b = new Bar(new Point2d(s.x, s.y), new Point2d(ee.x, ee.y));
                        if (b.distance(from) < tol) {
                            hoverOver(f,output);
                        }
                    }

                hoverOver((Edge)null);
                return;
            }
        }
    }

    public void setHightLights(Map<Corner, Color> map)
    {
        this.highlights = map;
        repaint();
    }
}

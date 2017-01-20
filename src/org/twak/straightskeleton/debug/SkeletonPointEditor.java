/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.twak.straightskeleton.debug;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import javax.swing.JButton;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
//import org.apache.batik.dom.GenericDOMImplementation;
//import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.twak.straightskeleton.Edge;
import org.twak.straightskeleton.Machine;
import org.twak.straightskeleton.Output;
import org.twak.straightskeleton.Skeleton;
import org.twak.straightskeleton.Output.Face;
import org.twak.straightskeleton.ui.Bar;
import org.twak.straightskeleton.ui.PointEditor;
import org.twak.utils.LContext;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Loopable;

/**
 *
 * @author
 * twak
 */
public class SkeletonPointEditor extends PointEditor
{

    boolean changed = true;
    
    public SkeletonPointEditor()
    {
        super();
        setLayout( new BorderLayout() );
        JButton b = new JButton( "save" );
        add( b, BorderLayout.NORTH );
        b.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                Writer out = null;
                try
                {
//                    // Get a DOMImplementation.
//                    DOMImplementation domImpl =
//                            GenericDOMImplementation.getDOMImplementation();
//                    // Create an instance of org.w3c.dom.Document.
//                    String svgNS = "http://www.w3.org/2000/svg";
//                    Document document = domImpl.createDocument( svgNS, "svg", null );
//                    // Create an instance of the SVG Generator.
//                    SVGGraphics2D svgGenerator = new SVGGraphics2D( document );
//                    // Ask the test to render into the SVG Graphics2D implementation.
//                    SkeletonPointEditor.this.paintPointEditor( svgGenerator );
//                    // Finally, stream out SVG to the standard output using
//                    // UTF-8 encoding.
//                    boolean useCSS = true; // we want to use CSS style attributes
//                    out = new FileWriter( "skeleton.svg" );
//                    svgGenerator.stream( out, useCSS );
                } catch ( Throwable ex )
                {
                    ex.printStackTrace();
                }
                finally
                {
                    try
                    {
                        out.close();
                    } catch ( IOException ex )
                    {
                        ex.printStackTrace();
                    }
                }
                SkeletonPointEditor.this.paint( null );
            }
        } );
        
    }
    
    @Override
    public void movePoint( LContext<Bar> ctx, Point2d pt, Point2d location, MouseEvent evt )
    {
        changed = true;
        super.movePoint( ctx, pt, location, evt );
    }
    
    @Override
    public void remove( LContext<Bar> ctx, Point2d dragged )
    {
        
        changed = true;
        
        super.remove( ctx, dragged );
    }
    
    public void addBetween( LContext<Bar> ctx, Point l )
    {
        changed = true;
        super.addBetween( ctx, l );
    }
    boolean busy = false;
    Output output;

    public void paintPointEditor( Graphics2D g2 )
    {
        // override me!

        g2.setColor( Color.red );
        
        Polygon border = new Polygon();
        
        for ( Loop<Bar> loop : edges )
        {
            for ( Bar bar : loop )
                border.addPoint( ma.toX( bar.start.x ), ma.toY( bar.start.y ) );
//                g2.drawLine(
//                        ma.toX( bar.start.x ),
//                        ma.toY( bar.start.y ),
//                        ma.toX( bar.end.x ),
//                        ma.toY( bar.end.y ) );
            
            
        }
        
//         g2.setColor( Color.orange );
//        for ( Bar bar : edges.eIterator() )
//            drawPixel( g2, bar.start );
        
        final LoopL<Edge> out = new LoopL();


        /**
         * Start
         * of
         * skeleton
         * code
         */
        // controls the gradient of the edge
        Machine machine = new Machine( Math.PI / 4 );
        
        for ( Loop<Bar> lb : edges )
        {
            Loop<Edge> loop = new Loop();
            out.add( loop );
            
            for ( Bar bar : lb )
            {
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
        
        if ( !busy && changed )
        {
            busy = true;
            changed = false;
            
            new Thread()
            {

                @Override
                public void run()
                {
                    try
                    {
                        
                        DebugDevice.reset();
                        Skeleton skeleton = new Skeleton( out, true );
                        skeleton.skeleton();
                        SkeletonPointEditor.this.output = skeleton.output;
                    }
                    finally
                    {
                        busy = false;
                        SkeletonPointEditor.this.repaint(); // erk
                    }
                }
            ;
        }
        .start();
        }

        /**
         * End of skeleton code...well we just have to paint it:
         */

         paintMultiColourOutput( g2 );
         
         g2.setColor( new Color (0, 168, 40) );
         g2.draw( border );
    }
    
    public void paintMultiColourOutput( Graphics2D g2 )
    {
        
        if ( output != null && output.faces != null )
        {
            int faceCount = output.faces.size();
            g2.setStroke( new BasicStroke( 1 ) );
            
            int faceIndex = 0;
            
            for ( Face face : output.faces.values() )
            {
                LoopL<Point3d> loopl = face.getLoopL();
                int loopIndex = 0;
                faceIndex++;

                for ( Loop<Point3d> loop : loopl )
                {
                    Polygon pg = new Polygon();
                    for ( Point3d p : loop )
                        pg.addPoint( ma.toX( p.x ), ma.toY( p.y ) );
                    
                    if ( pg.npoints > 2 )
                    {
                        if ( loopIndex == 0 ) // outer loop
                            g2.setColor( new Color (168, 212, 126));//new Color( Color.HSBtoRGB( (faceIndex / (float) faceCount), 0.5f, 1f ) ) ); // new Color ( (33 * faceIndex) % 255,200,150)
                        else
                            g2.setColor( Color.white ); // hole..?

                        g2.fillPolygon( pg );
                        
                        g2.setColor( new Color( 33, 108, 229 ) );
                        g2.drawPolygon( pg );
                    }
                    loopIndex++;
                }
            }
        }
    }
}

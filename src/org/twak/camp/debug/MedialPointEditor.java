/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.twak.camp.debug;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.vecmath.Point2d;

import org.twak.camp.Output;
import org.twak.camp.ui.Bar;
import org.twak.camp.ui.PointEditor;
import org.twak.utils.*;
import org.twak.utils.collections.Loop;

public class MedialPointEditor extends PointEditor
{

    boolean changed = true;
    
    enum RenderOption {
        UI, HEIGHTFIELD, ALPHA
    }

    
    public MedialPointEditor()
    {
        super();
        setLayout( new BorderLayout() );
        JButton b = new JButton( "save" );
        add( b, BorderLayout.NORTH );
        b.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
            
                BufferedImage heightMap = new BufferedImage( getMaxDim(), getMaxDim(), BufferedImage.TYPE_INT_ARGB);
                MedialPointEditor.this.paintPointEditor( (Graphics2D) heightMap.getGraphics(), RenderOption.HEIGHTFIELD );

                BufferedImage alphaMap = new BufferedImage( getMaxDim(), getMaxDim(), BufferedImage.TYPE_INT_ARGB);
                MedialPointEditor.this.paintPointEditor( (Graphics2D) alphaMap.getGraphics(), RenderOption.ALPHA );
                 
                try
                {
                    ImageIO.write( heightMap, "png" , new File ("/media/ubuntu_disk/tmp/medialAxis/heightMap.png"));
                    ImageIO.write( alphaMap, "png" , new File ("/media/ubuntu_disk/tmp/medialAxis/alphaMap.png"));
                    
                } catch ( IOException ex )
                {
                    ex.printStackTrace();
                }
            }
        } );

    }
    
    int getMaxDim()
    {
        return Math.max (getWidth(), getHeight());
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

    public void painting( Point2d location, Point2d offset, MouseEvent evt )
    {
        repaint();
    }
    double lastMaxdist = 100;

    public void paintPointEditor( Graphics2D g2 ) {
        paintPointEditor( g2, RenderOption.UI );
    }
    public void paintPointEditor( Graphics2D g2, RenderOption drawBorder ) 
    {

        g2.setColor( Color.white );

        
        if (drawBorder == RenderOption.HEIGHTFIELD)
            g2.fillRect( 0,0, getWidth(), getHeight());
        
        Polygon border = new Polygon();

        for ( Loop<Bar> loop : edges )
            for ( Bar bar : loop )
                border.addPoint( ma.toX( bar.start.x ), ma.toY( bar.start.y ) );


        if (drawBorder != RenderOption.ALPHA) {
        if ( !isDragging() )
        {
            double max = lastMaxdist;
            lastMaxdist = 0;
            for ( int x = 0; x < getMaxDim(); x++ )
            {
//                System.out.println(" x is "+x);
                for ( int y = 0; y < getMaxDim(); y++ )
                {

                    double shortestDist = Float.MAX_VALUE;
                    Bar nearestBar = null;
                    int timeSeen = 0;

                    Point ptI = new Point( x, y );
                    if ( !border.contains( ptI ) )
                        continue;

                    Point2d pt = ma.from( ptI );


                    for ( Loop<Bar> loop : edges )
                        for ( Bar bar : loop )
                        {

                            double dist = bar.distance( pt );
                            if ( dist < shortestDist )
                            {
                                if ( shortestDist - dist < 2 )
                                {
                                    timeSeen++;
                                }
                                shortestDist = dist;
                                nearestBar = bar;
                            }
                        }
                    lastMaxdist = Math.max( lastMaxdist, shortestDist );

                    int v = 255 - (int) (shortestDist * 255. / max);
                    v = MUtils.clamp( v, 0, 255 );

                    g2.setColor( new Color( v, v, v ) );
                    g2.fillRect( x, y, 1, 1 );
                }

            }
        }
        }
        else
        {
            g2.setColor( new Color (128, 128, 128) );
            g2.fill( border );
        }
        
        g2.setColor( new Color( 0, 168, 40 ) );
        
        if (drawBorder == RenderOption.UI)
            g2.draw( border );
    }
}

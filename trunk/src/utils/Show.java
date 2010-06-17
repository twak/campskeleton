package utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;

/**
 *
 * @author twak
 */
public class Show extends JFrame
{
    BufferedImage bi;
    public Show (BufferedImage bi)
    {
        this.bi = bi;
        setSize (bi.getWidth(), bi.getHeight()+40);
        setVisible( true );
    }

    @Override
    public void paint( Graphics g )
    {
        super.paint( g );
        g.drawImage( bi, 0, 30, null );
    }
}

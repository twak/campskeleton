package utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


/**
 *
 * @author twak
 */
public class FrameDumper 
{
    static int val = 0;
    
    static BufferedImage buffer = null;
    
    static Graphics2D graphics;
    
    public static Graphics2D dumpFrame (int width, int height)
    {
        if (buffer != null) // dump out image from last time
        {
            try
            {
                ImageIO.write( buffer, "PNG", new File( String.format( "%04d.png", val ++) ) );
                graphics.dispose();
            } 
            catch ( IOException ex )
            {
                ex.printStackTrace();
            }
        }
        
        buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        graphics = buffer.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        graphics.setColor(Color.white);
        graphics.fillRect(0,0, width, height);
        
        return graphics;
    }
}

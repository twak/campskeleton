package utils;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author twak
 */
public class ImageU
{
//    static File rootFile = new File ("/home/twak/Documents/facades/fish/");
    public static File rootFile = new File( "/home/twak/Documents/facades/willard/"  );
    static BufferedImage defImage = new BufferedImage (10, 10, BufferedImage.TYPE_INT_ARGB );

    public static BufferedImage getFlipH (String name)
    {
        BufferedImage image = getImage( name );
        BufferedImage i2 = new BufferedImage (image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D)i2.getGraphics();
//        AffineTransform at =new AffineTransform();
//        at.scale( -1, 1 );
//        at.translate( image.getWidth(), 0 );
//        g.setTransform( at );
        g.drawImage( image, image.getWidth(), 0, -image.getWidth(), image.getHeight(),  null );
        return i2;
    }

    public static BufferedImage getImage (String name)
    {
        try
        {
            return ImageIO.read( new FileInputStream( new File( rootFile, name ) ) );
        }
        catch ( IOException ex )
        {
            ex.printStackTrace();
            return defImage;
        }
    }

    public static BufferedImage clip (BufferedImage image, int x, int y, int width, int height)
    {
        try
        {
            BufferedImage out = new BufferedImage( width, height, image.getType() );
            Graphics g = out.getGraphics();
            g.drawImage( image, -x, -y, null);
            g.dispose();

            return out;
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            return null;
        }
    }
}

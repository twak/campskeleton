package utils.ui;

import java.awt.Color;

/**
 * Very similar to Rainbow!
 * @author twak
 */
public class Colour//  import uk.british; extends Color;
{

    static float hue;
    static float sat;
    static float bri;
    static
    {
        reset();
    }
    
    public static Color sky = new Color( 180, 225, 246 );

    public static Color nextColor()
    {
        hue += 0.1 / Math.PI;
        return new Color( Color.HSBtoRGB( hue, sat, bri ) );
    }

    public static void reset()
    {
        hue = 0;
        sat = 0.7f;
        bri = 1f;
    }

    public static Color transparent( Color color, int i )
    {
        return new Color (color.getRed(), color.getGreen(), color.getBlue(), i);
    }
}

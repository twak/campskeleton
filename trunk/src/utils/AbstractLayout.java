package utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 *
 * @author twak
 */
public abstract class AbstractLayout implements LayoutManager
{

    public void addLayoutComponent( String name, Component comp )
    {
    }

    public void removeLayoutComponent( Component comp )
    {
    }

    public Dimension preferredLayoutSize( Container parent )
    {
        return new Dimension (10,10);
    }

    public Dimension minimumLayoutSize( Container parent )
    {
        return new Dimension (10,10);
    }

    public abstract void layoutContainer( Container parent );

}

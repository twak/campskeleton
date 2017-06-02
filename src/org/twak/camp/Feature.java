package org.twak.camp;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.twak.camp.ui.Marker;
import org.twak.camp.ui.Marker.Type;
import org.twak.utils.ui.Rainbow;

/**
 * "Tags" for output properties
 * 
 * @author twak
 */
public class Feature
{
    static Rainbow rainbow;
    
    public Color color;
    public String name;
    String colorName;

    public Feature (String name)
    {
        color = Rainbow.next( this.getClass() );
        colorName = Rainbow.lastAsString( this.getClass() );
        this.name = name;
    }

    public static Comparator<Feature> nameComparator = new Comparator<Feature> ()
    {
        public int compare( Feature o1, Feature o2 )
        {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name);
        }
    };

    @Override
    public String toString()
    {
        return name +"("+colorName+")";
    }

    /**
     * This menu is shown when someone clicks on the marker
     * @return
     */
    public JPopupMenu createMenu(final Marker m)
    {
        JPopupMenu popup = new JPopupMenu();
        JMenu menu = new JMenu( "type:" );
        popup.add( menu );

        ButtonGroup group = new ButtonGroup();

        for (Type t : Marker.Type.values())
        {
            final Type tFinal = t;
            final JRadioButtonMenuItem item = new JRadioButtonMenuItem( t.toString() );
            if (m.properties.get( Marker.TYPE ) == t)
            {
                item.setSelected( true );
            }

            item.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent arg0 )
                {

                   if (item.isSelected())
                       m.properties.put( Marker.TYPE, tFinal);
                }
            });

            menu.add( item );
            group.add( item );
        }

        
        return popup;
    }
}

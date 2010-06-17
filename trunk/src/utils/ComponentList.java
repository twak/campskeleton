
package utils;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author twak
 */
public class ComponentList extends JPanel
{
    ListComponent selected;
    
    public interface ListComponent
    {
        public void selected();
        public void deselected();
        // used to retain selection after rebuilding the list
        public Object getObject();
    }
    
    public ComponentList()
    {
        setLayout( new ListDownLayout () );
    }
    
    @Override
    public Component add( Component comp )
    {
        addListeners( (JComponent)comp );
        return super.add( comp );
    }

    @Override
    public void remove( Component comp )
    {
        removeListeners( (JComponent)comp );
        super.remove(comp );
    }
    
    void addListeners(JComponent c)
    {
        addListeners( c,c);
    }
    
    void addListeners(JComponent root, JComponent c)
    {
        c.addMouseListener( new ComponentMouseListener( root ) );
        
        for (Component j : c.getComponents())
            if (j instanceof JComponent)
                addListeners( root, (JComponent)j);
    }
    
    void removeListeners(JComponent c)
    {
        for (MouseListener ml : c.getMouseListeners())
            if (ml instanceof ComponentMouseListener)
            {
                c.removeMouseListener( ml );
            }
        
        for (Component j : c.getComponents())
            if (j instanceof JComponent)
                removeListeners( (JComponent)j);
    }
    
    public void select (Component c)
    {
        for (MouseListener ml : c.getMouseListeners())
            if (ml instanceof ComponentMouseListener)
                ((ComponentMouseListener)ml).doSelect();
    }
    
    public class ComponentMouseListener extends MouseAdapter
    {
        JComponent src;
        
        
        public ComponentMouseListener(JComponent src)
        {
            this.src = src;
        }
        
        @Override public void mousePressed( MouseEvent e )
        {
            doSelect();
        }
        
        public void doSelect()
        {
            if (! (src instanceof ListComponent))
                return;
            
            ListComponent cl = (ListComponent)src;
            
            if (selected != null)
            {
                selected.deselected();
            }
            
            selected = cl;
            selected.selected();
        }
    }
}

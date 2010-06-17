package utils.ui;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * Adds save an load via xStream to a given menu
 * @author twak
 */
public class SaveLoad
{
    Field targetField;
    Object targetObject;
    JMenu menu; // used for popups
    public File saveAs;
    JMenuItem saveMenuItem = new javax.swing.JMenuItem();

    public void addSaveLoadMenuItems( JMenu menu, String targetField, Object targetObject )
    {
        try
        {
            this.targetField = targetObject.getClass().getField( targetField );
        }
        catch ( Exception ex )
        {
            ex.printStackTrace();
        }

        this.targetObject = targetObject;
        this.menu = menu;


        try
        {
            if ( this.getClass().getMethod( "makeNew" ).getDeclaringClass() != SaveLoad.class )
            {
                JMenuItem newItem = new JMenuItem();
                newItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK ) );
                newItem.setText( "new" );
                newItem.addActionListener( new java.awt.event.ActionListener()
                {

                    public void actionPerformed( java.awt.event.ActionEvent evt )
                    {
                        saveAs = null;
                        saveMenuItem.setEnabled( false );
                        makeNew();
                    }
                } );
                menu.add( newItem );
            }
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }

        JMenuItem loadMenuItem = new javax.swing.JMenuItem();

        loadMenuItem.setText( "load" );
        loadMenuItem.addActionListener( new java.awt.event.ActionListener()
        {
            public void actionPerformed( java.awt.event.ActionEvent evt )
            {
                loadMenuItemActionPerformed( evt );
            }
        } );
        
        loadMenuItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK ) );
        menu.add( loadMenuItem );

        saveMenuItem.setText( "save" );
        saveMenuItem.addActionListener( new java.awt.event.ActionListener()
        {

            public void actionPerformed( java.awt.event.ActionEvent evt )
            {
                saveMenuItemActionPerformed( evt );
            }
        } );
        saveMenuItem.setEnabled( false );
        saveMenuItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK ) );

        menu.add( saveMenuItem );


        JMenuItem saveAsMenuItem = new JMenuItem();
        saveAsMenuItem.setText( "save as..." );
        saveAsMenuItem.addActionListener( new java.awt.event.ActionListener()
        {

            public void actionPerformed( java.awt.event.ActionEvent evt )
            {
                saveAsMenuItemActionPerformed( evt );
            }
        } );

        menu.add( saveAsMenuItem );
        saveAsMenuItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK ) );


        JMenuItem quitMenuItem = new javax.swing.JMenuItem();
        quitMenuItem.setText( "quit" );
        quitMenuItem.addActionListener( new java.awt.event.ActionListener()
        {

            public void actionPerformed( java.awt.event.ActionEvent evt )
            {
                quitMenuItemActionPerformed( evt );
            }
        } );
        quitMenuItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK ) );
        menu.add( quitMenuItem );
    }

    public void makeNew()
    {
        //override!
    }

    public void beforeSave()
    {
    }

    public void afterSave()
    {
    }

    public void afterLoad()
    {
    }

    /**
     * @return
     */
    private boolean allowQuit()
    {
        return true;
    }

    private void quitMenuItemActionPerformed( java.awt.event.ActionEvent evt )
    {
        if ( allowQuit() )
            System.exit( 0 );
    }

    private void loadMenuItemActionPerformed( java.awt.event.ActionEvent evt )
    {
        saveAs = null;
        FileInputStream fis = null;
        try
        {
            XStream xs = createXStream();
            JFileChooser jf = new JFileChooser();

            int res = jf.showOpenDialog( menu );

            if ( res == JFileChooser.APPROVE_OPTION )
            {
                fis = new FileInputStream( saveAs = jf.getSelectedFile() );

                targetField.set( targetObject, xs.fromXML( fis ) );
            }
        }
        catch ( Exception ex )
        {
            JOptionPane.showMessageDialog( menu, "error loading file :(" );
            ex.printStackTrace();
        } finally
        {
            try
            {
                if ( fis != null )
                    fis.close();
                if (saveAs != null)
                    saveMenuItem.setEnabled( true );
            }
            catch ( IOException ex )
            {
                ex.printStackTrace();
            }
            afterLoad();
        }
    }

    private void saveMenuItemActionPerformed( java.awt.event.ActionEvent evt )
    {
        beforeSave();
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( saveAs );
            createXStream().toXML( targetField.get( targetObject ), fos );
        }
        catch ( Exception ex )
        {
            JOptionPane.showMessageDialog( menu, "error saving file :(" );
            ex.printStackTrace();
        } finally
        {
            try
            {
                if ( fos != null )
                    fos.close();
            }
            catch ( IOException ex )
            {
                ex.printStackTrace();
            }
            afterSave();
        }
    }

    private void saveAsMenuItemActionPerformed( java.awt.event.ActionEvent evt )
    {
        FileOutputStream fos = null;
        try
        {
            XStream xs = createXStream();
            JFileChooser jf = new JFileChooser();
            int res = jf.showSaveDialog( menu );

            if ( res == JFileChooser.APPROVE_OPTION )
            {
                fos = new FileOutputStream( saveAs = jf.getSelectedFile() );
                xs.toXML( targetField.get( targetObject ), fos );
            }
        }
        catch ( Exception ex )
        {
            JOptionPane.showMessageDialog( menu, "error saving file :(" );
            ex.printStackTrace();
        } finally
        {
            try
            {
                if ( fos != null )
                    fos.close();
                
                saveMenuItem.setEnabled( true );
            }
            catch ( IOException ex )
            {
                ex.printStackTrace();
            }
            afterSave();
        }
    }

    /**
     * Dump the object to disk without interfacing with the ui.
     */
    public static void debugSave(String fileName, Object toSave) {
        FileOutputStream fos = null;
        try {
            XStream xs = createXStream();

            fos = new FileOutputStream(fileName);
            xs.toXML(toSave, fos);

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static XStream createXStream()
    {
        return new XStream();
    }
}

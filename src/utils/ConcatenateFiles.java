/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author twak
 */
public class ConcatenateFiles
{

    String preamble = "";

    public ConcatenateFiles() throws IOException
    {
        File license = new File( "license.txt" );

        BufferedReader fr = new BufferedReader( new FileReader( license ) );
        String s;
        while ( ( s = fr.readLine() ) != null )
        {
            preamble += s+"\n";
        }
        fr.close();

        File home = new File( "../SimpleMorph/src" );
//        File home = new File( "../lsys/src" );
        it( home );
    }

    private void it( File in ) throws IOException
    {
        List<File> f = new ArrayList();
        f.addAll( Arrays.asList( in.listFiles() ) );
        Collections.sort( f, new FileNameComparator() );

        for ( File e : f )
        {
            if ( e.isDirectory() )
            {
                it( e );
            } else
            {
                if ( e.getName().endsWith( ".java" ) )
                {
                    BufferedReader fr = null;
                    try
                    {
                        fr = new BufferedReader( new FileReader( e ) );
                        System.out.println(preamble);
                        
                        String s;
                        while ( (s = fr.readLine()) != null )
                        {
                            System.out.println(s);
                        }
                    } catch ( FileNotFoundException ex )
                    {
                        ex.printStackTrace();
                    } finally
                    {
                        fr.close();
                    }

                }
            }
        }
    }

    public static void main( String[] args ) throws Exception
    {
        new ConcatenateFiles();

    }
}

package utils;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author twak
 */
public class WebUtils
{

    public static boolean fileContains( File file, String string )
    {
        try
        {
            BufferedReader br = new BufferedReader( new FileReader( file ) );
            String s;
            while ( ( s = br.readLine() ) != null )
            {
                if ( s.contains( string ) )
                    return true;
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return false;
    }
    

    public static void showBrowser( String url )
    {
        try
            {
            
                // java 1.5....
                Class c = Class.forName( "java.awt.Desktop" );
                Object o = c.getMethod( "getDesktop").invoke( null );
                o.getClass().getMethod( "browse", URI.class ).invoke( o, new URI(url) );
                
                // vs 1.6....
                //Desktop.getDesktop().browse( new URI( url ) );
            }
            catch ( Exception ex )
            {
                ex.printStackTrace();
            }
            return;
    }
    
        // Copies src file to dst file.
    // If the dst file does not exist, it is created
    public static void copy(InputStream in, File dst)
    {
        try
        {
        OutputStream out = new FileOutputStream(dst);
    
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static void importResource(File destDir, String ... names)
    {
        for ( String resourceName : names )
        {
            try
            {
                InputStream is = WebUtils.class.getResourceAsStream( "/resources/" + resourceName );
                copy (is, new File( destDir, resourceName ));
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }
    
    public static void getImageFromUrl( String location, File dest )
    {
        try
        {
            for ( int i = 1; i < 31; i++ )
            {
                URL url = new URL( location );
                InputStream in = url.openStream();
                
                OutputStream out = new BufferedOutputStream( new FileOutputStream( dest ) );
                for ( int b; ( b = in.read() ) != -1;)
                {
                    out.write( b );
                }
                out.close();
                in.close();
            }

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
    
    public static String stripXML(String in)
    {
//        try
//        {
//            in = "<blah>" + in + "</blah>";
//            InputStream is = new ByteArrayInputStream(in.getBytes("UTF-8")); 
//            Document n = Munger.docBuilder.parse( is );
//            
////            ByteArrayOutputStream os = new ByteArrayOutputStream();
////            OutputFormat of = new OutputFormat();
////            of.setMethod( Method.TEXT );
////            XMLSerializer serializer = new XMLSerializer( of );
////            serializer.setOutputCharStream( new OutputStreamWriter( os ) );
////            serializer.serialize( n );            
////            return os.toString();
//            
//            return toString (new StringBuilder(""), n).toString();
//            
//        }
//        catch ( Exception ex )
//        {
//            ex.printStackTrace();
//        }
            return unescapeHTML( in.replaceAll( "\\<.*?\\>", "" ), 0);
    }
    
    public static StringBuilder toString (StringBuilder sb, Node node)
    {
        String t = node.getTextContent();
        if ( t != null )
            sb.append( t );
        
        NodeList nl = node.getChildNodes();
        for (int i = 0; i< nl.getLength(); i++)
        {
            Node n2 = nl.item( i );
//            toString (sb, n2);
        }
        return sb;
    }
    
    private static final SimpleDateFormat subtitleFormat = new SimpleDateFormat("HH:mm:ss.00");
    public static String subtitleFormat(double seconds)
    {
        return subtitleFormat.format( new Date((int)(seconds*1000)));
    }
   
  private static HashMap<String,String> htmlEntities;
  static {
    htmlEntities = new HashMap<String,String>();
    htmlEntities.put("&lt;","<")    ; htmlEntities.put("&gt;",">");
    htmlEntities.put("&amp;","&")   ; htmlEntities.put("&quot;","\"");
    htmlEntities.put("&agrave;","à"); htmlEntities.put("&Agrave;","À");
    htmlEntities.put("&acirc;","â") ; htmlEntities.put("&auml;","ä");
    htmlEntities.put("&Auml;","Ä")  ; htmlEntities.put("&Acirc;","Â");
    htmlEntities.put("&aring;","å") ; htmlEntities.put("&Aring;","Å");
    htmlEntities.put("&aelig;","æ") ; htmlEntities.put("&AElig;","Æ" );
    htmlEntities.put("&ccedil;","ç"); htmlEntities.put("&Ccedil;","Ç");
    htmlEntities.put("&eacute;","é"); htmlEntities.put("&Eacute;","É" );
    htmlEntities.put("&egrave;","è"); htmlEntities.put("&Egrave;","È");
    htmlEntities.put("&ecirc;","ê") ; htmlEntities.put("&Ecirc;","Ê");
    htmlEntities.put("&euml;","ë")  ; htmlEntities.put("&Euml;","Ë");
    htmlEntities.put("&iuml;","ï")  ; htmlEntities.put("&Iuml;","Ï");
    htmlEntities.put("&ocirc;","ô") ; htmlEntities.put("&Ocirc;","Ô");
    htmlEntities.put("&ouml;","ö")  ; htmlEntities.put("&Ouml;","Ö");
    htmlEntities.put("&oslash;","ø") ; htmlEntities.put("&Oslash;","Ø");
    htmlEntities.put("&szlig;","ß") ; htmlEntities.put("&ugrave;","ù");
    htmlEntities.put("&Ugrave;","Ù"); htmlEntities.put("&ucirc;","û");
    htmlEntities.put("&Ucirc;","Û") ; htmlEntities.put("&uuml;","ü");
    htmlEntities.put("&Uuml;","Ü")  ; htmlEntities.put("&nbsp;"," ");
    htmlEntities.put("&copy;","\u00a9");
    htmlEntities.put("&reg;","\u00ae");
    htmlEntities.put("&euro;","\u20a0");
  }

  public static final String unescapeHTML(String source, int start){
     int i,j;

     i = source.indexOf("&", start);
     if (i > -1) {
        j = source.indexOf(";" ,i);
        if (j > i) {
           String entityToLookFor = source.substring(i , j + 1);
           String value = (String)htmlEntities.get(entityToLookFor);
           if (value != null) {
             source = new StringBuffer().append(source.substring(0 , i))
                                   .append(value)
                                   .append(source.substring(j + 1))
                                   .toString();
             return unescapeHTML(source, i + 1); // recursive call
           }
         }
     }
     return source;
  }




}

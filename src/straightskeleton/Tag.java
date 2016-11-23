package straightskeleton;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import org.twak.utils.Rainbow;

/**
 * "Tags" for output properties of faces
 * 
 * @author twak
 */
public class Tag
{
    static Rainbow rainbow;
    public Color color;
    public String name;
    String colorName;

    public Tag()
    {
        this ("unnamed");
    }

    public Tag (String name)
    {
        color = Rainbow.next( Tag.class );
        colorName = Rainbow.lastAsString( Tag.class );
        this.name = name;
    }

    public static Comparator<Tag> nameComparator = new Comparator<Tag> ()
    {
        public int compare( Tag o1, Tag o2 )
        {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name);
        }
    };

    @Override
    public String toString()
    {
        return name +"("+colorName+")";
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

import java.io.File;
import java.util.Comparator;

/**
 *
 * @author twak
 */
class FileNameComparator implements Comparator<File>
{

    public FileNameComparator()
    {
    }

    public int compare( File o1, File o2 )
    {
        return o1.getName().compareTo(o2.getName());
    }

}

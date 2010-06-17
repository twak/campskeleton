package utils;

/**
 *
 * @author twak
 */
public class Arrayz {
    public static int max (int[] array)
    {
        int max = Integer.MIN_VALUE;
        int maxI = -1;
        for (int i = 0; i < array.length; i++)
            if (array[i] > max)
            {
                maxI = i;
                max = array[i];
            }

        return maxI;
    }
}

package utils;

/**
 * Adapted from ibm.com
 * 
 * @author tomkelly
 * 
 * @param <A>
 * @param <B>
 */
public class Pair<A, B>
{
	private A element1;

	private B element2;

    public Pair(){}

	public Pair(A element1, B element2)
	{
		this.element1 = element1;
		this.element2 = element2;
	}

	public A first()
	{
		return element1;
	}

	public B second()
	{
		return element2;
	}
	public String toString()
	{
		return "(" + element1 + "," + element2 + ")";
	}

	public void set1(A element1)
	{
		this.element1 = element1;
	}

	public void set2(B element2)
	{
		this.element2 = element2;
	}
}

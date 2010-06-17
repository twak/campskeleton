package utils;

/**
 * Adapted from ibm.com
 * 
 * @author tomkelly
 * 
 * @param <A>
 * @param <B>
 */
public class Triple<A, B, C>
{
	private A element1;

	private B element2;

	private C element3;

	public Triple(A element1, B element2, C element3)
	{
		this.element1 = element1;
		this.element2 = element2;
		this.element3 = element3;
	}

	public A first()
	{
		return element1;
	}

	public B second()
	{
		return element2;
	}

	public C third()
	{
		return element3;
	}

	public String toString()
	{
		return "(" + element1 + "," + element2 + "," + element3 + ")";
	}

	public void set1(A element1)
	{
		this.element1 = element1;
	}

	public void set2(B element2)
	{
		this.element2 = element2;
	}

	public void set3(C element3)
	{
		this.element3 = element3;
	}
}

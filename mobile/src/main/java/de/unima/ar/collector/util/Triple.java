package de.unima.ar.collector.util;


public class Triple<A extends Comparable<? super A>, B extends Comparable<? super B>, C> implements Comparable<Triple<A, B, C>>
{
    private A a;
    private B b;
    private C c;


    public Triple(A a, B b, C c)
    {
        this.a = a;
        this.b = b;
        this.c = c;
    }


    public Object[] getValue()
    {
        return new Object[]{ a, b, c };
    }


    public A getA()
    {
        return a;
    }


    public B getB()
    {
        return b;
    }


    public C getC()
    {
        return c;
    }


    @Override
    public int compareTo(Triple<A, B, C> another)
    {
        if(this.getA().compareTo(another.getA()) == 0) {
            return this.getB().compareTo(another.getB());
        }

        return this.getA().compareTo(another.getA());
    }
}

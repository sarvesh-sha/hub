/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.async.instrument;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;

public class SampleTestCode
{
    String m_test = "test";

    public static class ToInstrument
    {
        public static class Inner
        {
            public int value;
        }

        public void init()
        {
            System.out.println("test");
        }

        public int test(int i,
                        int j)
        {
            try
            {
                return i + j;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return -1;
            }
        }
    }

    public static class ToTestVariableInsertion
    {
        public int test(int i,
                        int j)
        {
            int count = 1;
            for (int k = i; k < j; k++)
                count = count + 1;

            return count;
        }
    }

    public static class SomeGenericType<T extends SampleTestCode, U> extends CompletableFuture<T> implements Callable<U>
    {
        public class AnotherLayer
        {
            public class SomeNestedType<V extends List<?>>
            {
                public T doSomething(V list)
                {
                    return null;
                }

                public T doSomething2(V list)
                {
                    return doSomething(list);
                }
            }

            public class InstanceOfNestedType<V extends SampleTestCode> extends SomeGenericType<V, Integer>.AnotherLayer.SomeNestedType<ArrayList<String>>
            {
            }

            public class InstanceOfNestedType2 extends SomeGenericType<SampleTestCode, String>.AnotherLayer.SomeNestedType<List<?>>
            {
            }
        }

        @Override
        public U call() throws
                        Exception
        {
            return null;
        }
    }

    public void methodWithManyLocalsAndLambda() throws
                                                Exception
    {
        int  diff1  = 1;
        int  diff2  = 2;
        int  diff3  = 3;
        int  diff4  = 3;
        int  diff5  = 5;
        int  diff6  = 6;
        int  diff7  = 7;
        int  diff8  = 8;
        int  diff9  = 9;
        int  diff10 = 10;
        long diff11 = 11;
        int  diff12 = 12;
        int  diff13 = 13;
        int  diff14 = 14;
        int  diff15 = 15;
        int  diff16 = 16;
        try
        {
            int  diff17 = 17;
            int  diff18 = 18;
            int  diff19 = 19;
            int  diff20 = 20;
            byte diff21 = 21;
            int  diff22 = 22;

            {
                // Leave for testing nested variable scopes.
                @SuppressWarnings("unused") int diff23 = 23;

                diff23 += diff22;
            }

            switch (diff2)
            {
                case 1:
                    m_test = "1";
                    break;

                case 2:
                    m_test = "2";
                    break;

                case 3:
                    m_test = "3";
                    break;

                case 4:
                    m_test = "4";
                    break;
            }

            switch (diff3)
            {
                case 1000:
                    m_test = "1";
                    break;

                case 2000:
                    m_test = "2";
                    break;

                case 3000:
                    m_test = "3";
                    break;

                case 4000:
                    m_test = "4";
                    break;
            }

            switch (m_test)
            {
                case "1":
                    m_test = "1b";
                    break;

                case "2":
                    m_test = "2b";
                    break;
            }

            List<String> list = Lists.newArrayList();
            list.sort((s1, s2) ->
                      {
                          return (int) (m_test.length() + s1.length() - s2.length() + diff1 + diff2 + diff3 + diff4 + diff5 + diff6 + diff7 + diff8 + diff9 + diff10 + diff11 + diff12 + diff13 + diff14 + diff15 + diff16 + diff17 + diff18 + diff19 + diff20 + diff21 + diff22);
                      });
        }
        catch (Exception e)
        {
            System.out.println("Test");
        }
        finally
        {
            System.out.println("Test2");
        }
    }

    //--//

    static class Instance
    {
        static class Sub extends Instance
        {
            public int m_i;
        }

        public Instance m_child;

        public Instance()
        {
        }

        public Instance(Instance child)
        {
            m_child = child;
        }
    }

    public void instantiationWithCast()
    {
        for (int i = 0; i < 12; i++)
        {
            Instance t;
            synchronized (this)
            {
                t = new Instance(new Instance(i < 4 ? new Instance.Sub() : new Instance(null)));
            }

            t.m_child = t;
        }
    }

    public void nestedExceptions()
    {
        try
        {
            @SuppressWarnings("unused") int i = 2;

            try
            {
                i += 1;

                try
                {
                    i += 2;

                    callToMethodThatThrows();
                }
                catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                }
                catch (RuntimeException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    i += 3;
                }
            }
            catch (NullPointerException e2)
            {
                throw e2;
            }
            finally
            {
                i += 4;
            }
        }
        catch (Exception e3)
        {
        }
    }

    private String callToMethodThatThrows() throws
                                            IllegalAccessException
    {
        return "something";
    }

    public float methodThatUsesFloats(int j)
    {
        float sum = 0;
        for (int i = 0; i < 10; i++)
        {
            sum += i;
        }

        j = (int) sum;
        j = (int) (long) sum;

        Float.valueOf("12.3"); // Result ignored on purpose, to test POP.

        sum += j;

        return sum;
    }

    public double methodThatUsesDoubles(int j)
    {
        double sum = 0;
        for (int i = 0; i < 10; i++)
        {
            sum += i;
        }

        j = (int) sum;
        j = (int) (long) sum;

        Double.valueOf("12.3"); // Result ignored on purpose, to test POP.

        sum += j;

        return sum;
    }

    //
    // For this method, Eclipse compile doesn't properly split the EX range. The Java compiler does the correct thing.
    // So this tests our code to reorganize exception ranges.
    //
    public String methodWithTryResourcesAndCatch() throws
                                                   Exception
    {
        try (FileInputStream file = new FileInputStream("test"))
        {
            System.out.println("Hello");

            return "foo";
        }
        catch (FileNotFoundException ex)
        {
            if (ex.getMessage() != null)
            {
                return null;
            }

            System.out.println(ex);
            throw ex;
        }
    }

    public void usingVariousArrays()
    {
        boolean[] booleanArray = new boolean[10];
        booleanArray[1] = true;
        booleanArray[2] = booleanArray[2];

        byte[] byteArray = new byte[10];
        byteArray[1] = 1;
        byteArray[2] = byteArray[2];

        char[] charArray = new char[10];
        charArray[1] = 'a';
        charArray[2] = charArray[2];

        short[] shortArray = new short[10];
        shortArray[1] = 23;
        shortArray[2] = shortArray[2];

        int[] intArray = new int[10];
        intArray[1] = 23;
        intArray[2] = intArray[2];

        long[] longArray = new long[10];
        longArray[1] = 23;
        longArray[2] = longArray[2];

        float[] floatArray = new float[10];
        floatArray[1] = 23;
        floatArray[2] = floatArray[2];

        double[] doubleArray = new double[10];
        doubleArray[1] = 23;
        doubleArray[2] = doubleArray[2];
    }
}

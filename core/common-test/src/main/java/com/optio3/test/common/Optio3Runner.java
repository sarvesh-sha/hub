/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.common;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.util.TimeUtils;
import org.junit.internal.runners.statements.Fail;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class Optio3Runner extends BlockJUnit4ClassRunner
{
    private Optio3Test m_objectUnderTest;

    public Optio3Runner(Class<?> klass) throws
                                        InitializationError
    {
        super(klass);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods()
    {
        List<FrameworkMethod> list = Lists.newArrayList(super.computeTestMethods());
        list.sort((m1, m2) ->
                  {
                      TestOrder o1 = m1.getAnnotation(TestOrder.class);
                      TestOrder o2 = m2.getAnnotation(TestOrder.class);

                      if (o1 == null && o2 == null)
                      {
                          return m1.getName()
                                   .compareTo(m2.getName());
                      }

                      if (o1 == null)
                      {
                          return 1;
                      }

                      if (o2 == null)
                      {
                          return -1;
                      }

                      return o1.value() - o2.value();
                  });

        return list;
    }

    @Override
    protected void runChild(FrameworkMethod method,
                            RunNotifier notifier)
    {
        System.out.println();
        System.out.println(">>>>################################################################################");
        System.out.printf("%s : Running test %s.%s...\n",
                          TimeUtils.DEFAULT_FORMATTER_MILLI.format(TimeUtils.now()),
                          method.getDeclaringClass()
                                .getSimpleName(),
                          method.getName());
        System.out.println();

        super.runChild(method, notifier);

        System.out.println();
        System.out.printf("%s : Completed test %s.%s\n",
                          TimeUtils.DEFAULT_FORMATTER_MILLI.format(TimeUtils.now()),
                          method.getDeclaringClass()
                                .getSimpleName(),
                          method.getName());
        System.out.println("<<<<################################################################################");
    }

    @Override
    protected Statement methodBlock(FrameworkMethod method)
    {
        Statement statement = super.methodBlock(method);

        AutoRetryOnFailure autoRetry = method.getAnnotation(AutoRetryOnFailure.class);
        if (autoRetry != null)
        {
            return new Statement()
            {

                @Override
                public void evaluate() throws
                                       Throwable
                {
                    int run = 0;
                    while (run < autoRetry.retries())
                    {
                        m_objectUnderTest.setRunCounter(run++);

                        try
                        {
                            statement.evaluate();
                            return;
                        }
                        catch (Throwable e)
                        {
                            System.out.println();
                            System.out.printf("Test %s.%s failed on run n.%d, retrying...\n",
                                              method.getDeclaringClass()
                                                    .getSimpleName(),
                                              method.getName(),
                                              run);
                            System.out.println();
                        }
                    }

                    //
                    // One last time, but without try/catch, so it will propagate the failure.
                    //
                    m_objectUnderTest.setRunCounter(run);
                    statement.evaluate();
                }
            };
        }

        return statement;
    }

    @Override
    protected Object createTest() throws
                                  Exception
    {
        try
        {
            m_objectUnderTest = (Optio3Test) super.createTest();
        }
        catch (InvocationTargetException e)
        {
            return new Fail(e);
        }

        return m_objectUnderTest;
    }
}

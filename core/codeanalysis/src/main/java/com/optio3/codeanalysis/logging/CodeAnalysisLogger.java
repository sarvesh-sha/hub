/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.logging;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

public abstract class CodeAnalysisLogger
{
    public enum Level
    {
        OFF,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
    }

    public class IndentResource implements AutoCloseable
    {
        private String m_prefixToRestore;

        IndentResource(String prefix)
        {
            m_prefixToRestore = prefix;
        }

        @Override
        public void close()
        {
            m_prefix = m_prefixToRestore;
        }
    }

    private String m_prefix = "";

    public IndentResource indent(String prefix)
    {
        IndentResource res = new IndentResource(m_prefix);

        m_prefix += prefix;

        return res;
    }

    public void error(String context,
                      String msg)
    {
        log(context, Level.ERROR, msg);
    }

    public void error(String context,
                      String fmt,
                      Object... args)
    {
        log(context, Level.ERROR, fmt, args);
    }

    //--//

    public void warn(String context,
                     String msg)
    {
        log(context, Level.WARN, msg);
    }

    public void warn(String context,
                     String fmt,
                     Object... args)
    {
        log(context, Level.WARN, fmt, args);
    }

    //--//

    public void info(String context,
                     String msg)
    {
        log(context, Level.INFO, msg);
    }

    public void info(String context,
                     String fmt,
                     Object... args)
    {
        log(context, Level.INFO, fmt, args);
    }

    //--//

    public void debug(String context,
                      String msg)
    {
        log(context, Level.DEBUG, msg);
    }

    public void debug(String context,
                      String fmt,
                      Object... args)
    {
        log(context, Level.DEBUG, fmt, args);
    }

    //--//

    public void trace(String context,
                      String msg)
    {
        log(context, Level.TRACE, msg);
    }

    public void trace(String context,
                      String fmt,
                      Object... args)
    {
        log(context, Level.TRACE, fmt, args);
    }

    //--//

    public final void log(String context,
                          Level level,
                          String msg)
    {
        if (!isEnabled(context, level))
        {
            return;
        }

        logInner(context, level, msg);
    }

    public final void log(String context,
                          Level level,
                          String fmt,
                          Object... args)
    {
        if (!isEnabled(context, level))
        {
            return;
        }

        logInner(context, level, String.format(fmt, args));
    }

    public abstract boolean isEnabled(String context,
                                      Level level);

    protected abstract void logInner(String context,
                                     Level level,
                                     String msg);

    //--//

    public final void log(String context,
                          Level level,
                          TraceClassVisitor visitor)
    {
        expandList("", visitor.p.text, (t) -> log(context, level, "%s", t));
        visitor.p.text.clear();
    }

    public final void log(String context,
                          Level level,
                          TraceMethodVisitor visitor)
    {
        expandList("", visitor.p.text, (t) -> log(context, level, "%s", t));
        visitor.p.text.clear();
    }

    private static void expandList(String prefix,
                                   final List<?> l,
                                   Consumer<String> callback)
    {
        for (Object o : l)
        {
            if (o instanceof List)
            {
                expandList(prefix + "  ", (List<?>) o, callback);
            }
            else
            {
                String[] parts = o.toString()
                                  .split("\n");
                for (String part : parts)
                {
                    if (part.length() == 0)
                    {
                        continue;
                    }

                    callback.accept(prefix + part);
                }
            }
        }
    }

    //--//

    public static final CodeAnalysisLogger nullLogger = new CodeAnalysisLogger()
    {
        @Override
        protected void logInner(String context,
                                Level level,
                                String msg)
        {
        }

        @Override
        public boolean isEnabled(String context,
                                 Level level)
        {
            return false;
        }
    };

    protected String logHelper(String context,
                               Level level,
                               String msg)
    {
        StringBuilder sb = new StringBuilder();

        if (context != null && context.length() > 0)
        {
            sb.append(context);
            sb.append(" ");
        }

        if (m_prefix != null && m_prefix.length() > 0)
        {
            sb.append(m_prefix);
            sb.append(" ");
        }

        sb.append(msg);

        return sb.toString();
    }

    public static final CodeAnalysisLogger consoleLogger = new CodeAnalysisLogger()
    {
        @Override
        protected void logInner(String context,
                                Level level,
                                String msg)
        {
            System.out.println(logHelper(context, level, msg));
        }

        @Override
        public boolean isEnabled(String context,
                                 Level level)
        {
            return level != Level.OFF;
        }
    };

    public static CodeAnalysisLogger createStringLogger(Level enabled,
                                                        List<String> output)
    {
        return new CodeAnalysisLogger()
        {
            @Override
            protected void logInner(String context,
                                    Level level,
                                    String msg)
            {
                output.add(logHelper(context, level, msg));
            }

            @Override
            public boolean isEnabled(String context,
                                     Level level)
            {
                return (level.ordinal() <= enabled.ordinal());
            }
        };
    }

    public static CodeAnalysisLogger createCallbackLogger(Level enabled,
                                                          Consumer<String> output)
    {
        return new CodeAnalysisLogger()
        {
            @Override
            protected void logInner(String context,
                                    Level level,
                                    String msg)
            {
                output.accept(logHelper(context, level, msg));
            }

            @Override
            public boolean isEnabled(String context,
                                     Level level)
            {
                return (level.ordinal() <= enabled.ordinal());
            }
        };
    }

    //--//

    public static TraceClassVisitor printClass(ClassNode cn,
                                               Printer printer,
                                               PrintStream target)
    {
        if (printer == null)
        {
            printer = new TextifierWithOrderedLabels(cn);
        }

        final TraceClassVisitor classVisitor = new TraceClassVisitor(null, printer, null);

        cn.accept(classVisitor);

        flushPrinter(printer, target);

        return classVisitor;
    }

    public static TraceMethodVisitor printMethod(MethodNode mn,
                                                 Printer printer,
                                                 PrintStream target)
    {
        if (printer == null)
        {
            printer = new TextifierWithOrderedLabels(mn);
        }

        final TraceMethodVisitor methodVisitor = new TraceMethodVisitor(null, printer);

        mn.accept(methodVisitor);

        flushPrinter(printer, target);

        return methodVisitor;
    }

    private static void flushPrinter(Printer printer,
                                     PrintStream target)
    {
        if (target != null)
        {
            final PrintWriter printWriter = new PrintWriter(target);
            printer.print(printWriter);
            printWriter.flush();
        }
    }
}


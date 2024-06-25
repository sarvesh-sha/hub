/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public abstract class GenericType extends SignatureVisitor
{
    static class Parser extends SignatureVisitor
    {
        private final Parser                m_outer;
        private       GenericMethodOrType   m_context;
        private       Consumer<GenericType> m_doneCallback;

        private GenericMethodOrType m_activeFormal;
        private FormalParameter     m_lastFormal;

        private MethodDescriptor m_activeMethod;

        private TypeDeclaration m_activeTypeDeclaration;

        private TypeReference m_activeClass;

        private GenericType m_result;

        public Parser(GenericMethodOrType context)
        {
            this(null, context, null);
        }

        private Parser(Parser outer,
                       Consumer<GenericType> doneCallback)
        {
            this(outer, null, doneCallback);
        }

        private Parser(Parser outer,
                       GenericMethodOrType context,
                       Consumer<GenericType> doneCallback)
        {
            super(Opcodes.ASM7);

            m_outer = outer;
            m_doneCallback = doneCallback;
            m_context = context;
        }

        //--//

        GenericType getResult()
        {
            if (m_result != null)
            {
                return m_result;
            }

            if (m_activeMethod != null)
            {
                return m_activeMethod;
            }

            if (m_activeClass != null)
            {
                return m_activeClass;
            }

            if (m_activeTypeDeclaration != null)
            {
                return m_activeTypeDeclaration;
            }

            return null;
        }

        private void setResult(GenericType t)
        {
            m_result = t;

            if (m_doneCallback != null)
            {
                m_doneCallback.accept(t);
            }
        }

        private GenericMethodOrType ensureFormal()
        {
            if (m_activeFormal == null)
            {
                m_activeFormal = new GenericMethodOrType();
            }

            return m_activeFormal;
        }

        private MethodDescriptor ensureMethod()
        {
            if (m_activeMethod == null)
            {
                m_activeMethod = new MethodDescriptor(m_context, m_activeFormal != null ? m_activeFormal.formalParameters : null);
                m_activeFormal = null;
            }

            return m_activeMethod;
        }

        private TypeDeclaration ensureTypeDeclaration()
        {
            if (m_activeTypeDeclaration == null)
            {
                m_activeTypeDeclaration = new TypeDeclaration(m_context, m_activeFormal != null ? m_activeFormal.formalParameters : null);
                m_activeFormal = null;
            }

            return m_activeTypeDeclaration;
        }

        private GenericType searchTypeVariable(String name)
        {
            GenericMethodOrType context = m_context;
            while (context != null)
            {
                for (FormalParameter fp : context.formalParameters)
                {
                    if (fp.name.equals(name))
                    {
                        return fp;
                    }
                }

                context = context.outerContext;
            }

            return m_outer != null ? m_outer.searchTypeVariable(name) : null;
        }

        //--//

        @Override
        public void visitFormalTypeParameter(String name)
        {
            m_lastFormal = ensureFormal().addFormalParameter(name);
        }

        @Override
        public SignatureVisitor visitClassBound()
        {
            final FormalParameter fp = m_lastFormal;

            return new Parser(this, (t) ->
            {
                fp.classBound = t;
            });
        }

        @Override
        public SignatureVisitor visitInterfaceBound()
        {
            final FormalParameter fp = m_lastFormal;

            return new Parser(this, (t) ->
            {
                fp.addInterfaceBound(t);
            });
        }

        //--//

        @Override
        public SignatureVisitor visitSuperclass()
        {
            final TypeDeclaration td = ensureTypeDeclaration();

            return new Parser(this, (t) ->
            {
                td.superclass = (GenericType.TypeReference) t;
            });
        }

        @Override
        public SignatureVisitor visitInterface()
        {
            final TypeDeclaration td = ensureTypeDeclaration();

            return new Parser(this, (t) ->
            {
                td.addInterface((GenericType.TypeReference) t);
            });
        }

        //--//

        @Override
        public SignatureVisitor visitParameterType()
        {
            final MethodDescriptor md = ensureMethod();

            return new Parser(this, (t) ->
            {
                md.addParameterType(t);
            });
        }

        @Override
        public SignatureVisitor visitReturnType()
        {
            final MethodDescriptor md = ensureMethod();

            return new Parser(this, (t) ->
            {
                md.returnType = t;
            });
        }

        @Override
        public SignatureVisitor visitExceptionType()
        {
            final MethodDescriptor md = ensureMethod();

            return new Parser(this, (t) ->
            {
                md.addExceptionType(t);
            });
        }

        //--//

        @Override
        public void visitBaseType(char descriptor)
        {
            Type type;

            switch (descriptor)
            {
                // @formatter:off
                case 'V': type = Type.VOID_TYPE   ; break;
                case 'Z': type = Type.BOOLEAN_TYPE; break;
                case 'C': type = Type.CHAR_TYPE   ; break;
                case 'B': type = Type.BYTE_TYPE   ; break;
                case 'S': type = Type.SHORT_TYPE  ; break;
                case 'I': type = Type.INT_TYPE    ; break;
                case 'F': type = Type.FLOAT_TYPE  ; break;
                case 'J': type = Type.LONG_TYPE   ; break;
                case 'D': type = Type.DOUBLE_TYPE ; break;
                // @formatter:on

                default:
                    return;
            }

            setResult(new PrimitiveType(type));
        }

        @Override
        public void visitTypeVariable(String name)
        {
            setResult(new TypeVariable(name, searchTypeVariable(name)));
        }

        @Override
        public SignatureVisitor visitArrayType()
        {
            return new Parser(this, (t) ->
            {
                setResult(new ArrayType(t));
            });
        }

        @Override
        public void visitClassType(String name)
        {
            m_activeClass = new TypeReference(name, null);
        }

        @Override
        public void visitInnerClassType(String name)
        {
            m_activeClass = new TypeReference(name, m_activeClass);
        }

        @Override
        public void visitTypeArgument()
        {
            m_activeClass.addTypeArgument(new TypeArgument('*'));
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard)
        {
            final TypeReference clz = m_activeClass;

            return new Parser(this, (t) ->
            {
                TypeArgument ta = new TypeArgument(wildcard);
                ta.bound = t;
                clz.addTypeArgument(ta);
            });
        }

        @Override
        public void visitEnd()
        {
            setResult(m_activeClass);
        }
    }

    //--//

    public static class FormalParameter extends GenericType
    {
        public final String name;

        public GenericType classBound;

        public List<GenericType> interfaceBounds = Collections.emptyList();

        public FormalParameter(String name)
        {
            this.name = name;
        }

        public void addInterfaceBound(GenericType t)
        {
            if (interfaceBounds.isEmpty())
            {
                interfaceBounds = Lists.newArrayList();
            }

            interfaceBounds.add(t);
        }

        //--//

        @Override
        protected boolean equalsInnerNoCastCheck(GenericType obj)
        {
            FormalParameter obj2 = (FormalParameter) obj;

            if (!name.equals(obj2.name))
            {
                return false;
            }

            if (!equals(classBound, obj2.classBound))
            {
                return false;
            }

            if (!equals(interfaceBounds, obj2.interfaceBounds))
            {
                return false;
            }

            return true;
        }

        @Override
        public boolean isGeneric()
        {
            return true;
        }

        @Override
        protected Type asRawTypeSlow()
        {
            GenericType bound = classBound;

            //
            // NOTE: If this formal parameter is bound to multiple interfaces, the raw type is the one for the first interface.
            //
            if (bound == null)
            {
                bound = CollectionUtils.firstElement(interfaceBounds);
            }

            return ensureRawType(bound);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(name);
            if (classBound != null)
            {
                sb.append(":");
                sb.append(classBound.toString());
            }
            for (GenericType itf : interfaceBounds)
            {
                sb.append(":");
                sb.append(itf.toString());
            }

            return sb.toString();
        }
    }

    public static class GenericMethodOrType extends GenericType
    {
        public GenericMethodOrType outerContext;

        public List<FormalParameter> formalParameters = Collections.emptyList();

        private Map<String, GenericType> m_signatures;
        private Map<String, GenericType> m_signaturesForTypes;

        GenericMethodOrType()
        {
        }

        protected GenericMethodOrType(GenericMethodOrType context,
                                      List<FormalParameter> formalParameters)
        {
            outerContext = context;

            if (formalParameters != null)
            {
                this.formalParameters = formalParameters;
            }
        }

        //--//

        public FormalParameter addFormalParameter(String name)
        {
            if (formalParameters.isEmpty())
            {
                formalParameters = Lists.newArrayList();
            }

            FormalParameter val = new FormalParameter(name);
            formalParameters.add(val);
            return val;
        }

        public GenericType parse(String signature)
        {
            if (m_signatures == null)
            {
                m_signatures = Maps.newHashMap();
            }

            return m_signatures.computeIfAbsent(signature, s -> GenericType.parse(s, this));
        }

        public GenericType parseAsType(String signature)
        {
            if (m_signaturesForTypes == null)
            {
                m_signaturesForTypes = Maps.newHashMap();
            }

            return m_signaturesForTypes.computeIfAbsent(signature, s -> GenericType.parseType(s, this));
        }

        //--//

        @Override
        protected boolean equalsInnerNoCastCheck(GenericType obj)
        {
            GenericMethodOrType obj2 = (GenericMethodOrType) obj;

            return equals(formalParameters, obj2.formalParameters);
        }

        @Override
        public boolean isGeneric()
        {
            throw new RuntimeException("INTERNAL ERROR: found standalone instance of GenericMethodOrType");
        }

        @Override
        protected Type asRawTypeSlow()
        {
            throw new RuntimeException("INTERNAL ERROR: found standalone instance of GenericMethodOrType");
        }

        //--//

        protected void toString(StringBuilder sb)
        {
            if (!formalParameters.isEmpty())
            {
                sb.append("<");
                for (FormalParameter p : formalParameters)
                {
                    sb.append(p.toString());
                }
                sb.append(">");
            }
        }
    }

    public static class MethodDescriptor extends GenericMethodOrType
    {
        public List<GenericType> parameterTypes = Collections.emptyList();
        public GenericType       returnType;

        public List<GenericType> exceptionTypes = Collections.emptyList();

        private Type[] m_rawParameterTypes;

        public MethodDescriptor()
        {
            super();
        }

        MethodDescriptor(GenericMethodOrType context,
                         List<FormalParameter> formalParameters)
        {
            super(context, formalParameters);
        }

        //--//

        public void addParameterType(GenericType t)
        {
            if (parameterTypes.isEmpty())
            {
                parameterTypes = Lists.newArrayList();
            }

            parameterTypes.add(t);
        }

        public void addExceptionType(GenericType t)
        {
            if (exceptionTypes.isEmpty())
            {
                exceptionTypes = Lists.newArrayList();
            }

            exceptionTypes.add(t);
        }

        //--//

        @Override
        protected boolean equalsInnerNoCastCheck(GenericType obj)
        {
            if (!super.equalsInnerNoCastCheck(obj))
            {
                return false;
            }

            MethodDescriptor obj2 = (MethodDescriptor) obj;

            if (equals(parameterTypes, obj2.parameterTypes))
            {
                return false;
            }

            if (equals(returnType, obj2.returnType))
            {
                return false;
            }

            if (equals(exceptionTypes, obj2.exceptionTypes))
            {
                return false;
            }

            return true;
        }

        public boolean returnsVoid()
        {
            return getRawReturnType().equals(Type.VOID_TYPE);
        }

        public Type getRawReturnType()
        {
            return returnType.asRawType();
        }

        public Type[] getRawParameterTypes()
        {
            if (m_rawParameterTypes == null)
            {
                Type[] res = new Type[parameterTypes.size()];

                for (int i = 0; i < res.length; i++)
                    res[i] = parameterTypes.get(i)
                                           .asRawType();

                m_rawParameterTypes = res;
            }

            return m_rawParameterTypes;
        }

        @Override
        public boolean isGeneric()
        {
            if (!formalParameters.isEmpty())
            {
                return true;
            }

            if (returnType.isGeneric())
            {
                return true;
            }

            for (GenericType t : parameterTypes)
            {
                if (t.isGeneric())
                {
                    return true;
                }
            }

            for (GenericType t : exceptionTypes)
            {
                if (t.isGeneric())
                {
                    return true;
                }
            }

            return false;
        }

        @Override
        protected Type asRawTypeSlow()
        {
            return Type.getMethodType(getRawReturnType(), getRawParameterTypes());
        }

        //--//

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            toString(sb);

            return sb.toString();
        }

        @Override
        protected void toString(StringBuilder sb)
        {
            super.toString(sb);

            sb.append("(");
            for (GenericType t : parameterTypes)
                sb.append(t.toString());
            sb.append(")");
            sb.append(returnType.toString());

            for (GenericType t : exceptionTypes)
                sb.append(t.toString());
        }
    }

    public static class TypeDeclaration extends GenericMethodOrType
    {
        public GenericType.TypeReference superclass;

        public List<GenericType.TypeReference> interfaces = Collections.emptyList();

        public TypeDeclaration()
        {
            super();
        }

        TypeDeclaration(GenericMethodOrType context,
                        List<FormalParameter> formalParameters)
        {
            super(context, formalParameters);
        }

        //--//

        public void addInterface(GenericType.TypeReference t)
        {
            if (interfaces.isEmpty())
            {
                interfaces = Lists.newArrayList();
            }

            interfaces.add(t);
        }

        //--//

        @Override
        protected boolean equalsInnerNoCastCheck(GenericType obj)
        {
            if (!super.equalsInnerNoCastCheck(obj))
            {
                return false;
            }

            TypeDeclaration obj2 = (TypeDeclaration) obj;

            if (equals(superclass, obj2.superclass))
            {
                return false;
            }

            if (equals(interfaces, obj2.interfaces))
            {
                return false;
            }

            return true;
        }

        @Override
        public boolean isGeneric()
        {
            if (!formalParameters.isEmpty())
            {
                return true;
            }

            if (superclass != null && superclass.isGeneric())
            {
                return true;
            }

            for (GenericType itf : interfaces)
            {
                if (itf.isGeneric())
                {
                    return true;
                }
            }

            return false;
        }

        @Override
        protected Type asRawTypeSlow()
        {
            return superclass.asRawType();
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            toString(sb);

            return sb.toString();
        }

        @Override
        protected void toString(StringBuilder sb)
        {
            super.toString(sb);

            sb.append(superclass.toString());
            for (GenericType t : interfaces)
                sb.append(t.toString());
        }
    }

    public static class TypeReference extends GenericType
    {
        public final String        name;
        public final TypeReference outerClass;

        public List<TypeArgument> typeArguments = Collections.emptyList();
        public TypeReference      innerClass;

        public TypeReference(String name,
                             TypeReference outerClass)
        {
            //
            // As long as the outer class doesn't have type arguments,
            // we need to collapse the hierarchy.
            //
            while (outerClass != null && outerClass.typeArguments.isEmpty())
            {
                TypeReference outerOuterClass = outerClass.outerClass;
                if (outerOuterClass != null)
                {
                    // This is a piece in the middle, don't collapse.
                    break;
                }

                name = outerClass.name + "$" + name;
                outerClass = outerOuterClass;
            }

            this.name = name;
            this.outerClass = outerClass;

            if (outerClass != null)
            {
                outerClass.innerClass = this;
            }
        }

        public void addTypeArgument(TypeArgument t)
        {
            if (typeArguments.isEmpty())
            {
                typeArguments = Lists.newArrayList();
            }

            typeArguments.add(t);
        }

        public GenericType getBoundArgument(int i)
        {
            return typeArguments.get(i)
                                .asBound();
        }

        public GenericType getBoundArgumentOrDefault(int i)
        {
            if (i < typeArguments.size())
            {
                return typeArguments.get(i)
                                    .asBound();
            }

            return ensureNotNull(null);
        }

        //--//

        @Override
        protected boolean equalsInnerNoCastCheck(GenericType obj)
        {
            TypeReference obj2 = (TypeReference) obj;

            if (!name.equals(obj2.name))
            {
                return false;
            }

            // Don't check innerClass, it would cause an infinite recursion.

            if (!equals(typeArguments, obj2.typeArguments))
            {
                return false;
            }

            if (!equals(outerClass, obj2.outerClass))
            {
                return false;
            }

            return true;
        }

        @Override
        public boolean isGeneric()
        {
            if (outerClass != null && outerClass.isGeneric())
            {
                return true;
            }

            if (!typeArguments.isEmpty())
            {
                return true;
            }

            return false;
        }

        @Override
        public Type asRawTypeSlow()
        {
            StringBuilder sb = new StringBuilder();

            //
            // Always print from the outer class.
            //
            TypeReference ptr = this;
            while (ptr.outerClass != null)
            {
                ptr = ptr.outerClass;
            }

            while (ptr != null)
            {
                if (ptr.outerClass != null)
                {
                    sb.append("$");
                }

                sb.append(ptr.name);

                ptr = ptr.innerClass;
            }

            return Type.getObjectType(sb.toString());
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            //
            // Always print from the outer class.
            //
            TypeReference ptr = this;
            while (ptr.outerClass != null)
            {
                ptr = ptr.outerClass;
            }

            while (ptr != null)
            {
                sb.append(ptr.outerClass != null ? "." : "L");

                sb.append(ptr.name);
                if (!ptr.typeArguments.isEmpty())
                {
                    sb.append("<");
                    for (TypeArgument t : ptr.typeArguments)
                        sb.append(t.toString());
                    sb.append(">");
                }

                ptr = ptr.innerClass;
            }

            sb.append(";");

            return sb.toString();
        }
    }

    public static class TypeArgument extends GenericType
    {
        public final char        wildcard;
        public       GenericType bound;

        public TypeArgument(char wildcard)
        {
            this.wildcard = wildcard;
        }

        public TypeArgument(GenericType bound)
        {
            this.wildcard = '=';
            this.bound = bound;
        }

        //--//

        public GenericType asBound()
        {
            return ensureNotNull(bound);
        }

        //--//

        @Override
        protected boolean equalsInnerNoCastCheck(GenericType obj)
        {
            TypeArgument obj2 = (TypeArgument) obj;

            if (wildcard != obj2.wildcard)
            {
                return false;
            }

            if (!equals(bound, obj2.bound))
            {
                return false;
            }

            return true;
        }

        @Override
        public boolean isGeneric()
        {
            return true;
        }

        @Override
        protected Type asRawTypeSlow()
        {
            return ensureRawType(bound);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            if (wildcard != '=')
            {
                sb.append(wildcard);
            }

            if (bound != null)
            {
                sb.append(bound.toString());
            }

            return sb.toString();
        }
    }

    public static class PrimitiveType extends GenericType
    {
        public final Type type;

        public PrimitiveType(Type type)
        {
            Preconditions.checkNotNull(type);

            this.type = type;
        }

        //--//

        @Override
        protected boolean equalsInnerNoCastCheck(GenericType obj)
        {
            PrimitiveType obj2 = (PrimitiveType) obj;

            if (!type.equals(obj2.type))
            {
                return false;
            }

            return true;
        }

        @Override
        public boolean isGeneric()
        {
            return false;
        }

        @Override
        public Type asRawTypeSlow()
        {
            return type;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(type.getDescriptor());

            return sb.toString();
        }
    }

    public static class TypeVariable extends GenericType
    {
        public final String      name;
        public final GenericType bound;

        public TypeVariable(String name,
                            GenericType bound)
        {
            Preconditions.checkNotNull(name);

            this.name = name;
            this.bound = bound;
        }

        //--//

        public GenericType asBound()
        {
            return ensureNotNull(bound);
        }

        //--//

        @Override
        protected boolean equalsInnerNoCastCheck(GenericType obj)
        {
            TypeVariable obj2 = (TypeVariable) obj;

            if (!name.equals(obj2.name))
            {
                return false;
            }

            if (!equals(bound, obj2.bound))
            {
                return false;
            }

            return true;
        }

        @Override
        public boolean isGeneric()
        {
            return true;
        }

        @Override
        protected Type asRawTypeSlow()
        {
            return ensureRawType(bound);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("T");
            sb.append(name);
            sb.append(";");

            return sb.toString();
        }
    }

    public static class ArrayType extends GenericType
    {
        public final GenericType elementType;

        public ArrayType(GenericType elementType)
        {
            this.elementType = elementType;
        }

        //--//

        @Override
        protected boolean equalsInnerNoCastCheck(GenericType obj)
        {
            ArrayType obj2 = (ArrayType) obj;

            if (!equals(elementType, obj2.elementType))
            {
                return false;
            }

            return true;
        }

        @Override
        public boolean isGeneric()
        {
            return elementType.isGeneric();
        }

        @Override
        protected Type asRawTypeSlow()
        {
            Type element = elementType.asRawType();

            return Type.getType("[" + element.getDescriptor());
        }

        //--//

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("[");
            sb.append(elementType.toString());

            return sb.toString();
        }
    }

    //--//

    static final GenericType.TypeReference s_objectType = new GenericType.TypeReference(TypeResolver.TypeForObject.getInternalName(), null);

    private Type m_rawType;

    public GenericType()
    {
        super(Opcodes.ASM7);
    }

    public static GenericType parse(String signature,
                                    GenericMethodOrType context)
    {
        Parser parser = new Parser(context);

        SignatureReader sr = new SignatureReader(signature);
        sr.accept(parser);

        return parser.getResult();
    }

    public static GenericType parseType(String signature,
                                        GenericMethodOrType context)
    {
        Parser parser = new Parser(context);

        SignatureReader sr = new SignatureReader(signature);
        sr.acceptType(parser);

        return parser.getResult();
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        Type t = Reflection.as(o, Type.class);
        if (t != null)
        {
            return equals(t);
        }

        GenericType gt = Reflection.as(o, GenericType.class);
        if (gt != null)
        {
            return equals(this, gt);
        }

        return false;
    }

    public boolean equals(Type type)
    {
        return asRawType().equals(type);
    }

    public static boolean equals(GenericType left,
                                 GenericType right)
    {
        if (left == right)
        {
            return true;
        }

        if (right == null || left == null)
        {
            return false;
        }

        if (left.getClass() != right.getClass())
        {
            return false;
        }

        return left.equalsInnerNoCastCheck(right);
    }

    public static <T extends GenericType> boolean equals(Collection<T> left,
                                                         Collection<T> right)
    {
        if (left == right)
        {
            return true;
        }

        if (right == null || left == null)
        {
            return false;
        }

        if (left.size() != right.size())
        {
            return false;
        }

        Iterator<T> leftIt  = left.iterator();
        Iterator<T> rightIt = right.iterator();
        while (leftIt.hasNext())
        {
            T leftVal  = leftIt.next();
            T rightVal = rightIt.next();

            if (!equals(leftVal, rightVal))
            {
                return false;
            }
        }

        return true;
    }

    protected abstract boolean equalsInnerNoCastCheck(GenericType obj);

    //--//

    public abstract boolean isGeneric();

    public final String asInternalName()
    {
        return asRawType().getInternalName();
    }

    public final Type asRawType()
    {
        if (m_rawType == null)
        {
            m_rawType = asRawTypeSlow();
        }

        return m_rawType;
    }

    protected abstract Type asRawTypeSlow();

    public boolean equalsRawTypes(GenericType obj)
    {
        if (obj == null)
        {
            return false;
        }

        return asRawType().equals(obj.asRawType());
    }

    //--//

    private static Type ensureRawType(GenericType gt)
    {
        return gt != null ? gt.asRawType() : TypeResolver.TypeForObject;
    }

    private static GenericType ensureNotNull(GenericType val)
    {
        return val != null ? val : s_objectType;
    }
}

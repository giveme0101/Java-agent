package com.github.giveme0101;

import com.github.giveme0101.util.Monitor;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author kevin xiajun94@FoxMail.com
 * @Description
 * @name AgentMain
 * @Date 2020/09/17 15:49
 *
 * TODO java.lang.VerifyError: (class: com/github/giveme0101/Test, method: add signature: (II)Ljava/lang/Integer;) Expecting to find object/array on stack
 */
public class AgentMain {

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new DefineTransformer(), true);
    }

    static class DefineTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

            className = className.replace("/", ".");

            if (StringUtils.contains(className, "com.github.giveme0101")){

                try {

                    ClassPool classPool = new ClassPool(true);
                    CtClass ctClass = classPool.get(className);
                    String ctClassName = ctClass.getName();

                    for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {

                        MethodInfo ctMethodMethodInfo = ctMethod.getMethodInfo();
                        String ctMethodName = ctMethodMethodInfo.getName();

                        CodeAttribute codeAttribute = ctMethodMethodInfo.getCodeAttribute();
                        LocalVariableAttribute localVariableAttribute = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
                        CtClass[] parameterTypes = ctMethod.getParameterTypes();
                        int parameterSize = parameterTypes.length;

                        boolean isStatic = (ctMethodMethodInfo.getAccessFlags() & AccessFlag.STATIC) != 0;  // 判断是否为静态方法
                        List<String> parameterNameList = new ArrayList<>(parameterSize);            // 入参名称
                        List<String> parameterTypeList = new ArrayList<>(parameterSize);            // 入参类型
                        List<String> parameterList = new ArrayList<>(parameterSize);                // 参数组装；$1、$2...，$$可以获取全部，但是不能放到数组初始化

                        // 非static方法第0个参数是'this'
                        for (int i = isStatic ? 0 : 1; i < parameterTypes.length; i++) {
                            parameterNameList.add(localVariableAttribute.variableName(i));
                            parameterTypeList.add(parameterTypes[i].getName());
                            parameterList.add("$" + (i + 1));
                        }

                        // 方法：出参信息
                        CtClass returnType = ctMethod.getReturnType();
                        String returnTypeName = returnType.getName();

                        // 方法：生成方法唯一标识ID
                        int methodId = Monitor.generateMethodId(ctClassName, ctMethodName, parameterNameList, parameterTypeList, returnTypeName);

                        // 方法前加强
                        ctMethod.addLocalVariable("_startNano", CtClass.longType);
                        ctMethod.insertBefore("{ _startNano = System.nanoTime(); }");

                        // 方法前加强
                        ctMethod.addLocalVariable("_startNano", CtClass.longType);
                        ctMethod.insertBefore("{ _startNano = System.nanoTime(); }");

                        ctMethod.addLocalVariable("_parameterValues", classPool.get(Object[].class.getName()));
                        ctMethod.insertBefore("{ _parameterValues = new Object[]{" + StringUtils.join(parameterList, ",") + "}; }");

                        // 方法后加强 如果返回类型非对象类型，$_ 需要进行类型转换
                        ctMethod.insertAfter("{ com.github.giveme0101.util.Monitor.point(" + methodId + ", _startNano, _parameterValues, $_);}", false);

                        // 方法；添加TryCatch 添加异常捕获
                        ctMethod.addCatch("{ com.github.giveme0101.util.Monitor.point(" + methodId + ", $e); throw $e; }", ClassPool.getDefault().get("java.lang.Exception"));

                    }

                    return ctClass.toBytecode();
                } catch (Throwable ex){
                    ex.printStackTrace();
                }
            }

            return classfileBuffer;
        }
    }

}

package com.github.giveme0101;

import com.github.giveme0101.util.Monitor;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author kevin xiajun94@FoxMail.com
 * @Description
 * @name TestAgent
 * @Date 2020/09/18 17:39
 */
public class TestAgent extends ClassLoader {

    public static void main(String[] args) {

        try {
            // 创建新的 ClassPool，避免内存溢出
            ClassPool classPool = new ClassPool(true);

            // 获取类
            CtClass ctClass = classPool.get(Test.class.getName());
            String ctClassName = ctClass.getName();
            // 避免类提示重复加载
            // ctClass.replaceClassName("ApiTest", "ApiTest02");

            CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
            for (final CtMethod ctMethod : declaredMethods) {

                // 方法信息：methodInfo.getDescriptor();
                MethodInfo methodInfo = ctMethod.getMethodInfo();
                String ctMethodName = ctMethod.getName();

                // 方法：入参信息
                CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
                LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
                CtClass[] parameterTypes = ctMethod.getParameterTypes();
                int parameterSize = parameterTypes.length;

                boolean isStatic = (methodInfo.getAccessFlags() & AccessFlag.STATIC) != 0;  // 判断是否为静态方法
                List<String> parameterNameList = new ArrayList<>(parameterSize);            // 入参名称
                List<String> parameterTypeList = new ArrayList<>(parameterSize);            // 入参类型
                StringBuilder parameters = new StringBuilder();                             // 参数组装；$1、$2...，$$可以获取全部，但是不能放到数组初始化

                int idx = isStatic ? 0 : 1;
                for (int i = idx; i < parameterTypes.length; i++) {
                    parameterNameList.add(attr.variableName(i)); // 静态类型去掉第一个this参数
                    parameterTypeList.add(parameterTypes[i].getName());
                    if (i + 1 == parameterSize) {
                        parameters.append("$").append(i + 1);
                    } else {
                        parameters.append("$").append(i + 1).append(",");
                    }
                }

                // 方法：出参信息
                CtClass returnType = ctMethod.getReturnType();
                String returnTypeName = returnType.getName();

                // 方法：生成方法唯一标识ID
                int methodId = Monitor.generateMethodId(ctClassName, ctMethodName, parameterNameList, parameterTypeList, returnTypeName);

                // 方法前加强
                ctMethod.addLocalVariable("_startNano", CtClass.longType);
                ctMethod.insertBefore("{ _startNano = System.nanoTime(); }");

                ctMethod.addLocalVariable("_parameterValues", classPool.get(Object[].class.getName()));
                ctMethod.insertBefore("{ _parameterValues = new Object[]{" + parameters.toString() + "}; }");

                // 方法后加强
                ctMethod.insertAfter("{ com.github.giveme0101.util.Monitor.point(" + methodId + ", _startNano, _parameterValues, $_);}", false); // 如果返回类型非对象类型，$_ 需要进行类型转换

                // 方法；添加TryCatch
                ctMethod.addCatch("{ com.github.giveme0101.util.Monitor.point(" + methodId + ", $e); throw $e; }", ClassPool.getDefault().get("java.lang.Exception"));   // 添加异常捕获
//                        String ctMethodName = ctMethod.getName();
//
//                        String agentMethodName = ctMethodName + "$agent";
//                        CtMethod agentMethod = CtNewMethod.copy(ctMethod, agentMethodName, ctClass, null);
//                        ctClass.addMethod(agentMethod);
//
//                        boolean isRtnValue = ctMethod.getReturnType() != CtClass.voidType;
//
//                        ctMethod.setBody(JavaCodeUtil.build(
//                                "long startTime = System.currentTimeMillis();",
//                                isRtnValue ? "Object result = " + agentMethodName + "($$);" : agentMethodName + "($$);",
//                                "long endTime = System.currentTimeMillis();",
//                                JavaCodeUtil.log("\\n{0}:\\n" +
//                                                "      -> param  :  \\n" +
//                                                "      -> result : {1} \\n" +
//                                                "      -> cost   : \" + (endTime - startTime) + \" ms"
//                                        , ctMethod.getLongName()
//                                        , (isRtnValue ? "\" + result + \" " : "")
//                                ),
//                                // TODO 返回int等8中非包装类型会报错
//                                isRtnValue ? "return result;" : "return;"
//                        ));
            }

            // 输出类的内容
            ctClass.writeFile();

            // 测试调用
            byte[] bytes = ctClass.toBytecode();
            Class<?> clazzNew = new TestAgent().defineClass("com.github.giveme0101.Test", bytes, 0, bytes.length);

            // 反射获取 main 方法
            Method method = clazzNew.getMethod("main", String.class);
            Object obj_01 = method.invoke(clazzNew.newInstance(), "1");
            System.out.println("正确入参：" + obj_01);

        } catch (Throwable t){
            t.printStackTrace();
        }
    }

}

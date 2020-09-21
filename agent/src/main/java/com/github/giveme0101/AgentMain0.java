package com.github.giveme0101;

import com.github.giveme0101.util.JavaCodeUtil;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * @Author kevin xiajun94@FoxMail.com
 * @Description
 * @name AgentMain
 * @Date 2020/09/17 15:49
 */
public class AgentMain0 {

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new DefineTransformer(), true);
    }

    static class DefineTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

            className = className.replace("/", ".");

            if (StringUtils.contains(className, "com.github.giveme0101")){

                try {

                    // 创建新的 ClassPool，避免内存溢出
                    ClassPool classPool = new ClassPool(true);

                    // 获取类
                    CtClass ctClass = classPool.get(className);
                    String ctClassName = ctClass.getName();

                    CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                    for (final CtMethod ctMethod : declaredMethods) {

                        String ctMethodName = ctMethod.getName();

                        String agentMethodName = ctMethodName + "$agent";
                        CtMethod agentMethod = CtNewMethod.copy(ctMethod, agentMethodName, ctClass, null);
                        ctClass.addMethod(agentMethod);

                        boolean isRtnValue = ctMethod.getReturnType() != CtClass.voidType;

                        ctMethod.setBody(JavaCodeUtil.build(
                                "long startTime = System.currentTimeMillis();",
                                isRtnValue ? "Object result = " + agentMethodName + "($$);" : agentMethodName + "($$);",
                                "long endTime = System.currentTimeMillis();",
                                JavaCodeUtil.log("\\n{0}:\\n" +
                                                "      -> param  :  \\n" +
                                                "      -> result : {1} \\n" +
                                                "      -> cost   : \" + (endTime - startTime) + \" ms"
                                        , ctMethod.getLongName()
                                        , (isRtnValue ? "\" + result + \" " : "")
                                ),
                                // TODO 返回int等8中非包装类型会报错
                                isRtnValue ? "return result;" : "return;"
                        ));
                    }

                    return ctClass.toBytecode();
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }

            return classfileBuffer;
        }
    }

}

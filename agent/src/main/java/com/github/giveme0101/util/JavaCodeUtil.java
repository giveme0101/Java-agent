package com.github.giveme0101.util;

import java.text.MessageFormat;

/**
 * @Author kevin xiajun94@FoxMail.com
 * @Description
 * @name JavaCodeUtil
 * @Date 2020/09/18 10:24
 */
public class JavaCodeUtil {

    public static String build(String... codeSegment) {

        StringBuilder sb = new StringBuilder("{");

        for (final String code : codeSegment) {
            sb.append("\n").append(code);
        }

        return sb.append("\n").append("}").toString();
    }

    public static String log(String logSegment, Object ... arguments){
        String log = MessageFormat.format(logSegment, arguments);
        return "System.out.println(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(\"yyyy-MM-dd hh:mm:ss\")) + \" INFO : " + log + " \");";
    }

    public static void main(String[] args) {

        String oldMethodName = "test$old";
        String methodName = "test";

        String build = JavaCodeUtil.build(
                "long startTime = System.currentTimeMillis();",
                oldMethodName + "($$);",
                "long endTime = System.currentTimeMillis();",
                JavaCodeUtil.log("method 【{0}】 cost: \" + (endTime - startTime) + \" ms", methodName)
        );

        System.out.println(build);

    }

}

package com.github.giveme0101;

import java.util.Random;

/**
 * @Author kevin xiajun94@FoxMail.com
 * @Description
 * @name Test
 * @Date 2020/09/17 15:47
 */
public class Test {

    public static void main(String[] args) {
        int i = get();
        int j = get();
        int sum = add(i, j);
        System.out.println(sum);
    }

    public static Integer get(){
        return new Random().nextInt(100);
    }

    public static Integer add(int i, int j){
        return i + j;
    }

}

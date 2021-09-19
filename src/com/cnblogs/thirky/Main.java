package com.cnblogs.thirky;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 计算两个字符串的相识度
 */
public class Main {

    public static void main(String[] args) {
        try {
            File file=new File(args[0]);            //原文文件
            File file1=new File(args[1]);           //抄袭文件
            File file2=new File(args[2]);           //答案文件
            if (file.isFile() && file.exists() && file1.isFile() && file1.exists()) {

                //1、文件以字节流的方式读取
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                InputStreamReader read1 = new InputStreamReader(new FileInputStream(file1), StandardCharsets.UTF_8);

                //2、将文件转化为字符流
                BufferedReader bufferedReader = new BufferedReader(read);
                BufferedReader bufferedReader1 = new BufferedReader(read1);

                int text;
                int text1;

                StringBuilder response= new StringBuilder();
                StringBuilder response1= new StringBuilder();

                while ((text = bufferedReader.read()) != -1) {
                    response.append( (char)text ) ;
                }
                String result = response.toString();

                while ((text1 = bufferedReader1.read()) != -1) {
                    response1.append( (char)text1 ) ;
                }
                String result1 = response1.toString();

                //3、计算两文本的余弦相似度，并输出结果到文件file2上
                double  score=CosineSimilarity.getSimilarity(result.trim(), result1.trim());
                FileWriter fileWriter=new FileWriter(file2,true);
                fileWriter.write("原文文本："+args[0]);
                fileWriter.write("\r\n");
                fileWriter.write("抄袭文本："+args[1]);
                fileWriter.write("\r\n");
                fileWriter.write("相似度："+score);
                fileWriter.write("\r\n");

                //4.关闭流
                fileWriter.close();
                read.close();
                read1.close();
            }else{
                System.out.println("无法找到指定的文件");
            }
        } catch (Exception e) {
            System.out.println("读取文件内容操作出错");
            e.printStackTrace();
        }



    }

}

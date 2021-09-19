package com.cnblogs.thirky;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import java.util.ArrayList;
import java.io.*;
import java.io.IOException;
import java.util.List;

/**
 * 去除停用词
 */
public class StopWords {
    public static String delStopWords(String oldString) throws IOException{
        String newString=oldString;
        List<Term> termList = HanLP.segment(oldString);
        String filepath = "S:/Language/JAVA/IdeaProjects/SE_task2/lib/hit_stopwords.txt";
        File sw = new File(filepath);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(sw));
        List<String> stopWords = new ArrayList<>();
        String temp=null;
        while ((temp = bufferedReader.readLine()) != null) {
            stopWords.add(temp.trim());
        }

        List<String>tempStringList = new ArrayList<>();
        for(Term term:termList){
            tempStringList.add(term.word);
        }

        tempStringList.removeAll(stopWords);

        newString="";
        for(String string:tempStringList){
            newString +=string;
        }
        return newString;
    }
}

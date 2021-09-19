package com.cnblogs.thirky;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 分词
 * */
public class Participle {
    /**
     * @param text：需要进行分词操作的文本
     * @return 分词后的文本重新封装到Word对象中
     */
    public static List<WordGroup> segment(String text) {

        //采用HanLP中文自然语言处理中标准分词进行分词
        List<Term> termList = HanLP.segment(text);

        return termList.stream().map(term -> new WordGroup(term.word, term.nature.toString())).collect(Collectors.toList());
    }
}

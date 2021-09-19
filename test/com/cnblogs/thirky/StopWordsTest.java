package com.cnblogs.thirky;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StopWordsTest {
    public static final  String content="这只皮靴号码大了。那只号码合适";
    public static final  String content1="这只皮靴号码不小，那只更合适";
    @Test
    void delStopWords() {
        try {
            System.out.println(StopWords.delStopWords(content));
        } catch (IOException e) {
            System.out.println("停用词去除失败");
            e.printStackTrace();
        }
    }
}
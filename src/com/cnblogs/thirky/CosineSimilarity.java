package com.cnblogs.thirky;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 判定方式：余弦相似度，通过计算两个向量的夹角余弦值来评估他们的相似度 余弦夹角原理： 向量a=(x1,y1),向量b=(x2,y2) similarity=a.b/|a|*|b| a.b=x1x2+y1y2
 * |a|=根号[(x1)^2+(y1)^2],|b|=根号[(x2)^2+(y2)^2]*/
public class CosineSimilarity {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CosineSimilarity.class);

    /**
     * 计算两个字符串的相似度
     * @param text :原文文本
     * @param text1:抄袭文本
     * @return 相似度（取值范围：0~1）
     */
    public static double getSimilarity(String text, String text1) {

        //如果为空，或者字符长度为0，则代表完全相同
        if (StringUtils.isBlank(text) && StringUtils.isBlank(text1)) {
            return 1.0;
        }
        //如果一个为0或者空，一个不为，那说明完全不相似
        if (StringUtils.isBlank(text) || StringUtils.isBlank(text1)) {
            return 0.0;
        }

        String texts = null;
        try {
            texts = StopWords.delStopWords(text);
        } catch (IOException e) {
            System.out.println("停用词去除失败");
            e.printStackTrace();
        }
        String texts1 = null;
        try {
            texts1 = StopWords.delStopWords(text1);
        } catch (IOException e) {
            System.out.println("停用词去除失败");
            e.printStackTrace();
        }
        List<WordGroup> wordGroup = Participle.segment(texts);
        List<WordGroup> wordGroup1 = Participle.segment(texts1);

        return getSimilarity(wordGroup, wordGroup1);
    }

    /**
     * 对于计算出的相似度保留小数点后六位
     */
    public static double getSimilarity(List<WordGroup> wordGroups, List<WordGroup> wordGroups1) {

        double score = getSimilarityImpl(wordGroups, wordGroups1);

        //(int) (score * 1000000 + 0.5)其实代表保留小数点后六位 ,因为1034234.213强制转换不就是1034234。对于强制转换添加0.5就等于四舍五入
        score = (int) (score * 1000000 + 0.5) / (double) 1000000;

        return score;
    }

    /**
     * 文本相似度计算
     * 判定方式：余弦相似度——通过计算两个向量的夹角余弦值来评估他们的相似度
     * 余弦夹角原理： 向量X=(x1,y1),向量Y=(x2,y2) similarity=X.Y/|X|*|Y| X.Y=x1x2+y1y2
     * |X|=根号[(x1)^2+(y1)^2],|T|=根号[(x2)^2+(y2)^2]
     */
    public static double getSimilarityImpl(List<WordGroup> wordGroups, List<WordGroup> wordGroups1) {

        // 向每一个wordGroup对象的属性都注入weight（权重）属性值
        taggingWeightByFrequency(wordGroups, wordGroups1);

        //第二步：计算词频
        //通过上一步让每个Word对象都有权重值，那么在封装到map中（key是词，value是该词出现的次数（即权重））
        Map<String, Float> weightMap = getFastSearchMap(wordGroups);
        Map<String, Float> weightMap2 = getFastSearchMap(wordGroups1);

        //将所有词都装入set容器中
        Set<WordGroup> words = new HashSet<>();
        words.addAll(wordGroups);
        words.addAll(wordGroups1);

        AtomicFloat XY = new AtomicFloat();// X.Y
        AtomicFloat XX = new AtomicFloat();// |X|的平方
        AtomicFloat YY = new AtomicFloat();// |Y|的平方

        // 第三步：写出词频向量，后进行计算
        words.parallelStream().forEach(word -> {
            //看同一词在a、b两个集合出现的此次
            Float x1 = weightMap.get(word.getName());
            Float x2 = weightMap2.get(word.getName());
            if (x1 != null && x2 != null) {
                //x1x2
                float oneOfTheDimension = x1 * x2;
                //+
                XY.addAndGet(oneOfTheDimension);
            }
            if (x1 != null) {
                //(x1)^2
                float oneOfTheDimension = x1 * x1;
                //+
                XX.addAndGet(oneOfTheDimension);
            }
            if (x2 != null) {
                //(x2)^2
                float oneOfTheDimension = x2 * x2;
                //+
                YY.addAndGet(oneOfTheDimension);
            }
        });
        //|X| 对XX开方
        double XXX = Math.sqrt(XX.doubleValue());
        //|Y| 对YY开方
        double YYY = Math.sqrt(YY.doubleValue());

        //使用BigDecimal保证精确计算浮点数
        //double XXYY = XXX * YYY;
        BigDecimal XXYY = BigDecimal.valueOf(XXX).multiply(BigDecimal.valueOf(YYY));

        //similarity=X.Y/|X|*|Y|
        //divide参数说明：XXYY被除数,9表示小数点后保留9位，最后一个表示用标准的四舍五入法
        return BigDecimal.valueOf(XY.get()).divide(XXYY, 9, BigDecimal.ROUND_HALF_UP).doubleValue();
    }


    /**
     * 向每一个WordGroup对象的属性都分配权重
     */
    protected static void taggingWeightByFrequency(List<WordGroup> wordGroups, List<WordGroup> wordGroups1) {
        if (wordGroups.get(0).getWeight() != null && wordGroups1.get(0).getWeight() != null) {
            return;
        }
        //词频统计（key是词，value是该词在这段句子中出现的次数）
        Map<String, AtomicInteger> frequency1 = getFrequency(wordGroups);
        Map<String, AtomicInteger> frequency2 = getFrequency(wordGroups1);

        //如果是DEBUG模式输出词频统计信息
//        if (LOGGER.isDebugEnabled()) {
//            LOGGER.debug("词频统计1：\n{}", getWordsFrequencyString(frequency1));
//            LOGGER.debug("词频统计2：\n{}", getWordsFrequencyString(frequency2));
//        }
        // 标注权重（该词出现的次数）
        wordGroups.parallelStream().forEach(word -> word.setWeight(frequency1.get(word.getName()).floatValue()));
        wordGroups1.parallelStream().forEach(word -> word.setWeight(frequency2.get(word.getName()).floatValue()));
    }

    /**
     * 统计词频
     * @return 词频统计图
     */
    private static Map<String, AtomicInteger> getFrequency(List<WordGroup> wordGroups) {

        Map<String, AtomicInteger> freq = new HashMap<>();
        wordGroups.forEach(i -> freq.computeIfAbsent(i.getName(), k -> new AtomicInteger()).incrementAndGet());
        return freq;
    }

    /**
     *
     * @param frequency 词频数
     * @return 词频统计信息
     */
    private static String getWordsFrequencyString(Map<String, AtomicInteger> frequency) {
        StringBuilder str = new StringBuilder();
        if (frequency != null && !frequency.isEmpty()) {
            AtomicInteger integer = new AtomicInteger();
            frequency.entrySet().stream().sorted((X, Y) -> Y.getValue().get() - X.getValue().get()).forEach(
                    i -> str.append("\t").append(integer.incrementAndGet()).append("、").append(i.getKey()).append("=")
                            .append(i.getValue()).append("\n"));
        }
        str.setLength(str.length() - 1);
        return str.toString();
    }

    /**
     * 权重快速搜索容器
     */
    protected static Map<String, Float> getFastSearchMap(List<WordGroup> wordGroups) {
        if (CollectionUtils.isEmpty(wordGroups)) {
            return Collections.emptyMap();
        }
        Map<String, Float> weightMap = new ConcurrentHashMap<>(wordGroups.size());

        wordGroups.parallelStream().forEach(i -> {
            if (i.getWeight() != null) {
                weightMap.put(i.getName(), i.getWeight());
            } else {
                LOGGER.error("no WordGroup weight info:" + i.getName());
            }
        });
        return weightMap;
    }

}
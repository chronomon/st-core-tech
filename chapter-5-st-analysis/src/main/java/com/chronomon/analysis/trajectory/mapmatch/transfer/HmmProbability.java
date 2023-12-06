package com.chronomon.analysis.trajectory.mapmatch.transfer;


/**
 * 隐马尔科夫的概率计算模型: 用于计算发射概率和转移概率
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class HmmProbability {

    private final double sigma;

    private final double beta;

    public HmmProbability(double sigma) {
        this(sigma, 2.0);
    }

    public HmmProbability(double sigma, double beta) {
        this.sigma = sigma;
        this.beta = beta;
    }

    /**
     * 计算发射概率
     *
     * @param distance GPS点与投影点之间的直线距离
     * @return 发射概率值
     */
    public double emissionProbability(double distance) {
        return Math.log(1.0 / (Math.sqrt(2.0 * Math.PI) * sigma)) + (-0.5 * Math.pow(distance / sigma, 2));
    }

    /**
     * 计算转移概率
     *
     * @param candidateDist 前后两个GPS的投影点之间的路网最短路径的距离
     * @param gpsDist       前后两个GPS的直线距离
     * @return 转移概率
     */
    public double transitionProbability(double candidateDist, double gpsDist) {
        double transitionMetric = Math.abs(candidateDist - gpsDist);
        return Math.log(1.0 / beta) - (transitionMetric / beta);
    }
}

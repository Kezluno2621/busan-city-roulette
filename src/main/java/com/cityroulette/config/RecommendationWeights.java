package com.cityroulette.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 설정 파일의 추천 가중치와 후보 조회 상한을 타입 안전하게 바인딩한다. */
@Component
@ConfigurationProperties(prefix = "recommendation.weights")
public class RecommendationWeights {
    private double category = 30;
    private double distance = 30;
    private double diversity = 15;
    private double dataQuality = 15;
    private double transit = 10;
    private double maxDistanceMeters = 50_000;
    private int transitEvaluationLimit = 10;
    private int candidatePoolLimit = 200;

    public double getCategory() {
        return category;
    }

    public void setCategory(double category) {
        this.category = category;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDiversity() {
        return diversity;
    }

    public void setDiversity(double diversity) {
        this.diversity = diversity;
    }

    public double getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(double dataQuality) {
        this.dataQuality = dataQuality;
    }

    public double getTransit() {
        return transit;
    }

    public void setTransit(double transit) {
        this.transit = transit;
    }

    public double getMaxDistanceMeters() {
        return maxDistanceMeters;
    }

    public void setMaxDistanceMeters(double maxDistanceMeters) {
        this.maxDistanceMeters = maxDistanceMeters;
    }

    public int getTransitEvaluationLimit() {
        return transitEvaluationLimit;
    }

    public void setTransitEvaluationLimit(int transitEvaluationLimit) {
        this.transitEvaluationLimit = transitEvaluationLimit;
    }

    public int getCandidatePoolLimit() {
        return candidatePoolLimit;
    }

    public void setCandidatePoolLimit(int candidatePoolLimit) {
        this.candidatePoolLimit = candidatePoolLimit;
    }
}

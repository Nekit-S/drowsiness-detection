package com.drivermonitoring.ai;

public class FatiguePrediction {
    public enum RiskLevel { LOW, MEDIUM, HIGH }
    private RiskLevel riskLevel;
    private float probability; // [0,1]
    private int minutesUntilHigh;
    private String recommendation;

    public FatiguePrediction(RiskLevel riskLevel, float probability, int minutesUntilHigh, String recommendation) {
        this.riskLevel = riskLevel;
        this.probability = probability;
        this.minutesUntilHigh = minutesUntilHigh;
        this.recommendation = recommendation;
    }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public float getProbability() { return probability; }
    public void setProbability(float probability) { this.probability = probability; }
    public int getMinutesUntilHigh() { return minutesUntilHigh; }
    public void setMinutesUntilHigh(int minutesUntilHigh) { this.minutesUntilHigh = minutesUntilHigh; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}

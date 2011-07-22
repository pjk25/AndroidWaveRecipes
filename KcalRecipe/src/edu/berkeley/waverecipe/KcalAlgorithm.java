// 
//  KcalAlgorithm.java
//  KcalRecipe
//  
//  Created by Philip Kuryloski on 2011-07-19.
//  Copyright 2011 University of California, Berkeley. All rights reserved.
// 

package edu.berkeley.waverecipe;

import edu.berkeley.androidwave.waverecipe.waverecipealgorithm.*;

import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;
import java.lang.Math;
import java.util.HashMap;
import java.util.Map;

/**
 * Currently coded based on ~20Hz input, 1min output
 */
public class KcalAlgorithm implements WaveRecipeAlgorithm {
    
    private static final String TAG = KcalAlgorithm.class.getSimpleName();
    
    public static final String[] CHANNEL_NAMES = {"x", "y", "z"};
    public static final int CHANNEL_COUNT = CHANNEL_NAMES.length;
    
    private static final int WINDOW_SAMPLES = 20 * 2;   // 20Hz input assumption
    
    protected WaveRecipeAlgorithmListener theListener;
    
    protected long lastReportTime = 0;
    protected int reportInterval = 60 * 1000; // default interval of 60 seconds, should be adjusted in setAuthorizedMaxOutputRate
    protected long windowLastTime = 0;
    
    protected int sampleIndex = 0;
    protected double[][] sampleBuffer = new double[CHANNEL_COUNT][WINDOW_SAMPLES];
    
    protected double accumulatedH = 0;
    protected double accumulatedV = 0;
    
    public void setAuthorizedMaxOutputRate(double maxOutputRate) {
        reportInterval = (int)Math.ceil(1000.0 / maxOutputRate);
    }
    
    public boolean setWaveRecipeAlgorithmListener(Object listener) {
        try {
            theListener = new WaveRecipeAlgorithmListenerShadow(listener);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Exception in setWaveRecipeAlgorithmListener", e);
        }
        return false;
    }
    
    public void ingestSensorData(long time, Map<String, Double> values) {
        
        // simply store the samples
        for (int i=0; i<CHANNEL_COUNT; i++) {
            sampleBuffer[i][sampleIndex] = values.get(CHANNEL_NAMES[i]);
        }
        sampleIndex++;
        
        // if we have filled our window, generate kcals over that window
        if (sampleIndex == WINDOW_SAMPLES) {
            calculateComponents();
            windowLastTime = time;
            sampleIndex = 0;
        }
        
        // really we could do this on a separate timer for an exact interval
        // but since ingestSensorData happens around 20Hz, this is good
        // enough
        long now = SystemClock.elapsedRealtime();
        if (now - lastReportTime >= reportInterval && windowLastTime > 0) {
            Map<String, Double> outputValues = new HashMap<String, Double>(1);
            double kcal = calculateKcal(accumulatedH, accumulatedV);
            accumulatedH = accumulatedV = 0;
            outputValues.put("kcal", new Double(kcal));
            theListener.handleRecipeData(windowLastTime, outputValues);
            lastReportTime = now;
        }
    }
    
    /**
     * calculateComponents
     */
    protected void calculateComponents() {
        
        // method is currently hard coded for CHANNEL_COUNT == 3
        assert CHANNEL_COUNT == 3 : CHANNEL_COUNT;
        
        // calculate the mean of each channel
        double[] mean = new double[CHANNEL_COUNT];
        for (int i=0; i<CHANNEL_COUNT; i++) {
            double[] channel = sampleBuffer[i];
            
            for (double sample : channel) {
                mean[i] += sample;
            }
            mean[i] /= channel.length;
        }
        
        // now separate components
        double[] d = new double[CHANNEL_COUNT];
        double[] p = new double[CHANNEL_COUNT];
        for (int j=0; j<WINDOW_SAMPLES; j++) {
            
            for (int i=0; i<CHANNEL_COUNT; i++) {
                d[i] = mean[i] - sampleBuffer[i][j];
            }
            
            // NOTE: below is coded for CHANNEL_COUNT == 3
            double num = d[0] * mean[0] + d[1] * mean[1] + d[2] * mean[2];
            double den = mean[0] * mean[0] + mean[1] * mean[1] + mean[2] * mean[2];
            if (den == 0) den = 0.01;
            for (int i=0; i<CHANNEL_COUNT; i++) {
                p[i] = (num / den) * mean[i];
            }
            
            accumulatedV += Math.hypot(Math.hypot(p[0], p[1]), p[2]);
            accumulatedH += Math.hypot(Math.hypot(d[0]-p[0], d[1]-p[1]), d[2]-p[2]);
        }
    }
    
    /**
     * calculateKcal
     */
    protected static double calculateKcal(double h, double v) {
        
        // now make the Kcal computation
        h /= SensorManager.GRAVITY_EARTH;
        v /= SensorManager.GRAVITY_EARTH;
        // NOTE: we might need a scale factor somewhere (Edmund's code is a mess!)
        
        // EEact(k) = a*H^p1 + b*V^p2
        //
        // assume:  mass(kg) = 80 kg
        //          gender = 1 (male)
        //
        // a = (12.81 * mass(kg) + 843.22) / 1000 = 1.87
        // b = (38.90 * mass(kg) - 682.44 * gender(1=male,2=female) + 692.50)/1000 = 3.12
        // p1 = (2.66 * mass(kg) + 146.72)/1000 = 0.36
        // p2 = (-3.85 * mass(kg) + 968.28)/1000 = 0.66
        
        // EE in units kcal/minute --> EE_minute
        // the 4.184 is to convert from KJ to kilocalories
        return (1.87 * Math.pow(h, 0.36) + 3.12 * Math.pow(v, 0.66)) / 4.184;
    }
}

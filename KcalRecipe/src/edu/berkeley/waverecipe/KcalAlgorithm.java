// 
//  KcalAlgorithm.java
//  KcalRecipe
//  
//  Created by Philip Kuryloski on 2011-07-19.
//  Copyright 2011 University of California, Berkeley. All rights reserved.
// 

package edu.berkeley.waverecipe;

import edu.berkeley.androidwave.waverecipe.waverecipealgorithm.*;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class KcalAlgorithm implements WaveRecipeAlgorithm {
    
    private static final String TAG = KcalAlgorithm.class.getSimpleName();
    
    WaveRecipeAlgorithmListener theListener;
    
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
        throw new UnsupportedOperationException("not implemented yet");
    }
}

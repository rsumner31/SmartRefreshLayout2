package com.scwang.smartrefreshheader;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

import com.scwang.smartrefreshlayout.api.RefreshHeader;
import com.scwang.smartrefreshlayout.api.SizeDefinition;
import com.scwang.smartrefreshlayout.constant.RefreshState;
import com.scwang.smartrefreshlayout.constant.SpinnerStyle;
import com.scwang.smartrefreshlayout.internal.pathview.PathsDrawable;
import com.scwang.smartrefreshlayout.util.DensityUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 *
 * Created by SCWANG on 2017/5/31.
 */

public class TaurusHeader extends View implements RefreshHeader, SizeDefinition {

    private static final float SCALE_START_PERCENT = 0.5f;
    private static final int ANIMATION_DURATION = 1000;

    private static final float SIDE_CLOUDS_INITIAL_SCALE = 1.05f;
    private static final float SIDE_CLOUDS_FINAL_SCALE = 1.55f;

    private static final float CENTER_CLOUDS_INITIAL_SCALE = 0.8f;
    private static final float CENTER_CLOUDS_FINAL_SCALE = 1.30f;

    private static final Interpolator ACCELERATE_DECELERATE_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    // Multiply with this animation interpolator time
    private static final int LOADING_ANIMATION_COEFFICIENT = 80;
    private static final int SLOW_DOWN_ANIMATION_COEFFICIENT = 6;
    // Amount of lines when is going lading animation
    private static final int WIND_SET_AMOUNT = 10;
    private static final int Y_SIDE_CLOUDS_SLOW_DOWN_COF = 4;
    private static final int X_SIDE_CLOUDS_SLOW_DOWN_COF = 2;
    private static final int MIN_WIND_LINE_WIDTH = 50;
    private static final int MAX_WIND_LINE_WIDTH = 300;
    private static final int MIN_WIND_X_OFFSET = 1000;
    private static final int MAX_WIND_X_OFFSET = 2000;
    private static final int RANDOM_Y_COEFFICIENT = 5;

    private PathsDrawable mAirplane;
    private PathsDrawable mCloudCenter;
    private PathsDrawable mCloudLeft;
    private PathsDrawable mCloudRight;
    private Matrix mMatrix;
    private float mPercent;
    private int mHeaderHeight;
    private Animation mAnimation;

    private boolean isRefreshing = false;
    private float mLoadingAnimationTime;
    private float mLastAnimationTime;

    private Random mRandom;
    private boolean mEndOfRefreshing;

    //KEY: Y position, Value: X offset of wind
    private Map<Float, Float> mWinds;
    private Paint mWindPaint;
    private float mWindLineWidth;
    private boolean mNewWindSet;
    private boolean mInverseDirection;

    private enum AnimationPart {
        FIRST,
        SECOND,
        THIRD,
        FOURTH
    }

    public TaurusHeader(Context context) {
        this(context,null);
    }

    public TaurusHeader(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public TaurusHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mMatrix = new Matrix();
        mWinds = new HashMap<>();
        mRandom = new Random();

        mWindPaint = new Paint();
        mWindPaint.setColor(0xffffffff);
        mWindPaint.setStrokeWidth(DensityUtil.dp2px(3));
        mWindPaint.setAlpha(50);

        setupAnimations();
        setupPathDrawable();

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TaurusHeader);

        int primaryColor = ta.getColor(R.styleable.TaurusHeader_srlPrimaryColor, 0);
        if (primaryColor != 0) {
            setBackgroundColor(primaryColor);
        } else {
            setBackgroundColor(0xff11bbff);
        }

        ta.recycle();
    }

    private void setupPathDrawable() {
        DensityUtil density = new DensityUtil();

        mAirplane = new PathsDrawable();
        mAirplane.parserPaths("M60.68,16.15l-0.13,-0.11c-1.09,-0.76 -2.63,-1.16 -4.47,-1.16c-2.92,0 -5.95,0.99 -7.32,1.92l-10.76,7.35l-20.97,4.45c-0.18,0.04 -0.35,0.11 -0.51,0.22c-0.41,0.28 -0.64,0.76 -0.62,1.25c0.04,0.71 0.58,1.27 1.28,1.34l8.87,0.89l-8.65,5.9c-2.57,-1.18 -5.02,-2.33 -7.27,-3.4c-3.48,-1.67 -5.76,-1.96 -6.83,-0.89c-1.11,1.11 -0.39,3.02 0.01,3.6l8.33,10.8c0.28,0.41 0.6,0.64 0.99,0.71c0.64,0.11 1.2,-0.27 1.78,-0.68l2.11,-1.45l11.72,-5.69l-1.71,6.12c-0.19,0.68 0.14,1.38 0.78,1.68c0.18,0.08 0.39,0.13 0.59,0.13c0.29,0 0.57,-0.09 0.81,-0.25c0.16,-0.1 0.28,-0.23 0.38,-0.39l6.7,-10.19l4.1,-4.8L58.08,21.08c0.28,-0.19 0.55,-0.36 0.82,-0.54c0.63,-0.4 1.22,-0.78 1.65,-1.21C61.47,18.41 61.52,17.39 60.68,16.15z");
//        mAirplane.parserPaths("M606.8,161.5l-01.3,-01.1c-10.9,-07.6 -26.3,-11.6 -44.7,-11.6c-29.2,00 -59.5,09.9 -73.2,19.2l-107.6,73.5l-209.7,44.5c-01.8,00.4 -03.5,01.1 -05.1,02.2c-04.1,02.8 -06.4,07.6 -06.2,12.5c00.4,07.1 05.8,12.7 12.8,13.4l88.7,08.9l-86.5,59c-25.7,-11.8 -50.2,-23.3 -72.7,-34c-34.8,-16.7 -57.6,-19.6 -68.3,-08.9c-11.1,11.1 -03.9,30.2 00.1,36.l83.3,108c02.8,04.1 06,06.4 09.9,07.1c06.4,01.1 12,-02.7 17.8,-06.8l21.1,-14.5l117.2,-56.9l-17.1,61.2c-01.9,06.8 01.4,13.8 07.8,16.8c01.8,00.8 03.9,01.3 05.9,01.3c02.9,0 05.7,-0.9 08.1,-02.5c01.6,-01 02.8,-02.3 03.8,-03.9l67,-101.9l41,-48L580.8,210.8c02.8,-01.9 05.5,-03.6 08.2,-05.4c06.3,-04 12.2,-07.8 16.5,-12.1C614.7,184.1 615.2,173.9 606.8,161.5z");
        mAirplane.setBounds(0, 0, density.dip2px(60), density.dip2px(20));
        mAirplane.parserColors(0xffffffff);

        mCloudCenter = new PathsDrawable();
        mCloudCenter.parserPaths(
                "m36.46,120.94c-0.84,-1.36 -0.8,-1.38 4.42,-3.04 3.97,-1.26 4.56,-1.88 6.32,-6.64 2.75,-7.43 7.21,-10.67 14.76,-10.71 4.7,-0.03 6.44,0.48 9.89,2.87 3.66,2.54 4.51,2.75 6.75,1.73 2.37,-1.08 2.51,-1.49 1.81,-5.4C77.88,85.55 81.74,70.29 90.63,59.32 93.86,55.33 107.67,46 110.34,46c0.7,0 1.69,-0.42 2.21,-0.94 0.52,-0.52 5.38,-1.69 10.8,-2.62 8.39,-1.43 10.76,-1.49 16,-0.41 6.62,1.36 16.14,5.6 20.7,9.22 1.56,1.24 3.98,3.05 5.38,4.03 1.4,0.98 4.37,4.47 6.6,7.75 3.68,5.41 4.38,5.96 7.62,5.96 8.36,0 21.74,4.99 28.86,10.77 9.32,7.56 15.73,19.5 17.04,31.72l0.71,6.67 5.13,-0.7c3.1,-0.42 6.22,-1.62 7.91,-3.04 2.65,-2.23 8.44,-4.4 11.77,-4.41 0.85,-0 3.07,-1.57 4.91,-3.47 3.63,-3.75 9.51,-6.85 14.94,-7.89 5.78,-1.11 14.74,2.06 19.94,7.04 4.05,3.88 5,4.32 9.36,4.32 6.01,0 10.8,1.92 13.58,5.46 3.79,4.82 5.6,3.15 6.45,-5.97 1.26,-13.52 5.75,-24.35 13.61,-32.84 3.17,-3.42 14.26,-10.66 19.14,-12.5 6.17,-2.32 18.33,-4.19 23.24,-3.56 4.6,0.58 4.85,0.48 8.5,-3.6 3.52,-3.94 10.19,-9.14 14.76,-11.52 7.73,-4.02 12.16,-4.9 24.5,-4.9 11.41,0 13.11,0.25 19.5,2.84 3.85,1.56 8.8,4.13 11,5.71 2.2,1.58 5.44,3.81 7.21,4.95C464.29,55.74 473,67.58 473,69.41c0,0.25 0.54,1.73 1.19,3.27 0.66,1.55 1.67,4.84 2.25,7.32l1.06,4.5 6.5,0.61c15.65,1.47 26.87,9.07 34.22,23.16 1.5,2.88 2.74,6.26 2.75,7.51 0.03,3.05 2.3,3.75 7.35,2.26 4.27,-1.26 4.95,-1.6 10.18,-5.05 1.76,-1.16 4.69,-1.98 7.1,-1.99 2.9,-0 4.65,-0.62 6,-2.12 8.02,-8.91 20.9,-12.42 31.9,-8.71 2.71,0.91 3.72,0.54 10.5,-3.91 6.65,-4.37 10.27,-6.47 16.22,-9.39 5.02,-2.47 19.26,-5.09 26.78,-4.93 19.7,0.42 40.17,13.14 50.42,31.33 1.87,3.32 3.17,6.64 2.89,7.37 -0.43,1.11 -55.35,1.34 -326.85,1.35 -230.4,0.01 -326.53,-0.3 -327,-1.06z",
                "m4,120.62c0,-1.4 2.96,-3.72 8.5,-6.66 4.6,-2.44 13.88,-4.3 18.45,-3.69C35.43,110.87 38,109.31 38,105.99 38,103.31 42.82,92.39 46.8,86.06 54.24,74.23 67.4,63.11 80,58.03c9.62,-3.88 10.06,-4.03 11.8,-4.04 1.83,-0.01 3.99,-1.03 11.45,-5.4 2.61,-1.53 4.75,-2.57 4.75,-2.3 0,0.26 -3.12,2.34 -6.94,4.6 -9.84,5.84 -16.11,14.19 -19.66,26.15 -1.61,5.42 -2.09,16.49 -0.98,22.69 0.7,3.91 0.56,4.32 -1.81,5.4 -2.24,1.02 -3.09,0.8 -6.75,-1.73 -3.45,-2.39 -5.2,-2.89 -9.89,-2.87 -7.56,0.04 -12.02,3.28 -14.75,10.71 -1.79,4.86 -2.28,5.36 -6.61,6.69 -3.09,0.95 -4.5,1.88 -4.17,2.75C36.84,121.74 33.66,122 20.47,122 7.1,122 4,121.74 4,120.62zM521.75,121.34c0.69,-0.28 1.81,-0.28 2.5,0 0.69,0.28 0.13,0.5 -1.25,0.5 -1.38,0 -1.94,-0.23 -1.25,-0.5zM690.26,120.22c0.2,-2.59 -5.61,-12.27 -10.85,-18.1C674.88,97.07 664.57,89.64 658,86.69 648.27,82.31 635.44,80.79 625.5,82.83c-7.47,1.54 -13.36,3.09 -15.28,4.04 -5.94,2.92 -9.56,5.02 -16.22,9.39 -6.78,4.45 -7.79,4.83 -10.5,3.91 -11.01,-3.71 -23.89,-0.2 -31.9,8.71 -1.35,1.5 -3.1,2.12 -6,2.12 -2.4,0 -5.34,0.82 -7.1,1.99 -5.22,3.45 -5.91,3.79 -10.18,5.05 -5.05,1.49 -7.32,0.79 -7.35,-2.26 -0.01,-1.25 -1.25,-4.63 -2.75,-7.51C510.88,94.18 499.65,86.58 484,85.11L477.51,84.5 476.45,80c-0.58,-2.47 -1.6,-5.77 -2.25,-7.32C473.54,71.14 473,69.66 473,69.41 473,67.58 464.29,55.74 461.71,54.07 459.94,52.92 456.7,50.69 454.5,49.11c-2.2,-1.58 -7.15,-4.15 -11,-5.71 -6.39,-2.59 -8.09,-2.84 -19.5,-2.84 -9.82,-0 -13.57,0.4 -17.5,1.88 -2.75,1.04 -5.43,2.26 -5.95,2.72C400.03,45.62 399.14,46 398.58,46c-1.73,0 2.66,-2.54 7.92,-4.57 7.69,-2.97 28.03,-2.79 35.19,0.32 2.85,1.24 6.05,2.25 7.1,2.25 1.05,0 3.85,1.46 6.21,3.25 2.36,1.79 5.12,3.86 6.13,4.61 1.64,1.21 2.4,0.85 7.09,-3.36C479.4,38.44 490.79,34 505.42,34c16.11,0 27.02,4.51 38.7,16 8.03,7.89 12.53,15.61 14.96,25.59 0.65,2.7 1.54,5.3 1.97,5.78 0.43,0.48 3.86,1.01 7.61,1.18 8.92,0.4 17.54,3.71 21.74,8.35 2.46,2.72 4.76,2.65 9.4,-0.28 3.53,-2.23 10.12,-5.39 14.69,-7.05 1.1,-0.4 4.27,-1.1 7.03,-1.56 2.77,-0.46 8.14,-2.26 11.95,-4 14.44,-6.61 25.43,-9.01 41.22,-9 16.9,0.02 27.5,2.81 41.87,11.03 12.45,7.12 17.54,10.94 25.65,19.26C747.9,105.12 758,118.99 758,120.97 758,121.66 746.77,122 724.06,122c-31.96,0 -33.93,-0.1 -33.8,-1.78zM225.54,111.48C224.23,99.27 217.82,87.33 208.5,79.77 201.39,73.99 188.01,69 179.64,69c-3.24,0 -3.94,-0.55 -7.62,-5.96 -2.23,-3.28 -5.2,-6.77 -6.6,-7.75 -1.4,-0.98 -3.82,-2.8 -5.38,-4.03 -4.37,-3.47 -13.92,-7.79 -20.55,-9.3 -4.17,-0.95 -5.27,-1.48 -3.6,-1.75 4.88,-0.79 17.73,4.12 24.7,9.43 1.7,1.3 3.44,2.36 3.85,2.36 0.42,0 2.99,-2.01 5.72,-4.46 9.97,-8.97 18.68,-12.16 36.07,-13.22C220.15,33.47 232.5,38.5 243.5,49.5c7.38,7.38 11.09,13.73 14.54,24.88l1.96,6.36 6.16,0.7c12.22,1.38 26.69,11.41 31.86,22.1 1.71,3.52 2.69,4.46 4.67,4.46 3.34,0 8.53,2.54 11.36,5.56 3.05,3.25 3.95,3.08 3.95,-0.72 0,-6.77 2.35,-16.64 5.6,-23.5 3.53,-7.46 12.02,-17.76 15.55,-18.88 1.12,-0.36 3.97,-1.95 6.34,-3.55C347.85,65.31 350.42,64 351.2,64c0.77,0 1.84,-0.44 2.38,-0.98 1.82,-1.82 16.61,-4.28 21.93,-3.65 4.98,0.58 5.22,0.49 9.05,-3.48 2.17,-2.24 5.96,-5.41 8.44,-7.03 2.47,-1.62 3.15,-1.92 1.5,-0.66 -1.65,1.26 -5.42,4.69 -8.39,7.62 -5.27,5.21 -5.49,5.31 -10,4.74 -4.79,-0.61 -17.03,1.29 -23.11,3.58 -4.88,1.84 -15.98,9.08 -19.14,12.5 -7.86,8.49 -12.35,19.32 -13.61,32.84 -0.85,9.12 -2.66,10.8 -6.45,5.97C311.01,111.92 306.22,110 300.21,110c-4.36,0 -5.31,-0.44 -9.36,-4.32 -10.08,-9.66 -25.05,-9.29 -34.88,0.85 -1.85,1.91 -4.06,3.47 -4.91,3.47 -3.33,0.01 -9.12,2.18 -11.77,4.41 -1.68,1.41 -4.81,2.61 -7.91,3.04l-5.13,0.7 -0.71,-6.67zM111.31,44.4c1.54,-0.84 3.57,-1.32 4.5,-1.08 1.24,0.33 1.1,0.48 -0.5,0.56C114.1,43.95 112.84,44.45 112.5,45c-0.34,0.55 -1.38,0.98 -2.31,0.96 -1.13,-0.03 -0.76,-0.54 1.12,-1.56zM120,42c1.38,-0.44 4.07,-0.79 6,-0.79l3.5,0.02 -4,0.83c-2.2,0.46 -4.9,0.81 -6,0.79C117.88,42.81 117.98,42.65 120,42z",
                "m3.07,120.82c-0.09,-2.07 10.35,-9.73 11.58,-8.5 0.19,0.19 -0.89,0.98 -2.4,1.74 -4.18,2.12 -7.67,4.92 -8.43,6.78 -0.63,1.55 -0.68,1.55 -0.75,-0.01zM757.24,119.24c-1.17,-2.49 -1.14,-2.57 0.37,-1.07 0.9,0.9 1.46,2.11 1.26,2.69 -0.2,0.59 -0.93,-0.15 -1.63,-1.63zM314.05,113.56C311.22,110.54 306.03,108 302.69,108c-1.98,0 -2.96,-0.94 -4.67,-4.46 -5.17,-10.69 -19.63,-20.72 -31.86,-22.1l-6.16,-0.7 -1.96,-6.36C254.59,63.23 250.88,56.88 243.5,49.5 232.5,38.5 220.15,33.47 206.23,34.32 188.84,35.38 180.14,38.58 170.16,47.54 167.44,49.99 164.86,52 164.45,52 164.03,52 162.3,50.94 160.59,49.64 154.25,44.81 142.64,40.06 137,39.99l-2.5,-0.03 2.5,-1.93c12.7,-9.82 33.54,-12.11 48.87,-5.38 5.4,2.37 9.7,2.96 11.7,1.59 0.59,-0.4 2.35,-3.09 3.9,-5.97 3.36,-6.23 10.3,-13.77 16.52,-17.96 5.57,-3.74 13.55,-7.31 16.38,-7.32 1.17,-0 3.41,-0.68 4.99,-1.5C242.41,-0.1 254,-0.6 254,0.86c0,0.47 2.47,1.36 5.49,1.96 14.09,2.83 27.59,13.62 34.34,27.46 2.2,4.52 4.3,10.1 4.66,12.4 0.81,5.19 1.53,5.77 9,7.36 8.81,1.88 17.64,6.49 23.41,12.24C333.71,65.08 336,67.72 336,68.18 336,68.63 336.67,69 337.5,69c0.82,0 1.5,0.13 1.5,0.29 0,0.16 -2.65,3.02 -5.89,6.35C324.01,85 318,99.8 318,112.83c0,3.81 -0.9,3.97 -3.95,0.72zM751.67,110.69C744.68,100.17 732.18,88.77 720.75,82.51c-6.92,-3.79 -8.89,-5.4 -4,-3.27 11.86,5.16 30.84,22.17 37.56,33.67 2.92,5 1.19,3.54 -2.64,-2.22zM20,110.46c2.47,-0.83 6.87,-1.43 9.78,-1.32 5.54,0.21 6.72,-0.37 7.51,-3.65 0.48,-2 0.48,-2 0.58,0 0.2,3.89 -1.36,4.79 -8.52,4.95 -3.77,0.08 -8.42,0.46 -10.35,0.84 -2.73,0.54 -2.51,0.36 1,-0.83zM38.03,102.5c0,-0.55 0.44,-1.67 0.97,-2.5 0.53,-0.82 0.97,-1.05 0.97,-0.5 0,0.55 -0.44,1.67 -0.97,2.5 -0.53,0.82 -0.97,1.05 -0.97,0.5zM40.07,97.5c0,-0.55 0.64,-2.13 1.43,-3.5 0.79,-1.38 1.43,-2.05 1.43,-1.5 0,0.55 -0.64,2.13 -1.43,3.5 -0.79,1.38 -1.43,2.05 -1.43,1.5zM590.41,90.9c-4.2,-4.64 -12.82,-7.95 -21.74,-8.35 -3.76,-0.17 -7.18,-0.7 -7.61,-1.18 -0.43,-0.48 -1.32,-3.08 -1.97,-5.78C556.66,65.61 552.15,57.89 544.13,50 532.44,38.51 521.54,34 505.42,34c-14.63,0 -26.02,4.44 -37.21,14.5 -5.81,5.22 -4.86,5.29 -12.62,-0.87 -1.99,-1.58 -5.08,-3.22 -6.86,-3.65 -1.93,-0.46 -2.42,-0.81 -1.21,-0.87 1.11,-0.06 4.65,-2.58 7.85,-5.61 10.46,-9.89 25.44,-15.27 39.67,-14.25l7.02,0.5 4.71,-4.19C515.1,12.16 522.95,7.62 533,4.38c8.31,-2.68 28.27,-2.4 37,0.52 7.83,2.62 17.8,8.4 24.2,14.04C598.77,22.97 608,35.64 608,37.88c0,2.25 3.77,1.02 9.07,-2.96 5.4,-4.05 8.34,-5.57 16.93,-8.74 6.52,-2.4 28.63,-2.34 35.38,0.11 10.72,3.88 16.24,7.31 23.75,14.76 7.8,7.74 14.02,18.18 15.51,26.03 0.36,1.88 1.45,5.11 2.43,7.2l1.79,3.79 -3.37,-2.08c-6.26,-3.87 -21.69,-6.96 -34.78,-6.98 -15.79,-0.02 -26.77,2.38 -41.22,9 -3.8,1.74 -9.18,3.54 -11.95,4 -2.77,0.46 -5.93,1.16 -7.03,1.56 -4.57,1.66 -11.17,4.83 -14.69,7.05 -4.64,2.93 -6.94,3 -9.4,0.28zM44.47,88.06c0.84,-1.62 1.97,-3.22 2.53,-3.56 0.55,-0.34 0.06,0.98 -1.09,2.94 -2.53,4.29 -3.56,4.74 -1.44,0.62zM49.73,80.5C54.68,73.47 72.16,59 75.7,59 76.14,59 73.13,61.17 69,63.82 64.88,66.46 58.38,71.98 54.56,76.07 50.74,80.15 48.57,82.15 49.73,80.5zM340.08,69.72C340.31,69.56 341.4,68.77 342.5,67.97c1.85,-1.36 1.9,-1.34 0.63,0.28 -0.75,0.96 -1.84,1.75 -2.42,1.75 -0.57,0 -0.86,-0.13 -0.63,-0.28zM351,63.61c0,-0.22 0.7,-0.66 1.55,-0.99 0.89,-0.34 1.29,-0.17 0.94,0.39C352.91,63.96 351,64.42 351,63.61zM363.75,59.34c0.69,-0.28 1.81,-0.28 2.5,0 0.69,0.28 0.13,0.5 -1.25,0.5 -1.38,0 -1.94,-0.23 -1.25,-0.5zM378.5,59.29c1.1,-0.26 4.25,-2.57 7,-5.13l5,-4.66 -4.96,5.25c-3.06,3.23 -5.75,5.21 -7,5.13 -2.01,-0.11 -2.01,-0.12 -0.04,-0.59zM78.56,57.53C79.63,56.72 81.18,56.07 82,56.07c0.87,0 0.45,0.6 -1,1.43 -3.29,1.88 -4.91,1.9 -2.44,0.03zM85,55c0.82,-0.53 2.17,-0.97 3,-0.97 1.33,0 1.33,0.11 0,0.97 -0.82,0.53 -2.17,0.97 -3,0.97 -1.33,0 -1.33,-0.11 0,-0.97zM90.75,53.34c0.69,-0.28 1.81,-0.28 2.5,0 0.69,0.28 0.13,0.5 -1.25,0.5 -1.38,0 -1.94,-0.23 -1.25,-0.5zM95,52.61c0,-0.22 0.7,-0.66 1.55,-0.99 0.89,-0.34 1.29,-0.17 0.94,0.39C96.91,52.96 95,53.42 95,52.61zM391.76,48.79C392.94,47.37 401,42.57 401,43.29c0,0.23 -2.33,1.86 -5.18,3.64 -2.85,1.77 -4.68,2.61 -4.06,1.87zM105.44,46.56C106.51,45.7 107.82,45 108.36,45c1.27,0 0.7,0.46 -2.36,1.92 -2.36,1.12 -2.39,1.1 -0.56,-0.36zM114.75,42.34c0.69,-0.28 1.81,-0.28 2.5,0 0.69,0.28 0.13,0.5 -1.25,0.5 -1.38,0 -1.94,-0.23 -1.25,-0.5zM441,41.5c-1.38,-0.79 -2.05,-1.43 -1.5,-1.43 0.55,0 2.13,0.64 3.5,1.43 1.38,0.79 2.05,1.43 1.5,1.43 -0.55,0 -2.13,-0.64 -3.5,-1.43zM120.5,40.95c1.1,-0.44 4.7,-0.81 8,-0.8 7.54,0.01 6.14,0.54 -3,1.15 -4.07,0.27 -6.16,0.12 -5,-0.35zM406,41c0.82,-0.53 1.95,-0.97 2.5,-0.97 0.55,0 0.32,0.44 -0.5,0.97 -0.82,0.53 -1.95,0.97 -2.5,0.97 -0.55,0 -0.32,-0.44 0.5,-0.97zM412.27,39.4c0.33,-0.33 1.19,-0.37 1.92,-0.08 0.8,0.32 0.56,0.56 -0.6,0.6 -1.05,0.04 -1.64,-0.19 -1.31,-0.53zM432.75,39.34c0.69,-0.28 1.81,-0.28 2.5,0 0.69,0.28 0.13,0.5 -1.25,0.5 -1.38,0 -1.94,-0.23 -1.25,-0.5zM419.78,38.27c2.35,-0.21 5.95,-0.21 8,0.01 2.05,0.21 0.12,0.39 -4.28,0.38 -4.4,-0 -6.08,-0.18 -3.72,-0.39z"
        );
        mCloudCenter.parserColors(0xfffdfdfd,0xffe8f3fd,0xffc7dcf1);
        mCloudCenter.setBounds(0, 0, density.dip2px(200), density.dip2px(32));

        mCloudLeft = new PathsDrawable();
        mCloudLeft.parserPaths(
                "M0,91.57 L0,62.16 6.55,61.46c3.6,-0.39 8.15,-0.44 10.1,-0.13 3.3,0.54 4.04,0.12 10.7,-6.13 6.4,-6.01 9.84,-8.18 20.15,-12.72 4.06,-1.79 16.56,-2.58 24.5,-1.56 7.92,1.02 19.28,5.21 23.43,8.63 1.69,1.39 4.51,3.44 6.27,4.56C104.28,55.75 113,67.55 113,69.41c0,0.25 0.54,1.73 1.19,3.27 0.66,1.55 1.67,4.84 2.25,7.32l1.06,4.5 6.5,0.61c15.65,1.47 26.87,9.07 34.22,23.16 1.5,2.88 2.74,6.26 2.75,7.51 0.03,3.05 2.3,3.75 7.35,2.26 4.27,-1.26 4.95,-1.6 10.18,-5.05 1.76,-1.16 4.69,-1.98 7.1,-1.99 2.9,-0 4.65,-0.62 6,-2.12 8.02,-8.91 20.9,-12.42 31.9,-8.71 2.71,0.91 3.72,0.54 10.5,-3.91 6.65,-4.37 10.27,-6.47 16.22,-9.39 5.02,-2.47 19.26,-5.09 26.78,-4.93 19.73,0.42 40.16,13.14 50.44,31.38 1.89,3.35 3.21,6.44 2.94,6.88 -0.27,0.44 -74.72,0.79 -165.44,0.79L0,120.99 0,91.57z",
                "M0.17,121.49C0.45,121.22 74.75,120.99 165.28,120.99c90.53,0 164.83,-0.36 165.1,-0.79 0.81,-1.31 -6.03,-12.59 -10.93,-18.04C314.88,97.07 304.59,89.65 298,86.69 288.27,82.31 275.44,80.79 265.5,82.83c-7.47,1.54 -13.36,3.09 -15.28,4.04 -5.94,2.92 -9.56,5.02 -16.22,9.39 -6.78,4.45 -7.79,4.83 -10.5,3.91 -11.01,-3.71 -23.89,-0.2 -31.9,8.71 -1.35,1.5 -3.1,2.12 -6,2.12 -2.4,0 -5.34,0.82 -7.1,1.99 -5.22,3.45 -5.91,3.79 -10.18,5.05 -5.05,1.49 -7.32,0.79 -7.35,-2.26 -0.01,-1.25 -1.25,-4.63 -2.75,-7.51C150.88,94.18 139.65,86.58 124,85.11L117.51,84.5 116.45,80c-0.58,-2.47 -1.6,-5.77 -2.25,-7.32C113.54,71.14 113,69.64 113,69.36 113,68.14 103.95,55 103.11,55 102.61,55 100.23,53.36 97.84,51.36 95.46,49.35 92.15,47.12 90.5,46.4 87.68,45.16 87.65,45.08 89.91,45.04c1.33,-0.02 4.83,1.79 7.79,4.02l5.38,4.06 5.2,-4.67C119.4,38.44 130.82,34 145.42,34c16.11,0 27.02,4.51 38.7,16 7.97,7.84 12.14,14.98 14.87,25.5 0.71,2.75 1.65,5.39 2.07,5.87 0.42,0.48 3.85,1.01 7.6,1.18 8.92,0.4 17.54,3.71 21.74,8.35 2,2.21 6.03,2.82 7.1,1.08 1.64,-2.66 16.04,-8.63 24.02,-9.96 2.77,-0.46 8.14,-2.27 11.95,-4.01 3.8,-1.74 8.74,-3.76 10.97,-4.5 22.95,-7.53 53.96,-5.48 67.69,4.47C354.43,79.64 356.74,81 357.29,81c2.34,0 17.76,11.42 23.86,17.68C387.86,105.55 397,118.25 397,120.69 397,121.77 361.75,122 198.33,122 89.07,122 -0.11,121.77 0.17,121.49zM4,60.82c1.92,-0.44 6.54,-0.71 10.25,-0.6l6.75,0.21 6.25,-5.96L33.5,48.52 27.44,54.81 21.38,61.1 10.94,61.37C3.96,61.54 1.66,61.36 4,60.82zM39,45.61c0,-0.22 0.7,-0.66 1.55,-0.99 0.89,-0.34 1.29,-0.17 0.94,0.39C40.91,45.96 39,46.42 39,45.61zM44,43.29c0.82,-0.31 2.4,-0.97 3.5,-1.48 1.97,-0.91 1.97,-0.9 0.06,0.64 -1.07,0.86 -2.64,1.52 -3.5,1.48C42.71,43.85 42.7,43.77 44,43.29zM80,42.5C76.73,40.63 77.88,40.61 82.14,42.46 83.99,43.26 84.82,43.92 84,43.93 83.18,43.93 81.38,43.29 80,42.5zM54.24,40.31c0.96,-0.25 2.76,-0.26 4,-0.02C59.48,40.53 58.7,40.74 56.5,40.75c-2.2,0.01 -3.22,-0.18 -2.26,-0.43zM69.24,40.31c0.96,-0.25 2.76,-0.26 4,-0.02C74.48,40.53 73.7,40.74 71.5,40.75 69.3,40.76 68.28,40.56 69.24,40.31zM62.25,39.31c0.96,-0.25 2.54,-0.25 3.5,0 0.96,0.25 0.17,0.46 -1.75,0.46 -1.92,0 -2.71,-0.21 -1.75,-0.46z",
                "m397,120.69c0,-2.43 -9.14,-15.13 -15.85,-22.01C375.05,92.42 359.63,81 357.29,81c-0.54,0 -2.86,-1.36 -5.15,-3.02 -13.73,-9.95 -44.74,-11.99 -67.69,-4.47 -2.23,0.73 -7.16,2.75 -10.97,4.5 -3.8,1.74 -9.18,3.55 -11.95,4.01 -7.99,1.34 -22.38,7.31 -24.02,9.96 -1.07,1.74 -5.11,1.12 -7.1,-1.08 -4.2,-4.64 -12.82,-7.95 -21.74,-8.35 -3.76,-0.17 -7.18,-0.7 -7.6,-1.18C200.64,80.89 199.71,78.25 199,75.5 196.26,64.98 192.1,57.84 184.13,50 172.44,38.51 161.54,34 145.42,34c-14.61,0 -26.02,4.44 -37.15,14.45l-5.2,4.67 -5.38,-4.06C94.74,46.83 91.38,45 90.24,45 89.1,45 86.13,44.13 83.64,43.06 81.16,42 75.72,40.65 71.56,40.07 55.04,37.77 39.14,42.9 27.18,54.38L20.86,60.45 14.65,60.17C11.24,60.02 6.52,60.32 4.17,60.85 1.04,61.55 -0,61.5 0.28,60.67 0.82,59.03 8.6,57.81 14.82,58.38l5.32,0.49 7.18,-6.6c8.34,-7.66 15.84,-11.43 27.48,-13.79 9.17,-1.86 17.69,-1.08 28.03,2.55l5.65,1.98 3.01,-3.26C102.82,27.5 116.04,21.72 133.15,21.52c9.57,-0.11 9.68,-0.14 13.54,-3.62 8.35,-7.55 20.89,-14.17 29.39,-15.53 2.18,-0.35 4.2,-1.02 4.5,-1.5C181.33,-0.35 199.74,-0.22 200.5,1c0.34,0.55 1.43,1 2.43,1 3.68,0 14.77,4.19 21.21,8.01 7.41,4.4 17.07,13.64 20.87,19.96 5.24,8.72 4.84,8.55 10.5,4.47 13.73,-9.89 29.21,-13.9 44.58,-11.55 25.37,3.88 43.61,20.72 50.78,46.87 1.71,6.25 2.04,6.69 6.81,9.25 14.21,7.59 27.6,19.55 36.28,32.42C398.82,118.64 400.11,122 398,122c-0.55,0 -1,-0.59 -1,-1.31z"
        );
        mCloudLeft.parserColors(0x88ffffff,0x88e9f4ff,0x88c9def3);
        mCloudLeft.setBounds(0, 0, density.dip2px(82), density.dip2px(25));

        mCloudRight = new PathsDrawable();
        mCloudRight.parserPaths(
                "m37.4,124.25c0.41,-0.41 2.82,-1.45 5.36,-2.32 4.21,-1.43 4.76,-1.99 6.18,-6.41 1.08,-3.34 2.76,-5.8 5.41,-7.93 3.39,-2.72 4.59,-3.09 10.1,-3.07 5.32,0.01 6.81,0.44 9.99,2.87 3.22,2.46 4.08,2.7 6.2,1.73 2.22,-1.01 2.38,-1.49 1.66,-4.87 -2.54,-11.99 0.8,-27.72 8.13,-38.25 3.23,-4.64 5.91,-7.01 12.41,-10.96C107.4,52.27 111.75,50 112.51,50c0.76,0 1.62,-0.4 1.92,-0.88 0.3,-0.48 4.93,-1.67 10.3,-2.64 8.43,-1.52 10.64,-1.58 16.27,-0.49 7.15,1.39 16.57,5.62 21.2,9.51C163.82,56.88 165.55,58 166.04,58c1.36,0 4.85,3.95 8.62,9.75 3.19,4.92 3.67,5.25 7.48,5.25 10.84,0 23.55,5.67 32.06,14.3 7.22,7.31 11.57,16 13.18,26.33l1.23,7.88 4.88,-0.35c3.51,-0.25 5.87,-1.15 8.44,-3.21 2.02,-1.62 5.45,-3.15 7.93,-3.54 3.07,-0.49 5.38,-1.71 7.79,-4.12 10.09,-10.09 24.49,-10.35 35.31,-0.62 4.38,3.94 5.31,4.34 10,4.34 5.83,0 8.19,0.96 12.62,5.15 4.72,4.46 6.02,3.21 6.77,-6.57 1.34,-17.4 8.67,-30.07 22.51,-38.89 10.53,-6.71 18.12,-8.94 30.98,-9.1 7.34,-0.09 7.34,-0.09 12,-4.7 4.6,-4.55 9.48,-8.26 13.89,-10.59L404,48.12 404,86.56 404,125 220.33,125C119.32,125 37,124.66 37.4,124.25z",
                "m6,124.12c0,-0.95 3.61,-3.56 8.5,-6.16 4.6,-2.44 13.88,-4.3 18.45,-3.69C37.43,114.87 40,113.31 40,109.99 40,107.31 44.82,96.39 48.8,90.06 56.24,78.23 69.4,67.11 82,62.03c9.62,-3.88 10.06,-4.03 11.8,-4.04 1.83,-0.01 3.99,-1.03 11.45,-5.4 2.61,-1.53 4.75,-2.55 4.75,-2.27 0,0.28 -3.49,2.57 -7.76,5.09 -6.32,3.73 -8.53,5.74 -11.94,10.83 -7.39,11.03 -10.54,26.01 -8,38 0.72,3.38 0.56,3.86 -1.66,4.87 -2.11,0.96 -2.98,0.72 -6.2,-1.73 -3.18,-2.43 -4.67,-2.86 -9.99,-2.87 -5.51,-0.01 -6.71,0.35 -10.1,3.07 -2.65,2.13 -4.33,4.6 -5.41,7.93 -1.42,4.42 -1.97,4.98 -6.18,6.41 -2.54,0.86 -4.95,1.9 -5.36,2.32C36.36,125.32 6,125.19 6,124.12zM228.56,120.74c-0.2,-0.68 -0.84,-4.37 -1.43,-8.19C224.38,94.46 210.35,79.25 192.03,74.51 188.83,73.68 184.38,73 182.14,73c-3.82,0 -4.29,-0.33 -7.48,-5.25 -3.76,-5.8 -7.26,-9.75 -8.62,-9.75 -0.49,0 -2.21,-1.11 -3.82,-2.47 -4.47,-3.76 -12.71,-7.58 -20.6,-9.54 -5.49,-1.37 -6.35,-1.79 -3.77,-1.88 6.69,-0.22 14.19,2.82 26.81,10.88 1.98,1.26 2.56,1.01 7.31,-3.26 10.12,-9.1 18.65,-12.27 36.01,-13.41 13.96,-0.91 26.99,4.44 38.02,15.62 6.85,6.94 10.7,13.65 14.04,24.44l1.96,6.36 6.25,0.7c12.38,1.38 25.83,10.69 31.85,22.04 1.8,3.4 2.98,4.52 4.75,4.52 3.19,0 8.42,2.59 11.21,5.56 3.05,3.25 3.95,3.08 3.95,-0.72 0,-6.77 2.35,-16.64 5.6,-23.5 3.53,-7.46 12.02,-17.76 15.55,-18.88 1.12,-0.36 3.97,-1.95 6.34,-3.55C349.85,69.31 352.42,68 353.2,68c0.77,0 1.84,-0.44 2.38,-0.98 1.82,-1.82 16.61,-4.28 21.93,-3.65 4.98,0.58 5.22,0.49 9.05,-3.48 2.17,-2.24 5.96,-5.41 8.44,-7.03 2.47,-1.62 3.16,-1.92 1.52,-0.66C394.88,53.47 391.17,56.75 388.29,59.5l-5.25,5 -7.27,0.09c-12.78,0.16 -20.39,2.4 -30.91,9.1 -13.84,8.82 -21.17,21.49 -22.51,38.89 -0.75,9.77 -2.05,11.03 -6.77,6.57C311.15,114.96 308.79,114 302.96,114c-4.69,0 -5.62,-0.4 -10,-4.34 -10.82,-9.73 -25.22,-9.47 -35.31,0.62 -2.41,2.41 -4.73,3.63 -7.79,4.12 -2.44,0.39 -5.91,1.92 -7.86,3.48 -3.93,3.14 -12.8,5.02 -13.44,2.86zM113.31,48.4c1.54,-0.84 3.57,-1.32 4.5,-1.08 1.24,0.33 1.1,0.48 -0.5,0.56C116.1,47.95 114.84,48.45 114.5,49c-0.34,0.55 -1.38,0.98 -2.31,0.96 -1.13,-0.03 -0.76,-0.54 1.12,-1.56zM400.08,49.72C400.31,49.56 401.4,48.77 402.5,47.97c1.85,-1.36 1.9,-1.34 0.63,0.28 -0.75,0.96 -1.84,1.75 -2.42,1.75 -0.57,0 -0.86,-0.13 -0.63,-0.28zM122,46c1.38,-0.44 3.85,-0.79 5.5,-0.78l3,0.02 -3,0.76c-1.65,0.42 -4.13,0.77 -5.5,0.78L119.5,46.8 122,46z",
                "M6.46,121.53C13.91,115.16 25.71,111.19 32.91,112.63 36.08,113.27 39,111 39,107.91 39,105.13 44.08,94.29 48.36,87.91 58.39,73 77.93,59.12 92.53,56.55c2.19,-0.38 5.38,-1.45 7.09,-2.38 1.71,-0.92 5.49,-2.96 8.4,-4.52 6.96,-3.74 12.95,-5.5 21.47,-6.32 5.48,-0.52 7.85,-1.3 11,-3.59 13.44,-9.77 32.98,-11.31 48.69,-3.83 7.26,3.46 9.65,2.68 13.56,-4.42 4.58,-8.32 14.77,-17.86 23.59,-22.09C230.24,7.54 234.24,6 235.22,6 236.2,6 237,5.55 237,5c0,-1.28 23.66,-1.35 24.45,-0.08 0.31,0.51 2.03,1.23 3.81,1.6 5.78,1.2 16.09,7.67 22.31,13.97 6.57,6.66 12.72,17.8 13.91,25.2 0.83,5.12 1.45,5.63 9.02,7.29 8.83,1.95 17.19,6.68 24.29,13.75 6.37,6.35 7.59,7.08 8.72,5.24 1.06,-1.72 10.62,-6.16 17.48,-8.12C364.75,62.78 370.68,62 375.05,62c7.36,0 7.65,-0.09 11,-3.58 3.88,-4.02 16.78,-12.93 17.57,-12.13 0.28,0.28 -2.55,2.53 -6.3,4.99 -3.75,2.46 -8.59,6.32 -10.76,8.58 -3.84,4.01 -4.07,4.1 -9.05,3.51 -5.33,-0.63 -20.12,1.84 -21.93,3.65C355.04,67.56 353.97,68 353.2,68c-0.77,0 -3.34,1.31 -5.71,2.9 -2.37,1.6 -5.22,3.19 -6.34,3.55 -3.53,1.12 -12.02,11.42 -15.55,18.88C322.35,100.19 320,110.06 320,116.83c0,3.81 -0.9,3.97 -3.95,0.72C313.27,114.59 308.04,112 304.85,112c-1.77,0 -2.95,-1.12 -4.75,-4.52C294.08,96.14 280.63,86.83 268.25,85.44l-6.25,-0.7 -1.96,-6.36C256.7,67.59 252.85,60.87 246,53.94 234.96,42.76 221.94,37.4 207.98,38.31c-17.36,1.13 -25.89,4.31 -36.01,13.41 -4.75,4.27 -5.33,4.53 -7.31,3.26 -14.63,-9.34 -20.64,-11.41 -30.53,-10.52 -14.93,1.35 -19.34,2.77 -34.1,11.03C97.55,56.87 94.76,58 93.81,58 92.87,58 91.06,58.41 89.8,58.91 79.14,63.11 75.67,64.79 70,68.48 62.44,73.39 53.66,82.33 48.8,90.06 44.82,96.39 40,107.31 40,109.99c0,3.31 -2.57,4.87 -7.05,4.27 -7.52,-1.01 -19.65,2.95 -25.41,8.29 -1.45,1.34 -3.18,2.43 -3.83,2.41 -0.66,-0.02 0.59,-1.56 2.77,-3.43z"
        );
        mCloudRight.parserColors(0x88ffffff,0x88e9f4ff,0x88c9def3);
        mCloudRight.setBounds(0, 0, density.dip2px(82), density.dip2px(25));

    }

    @Override
    public void onPullingDown(int offset, int headHeight, int extendHeight) {
        mEndOfRefreshing = false;
        mPercent = 1f * offset / headHeight;
        mHeaderHeight = headHeight;
    }

    @Override
    public void onReleasing(int offset, int headHeight, int extendHeight) {
        mPercent = 1f * offset / headHeight;
        mHeaderHeight = headHeight;
    }

    @Override
    public void startAnimator(int headHeight, int extendHeight) {
        isRefreshing = true;
        startAnimation(mAnimation);
    }

    @Override
    public void onStateChanged(RefreshState state) {
    }

    @Override
    public void onFinish() {
        isRefreshing = false;
        mEndOfRefreshing = true;
        clearAnimation();
    }

    @Override
    public void setPrimaryColors(int... colors) {
        setBackgroundColor(colors[0]);
    }

    @NonNull
    @Override
    public View getView() {
        return this;
    }

    @Override
    public SpinnerStyle getSpinnerStyle() {
        return SpinnerStyle.Scale;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (isRefreshing) {
            // Set up new set of wind
            while (mWinds.size() < WIND_SET_AMOUNT) {
                float y = (float) (mHeaderHeight / (Math.random() * RANDOM_Y_COEFFICIENT));
                float x = random(MIN_WIND_X_OFFSET, MAX_WIND_X_OFFSET);

                // Magic with checking interval between winds
                if (mWinds.size() > 1) {
                    y = 0;
                    while (y == 0) {
                        float tmp = (float) (mHeaderHeight / (Math.random() * RANDOM_Y_COEFFICIENT));

                        for (Map.Entry<Float, Float> wind : mWinds.entrySet()) {
                            // We want that interval will be greater than fifth part of draggable distance
                            if (Math.abs(wind.getKey() - tmp) > mHeaderHeight / RANDOM_Y_COEFFICIENT) {
                                y = tmp;
                            } else {
                                y = 0;
                                break;
                            }
                        }
                    }
                }

                mWinds.put(y, x);
                drawWind(canvas, y, x, width);
            }

            // Draw current set of wind
            if (mWinds.size() >= WIND_SET_AMOUNT) {
                for (Map.Entry<Float, Float> wind : mWinds.entrySet()) {
                    drawWind(canvas, wind.getKey(), wind.getValue(), width);
                }
            }

            // We should to create new set of winds
            if (mInverseDirection && mNewWindSet) {
                mWinds.clear();
                mNewWindSet = false;
                mWindLineWidth = random(MIN_WIND_LINE_WIDTH, MAX_WIND_LINE_WIDTH);
            }

            // needed for checking direction
            mLastAnimationTime = mLoadingAnimationTime;
        }
        drawAirplane(canvas, width, height);
        drawSideClouds(canvas, width, height);
        drawCenterClouds(canvas, width, height);
    }

    /**
     * Draw wind on loading animation
     *
     * @param canvas  - area where we will draw
     * @param y       - y position fot one of lines
     * @param xOffset - x offset for on of lines
     */
    private void drawWind(Canvas canvas, float y, float xOffset, int width) {
        /* We should multiply current animation time with this coefficient for taking all screen width in time
        Removing slowing of animation with dividing on {@LINK #SLOW_DOWN_ANIMATION_COEFFICIENT}
        And we should don't forget about distance that should "fly" line that depend on screen of device and x offset
        */
        float cof = (width + xOffset) / (LOADING_ANIMATION_COEFFICIENT / SLOW_DOWN_ANIMATION_COEFFICIENT);
        float time = mLoadingAnimationTime;

        // HORRIBLE HACK FOR REVERS ANIMATION THAT SHOULD WORK LIKE RESTART ANIMATION
        if (mLastAnimationTime - mLoadingAnimationTime > 0) {
            mInverseDirection = true;
            // take time from 0 to end of animation time
            time = (LOADING_ANIMATION_COEFFICIENT / SLOW_DOWN_ANIMATION_COEFFICIENT) - mLoadingAnimationTime;
        } else {
            mNewWindSet = true;
            mInverseDirection = false;
        }

        // Taking current x position of drawing wind
        // For fully disappearing of line we should subtract wind line width
        float x = (width - (time * cof)) + xOffset - mWindLineWidth;
        float xEnd = x + mWindLineWidth;

        canvas.drawLine(x, y, xEnd, y, mWindPaint);
    }

    private void drawSideClouds(Canvas canvas, int width, int height) {
        Matrix matrix = mMatrix;
        matrix.reset();

        // Drag percent will newer get more then 1 here
        float dragPercent = Math.min(1f, Math.abs(mPercent));

        float scale;
        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            scale = SIDE_CLOUDS_INITIAL_SCALE + (SIDE_CLOUDS_FINAL_SCALE - SIDE_CLOUDS_INITIAL_SCALE) * scalePercent;
        } else {
            scale = SIDE_CLOUDS_INITIAL_SCALE;
        }

        // Current y position of clouds
        float dragYOffset = mHeaderHeight * (1.0f - dragPercent);

        // Position where clouds fully visible on screen and we should drag them with content of listView
        int cloudsVisiblePosition = mHeaderHeight / 2 - mCloudCenter.height() / 2;

//        boolean needMoveCloudsWithContent = false;
//        if (dragYOffset < cloudsVisiblePosition) {
//            needMoveCloudsWithContent = true;
//        }

        float offsetRightX = width - mCloudRight.width();
        float offsetRightY = (//needMoveCloudsWithContent
                //? mHeaderHeight * dragPercent - mCloudRight.height() :
                dragYOffset);

        float offsetLeftX = 0;
        float offsetLeftY = (//needMoveCloudsWithContent
                //? mHeaderHeight * dragPercent - mCloudLeft.height() :
                dragYOffset);

        // Magic with animation on loading process
        if (isRefreshing) {
            if (checkCurrentAnimationPart(AnimationPart.FIRST)) {
                offsetLeftY += getAnimationPartValue(AnimationPart.FIRST) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX -= getAnimationPartValue(AnimationPart.FIRST) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
            } else if (checkCurrentAnimationPart(AnimationPart.SECOND)) {
                offsetLeftY += getAnimationPartValue(AnimationPart.SECOND) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX -= getAnimationPartValue(AnimationPart.SECOND) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
            } else if (checkCurrentAnimationPart(AnimationPart.THIRD)) {
                offsetLeftY -= getAnimationPartValue(AnimationPart.THIRD) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX += getAnimationPartValue(AnimationPart.THIRD) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
            } else if (checkCurrentAnimationPart(AnimationPart.FOURTH)) {
                offsetLeftY -= getAnimationPartValue(AnimationPart.FOURTH) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX += getAnimationPartValue(AnimationPart.FOURTH) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
            }
        }

        if (offsetLeftY + scale * mCloudLeft.height() < height) {
            offsetLeftY = height - scale * mCloudLeft.height();
        }
        if (offsetRightY + scale * mCloudRight.height() < height) {
            offsetRightY = height - scale * mCloudRight.height();
        }

        final int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.translate(offsetLeftX, offsetLeftY);
        matrix.postScale(scale, scale, mCloudLeft.width() / 2, 0);
        canvas.concat(matrix);
        mCloudLeft.draw(canvas);
        canvas.restoreToCount(saveCount);
        canvas.save();
        canvas.translate(offsetRightX, offsetRightY);
        matrix.postScale(scale, scale, mCloudRight.width() / 2, 0);
        canvas.concat(matrix);
        mCloudRight.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    private void drawCenterClouds(Canvas canvas, int width, int height) {
        Matrix matrix = mMatrix;
        matrix.reset();
        float dragPercent = Math.min(1f, Math.abs(mPercent));

        float scale;
        float overdragPercent = 0;
        boolean overdrag = false;

        if (mPercent > 1.0f) {
            overdrag = true;
            // Here we want know about how mach percent of over drag we done
            overdragPercent = Math.abs(1.0f - mPercent);
        }

        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            scale = CENTER_CLOUDS_INITIAL_SCALE + (CENTER_CLOUDS_FINAL_SCALE - CENTER_CLOUDS_INITIAL_SCALE) * scalePercent;
        } else {
            scale = CENTER_CLOUDS_INITIAL_SCALE;
        }

        float parallaxPercent = 0;
        boolean parallax = false;
        // Current y position of clouds
        float dragYOffset = mHeaderHeight * dragPercent;
        // Position when should start parallax scrolling
        int startParallaxHeight = mHeaderHeight - mCloudCenter.height()/2;

        if (dragYOffset > startParallaxHeight) {
            parallax = true;
            parallaxPercent = dragYOffset - startParallaxHeight;
        }

        float offsetX = (width / 2) - mCloudCenter.width() / 2;
        float offsetY = dragYOffset
                - (parallax ? mCloudCenter.height()/2 + parallaxPercent : mCloudCenter.height()/2);

        float sx = overdrag ? scale + overdragPercent / 4 : scale;
        float sy = overdrag ? scale + overdragPercent / 2 : scale;

        if (isRefreshing && !overdrag) {
            if (checkCurrentAnimationPart(AnimationPart.FIRST)) {
                sx = scale - (getAnimationPartValue(AnimationPart.FIRST) / LOADING_ANIMATION_COEFFICIENT) / 8;
            } else if (checkCurrentAnimationPart(AnimationPart.SECOND)) {
                sx = scale - (getAnimationPartValue(AnimationPart.SECOND) / LOADING_ANIMATION_COEFFICIENT) / 8;
            } else if (checkCurrentAnimationPart(AnimationPart.THIRD)) {
                sx = scale + (getAnimationPartValue(AnimationPart.THIRD) / LOADING_ANIMATION_COEFFICIENT) / 6;
            } else if (checkCurrentAnimationPart(AnimationPart.FOURTH)) {
                sx = scale + (getAnimationPartValue(AnimationPart.FOURTH) / LOADING_ANIMATION_COEFFICIENT) / 6;
            }
            sy = sx;
        }


        matrix.postScale(sx, sy, mCloudCenter.width() / 2, 0);

        if (offsetY + sy * mCloudCenter.height() < height) {
            offsetY = height - sy * mCloudCenter.height();
        }

        final int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.concat(matrix);
        mCloudCenter.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    private void drawAirplane(Canvas canvas, int width, int height) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = mPercent;
        float rotateAngle = 0;

        if (isInEditMode()) {
            dragPercent = 1;
        }

        // Check overdrag
        if (dragPercent > 1.0f /*&& !mEndOfRefreshing*/) {
            rotateAngle = (dragPercent % 1) * 20;
            dragPercent = 1.0f;
        }

        float offsetX = ((width * dragPercent) / 2) - mAirplane.width() / 2;
        float offsetY = mHeaderHeight * (1 - dragPercent/2) - mAirplane.height() / 2;

        if (mEndOfRefreshing) {
            offsetX = width/2 + width * (1-dragPercent) / 2 - mAirplane.width() / 2;
            offsetY = mHeaderHeight / 2 * (dragPercent) - mAirplane.height() / 2;
        }

        if (isRefreshing) {
            if (checkCurrentAnimationPart(AnimationPart.FIRST)) {
                offsetY -= getAnimationPartValue(AnimationPart.FIRST);
            } else if (checkCurrentAnimationPart(AnimationPart.SECOND)) {
                offsetY -= getAnimationPartValue(AnimationPart.SECOND);
            } else if (checkCurrentAnimationPart(AnimationPart.THIRD)) {
                offsetY += getAnimationPartValue(AnimationPart.THIRD);
            } else if (checkCurrentAnimationPart(AnimationPart.FOURTH)) {
                offsetY += getAnimationPartValue(AnimationPart.FOURTH);
            }
        }

        if (rotateAngle > 0) {
            matrix.postRotate(rotateAngle,
                    mAirplane.width() / 2,
                    mAirplane.height() / 2);
        }

        final int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.concat(matrix);
        mAirplane.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public int defineHeight() {
        return (int)(Resources.getSystem().getDisplayMetrics().widthPixels * 0.3);
    }

    @Override
    public int defineExtendHeight() {
        return (int) (defineHeight() * 0.8f);
    }

    public float random(int min, int max) {

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return mRandom.nextInt((max - min) + 1) + min;
    }

    /**
     * We need a special value for different part of animation
     *
     * @param part - needed part
     * @return - value for needed part
     */
    private float getAnimationPartValue(AnimationPart part) {
        switch (part) {
            case FIRST: {
                return mLoadingAnimationTime;
            }
            case SECOND: {
                return getAnimationTimePart(AnimationPart.FOURTH) - (mLoadingAnimationTime - getAnimationTimePart(AnimationPart.FOURTH));
            }
            case THIRD: {
                return mLoadingAnimationTime - getAnimationTimePart(AnimationPart.SECOND);
            }
            case FOURTH: {
                return getAnimationTimePart(AnimationPart.THIRD) - (mLoadingAnimationTime - getAnimationTimePart(AnimationPart.FOURTH));
            }
            default:
                return 0;
        }
    }

    /**
     * On drawing we should check current part of animation
     *
     * @param part - needed part of animation
     * @return - return true if current part
     */
    private boolean checkCurrentAnimationPart(AnimationPart part) {
        switch (part) {
            case FIRST: {
                return mLoadingAnimationTime < getAnimationTimePart(AnimationPart.FOURTH);
            }
            case SECOND:
            case THIRD: {
                return mLoadingAnimationTime < getAnimationTimePart(part);
            }
            case FOURTH: {
                return mLoadingAnimationTime > getAnimationTimePart(AnimationPart.THIRD);
            }
            default:
                return false;
        }
    }

    /**
     * Get part of animation duration
     *
     * @param part - needed part of time
     * @return - interval of time
     */
    private int getAnimationTimePart(AnimationPart part) {
        switch (part) {
            case SECOND: {
                return LOADING_ANIMATION_COEFFICIENT / 2;
            }
            case THIRD: {
                return getAnimationTimePart(AnimationPart.FOURTH) * 3;
            }
            case FOURTH: {
                return LOADING_ANIMATION_COEFFICIENT / 4;
            }
            default:
                return 0;
        }
    }
    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, @NonNull Transformation t) {
                setLoadingAnimationTime(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }

    private void setLoadingAnimationTime(float loadingAnimationTime) {
        /**SLOW DOWN ANIMATION IN {@link #SLOW_DOWN_ANIMATION_COEFFICIENT} time */
        mLoadingAnimationTime = LOADING_ANIMATION_COEFFICIENT * (loadingAnimationTime / SLOW_DOWN_ANIMATION_COEFFICIENT);
        invalidate();
    }
}

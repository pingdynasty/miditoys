package com.pingdynasty.midi;

/**
 * The Veloscillator generates a MIDI-sync signal modulated by a waveform.
 */
public class Veloscillator {

    abstract class Waveform {
        public abstract float value(float fPeriodPosition);

        /**
         * generates a set of floats with values ranging from -1 to 1,
         * covering one oscillation.
         */
        public float[] generate(int datalength){
            float[] values = new float[datalength];
            float pos = 0F;
            float step = 1F / (float)datalength;
            for(int i=0; i<datalength; ++i){
                values[i] = value(pos);
                pos += step;
            }
            return values;
        }
    }

    class Sine extends Waveform {
        public float value(float fPeriodPosition){
            return (float)Math.sin(fPeriodPosition * 2.0 * Math.PI);
        }
    }

    class Square extends Waveform {
        public float value(float fPeriodPosition){
            return (fPeriodPosition < 0.5F) ? 1.0F : -1.0F;
        }
    }

    class Triangle extends Waveform {
        public float value(float fPeriodPosition){
            if(fPeriodPosition < 0.25F)
                return 4.0F * fPeriodPosition;
            else if (fPeriodPosition < 0.75F)
                return -4.0F * (fPeriodPosition - 0.5F);
            else
                return 4.0F * (fPeriodPosition - 1.0F);
        }
    }

    class Sawtooth extends Waveform {
        public float value(float fPeriodPosition){
            if(fPeriodPosition < 0.5F)
                return 2.0F * fPeriodPosition;
            else
                return 2.0F * (fPeriodPosition - 1.0F);
        }
    }

    public static void main(String[] args){
        Veloscillator velo = new Veloscillator();
        int length = 8;
        Veloscillator.Waveform wave;
        float[] values;
        wave = velo.new Sine();
        values = wave.generate(length);
        System.out.println("Sine form");
        for(int i=0; i<values.length; ++i)
            System.out.println(values[i]);
        System.out.println("Square form");
        wave = velo.new Square();
        values = wave.generate(length);
        for(int i=0; i<values.length; ++i)
            System.out.println(values[i]);
        System.out.println("Triangle form");
        wave = velo.new Triangle();
        values = wave.generate(length);
        for(int i=0; i<values.length; ++i)
            System.out.println(values[i]);
        System.out.println("Sawtooth form");
        wave = velo.new Sawtooth();
        values = wave.generate(length);
        for(int i=0; i<values.length; ++i)
            System.out.println(values[i]);
    }

}
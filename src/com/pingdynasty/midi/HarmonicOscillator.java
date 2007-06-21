package com.pingdynasty.midi;

/*
  Quantum Harmonic Oscillator Java applet:
  http://web.ift.uib.no/AMOS/MOV/HO/

  Quantum Harmonic Oscillator
  originally by Ondrej Psencik
  http://vega.fjfi.cvut.cz/docs/pvok/aplety/kv_osc/index.html

  Modified 2000 by L. Kocbach
  Modified and extended 2002 by L. Kocbach and Nazila Yavari
  Modified and reorganized 2003 by L. Kocbach

  Modified on the occasion of Glauber's Nobel Prize
  by L. Kocbach, October 2005

  This is a new documented version with many states
  Indicator of Glauber/Superposed added October 2005

*/

import java.awt.Dimension;
import javax.swing.*;

public class HarmonicOscillator {

    private int samples;

    // todo: find out discrepancies between Nstate, controls, and HeightConstant
    static final int controls = 10; // 10
    static final int Nstate = 10; // 16
    static final int  HeightConstant = 10; // 30
    static final double EnergyConstant = 0.5d; // increasing constant moves waveform up
    int[] controlvalues;
    int energyControl;

    double dt = 0.001 * 30;
    double t = 1;
    double wtx;

    double[] values;

    double AverageEnerg;

    double[] aryCn = new double[HeightConstant];  //   MOVED
    double[] rawCn = new double[HeightConstant];
    double[] alfan = new double[HeightConstant];
    double[] nfact = new double[HeightConstant];

    double[][] PsiArray;

    //   int [][]  StraightLines=new int[3][controls];
    //   int []parab=new int[212];

    double Scale = 104.0;//21.0;
    double ScaleParab = 21;//1.25*21;
//     double ScaleParab= samples / 10.0d;
    // increasing ScaleParab moves waveform up

    double HALFd = 5.0d; 
    //     double HALFd = samples / 50.0d;
    // increasing HALFd moves center of waveform to the right
    int HalfSize = 21;
//     int HalfSize= samples / 10;
    // increasing HalfSize moves center of waveform to the right
    //      double NormFact;


    public HarmonicOscillator(int samples){
        this.samples = samples;

        PsiArray = new double[samples+1][HeightConstant];

        // start values
        controlvalues = new int[]{20, 36, 45, 47, 42, 34, 25, 17, 11, 6};

        //   Fill the coefficient array used in functions PSI_n_form()

        for(int j=0;j<HeightConstant;j++)
            alfan[j]=fan(j);
        
        nfact[0] = 1.0d;
        for(int j=1;j<HeightConstant;j++)
            nfact[j]=nfact[j-1]*(double)j;

        //      double py;
        // py is used to calculate parab[] array values

        //
        //      Filling the wavefunction vectors
        //      for all the points
        //
        //      Note: point -> x  (lpoint-1.0)/HalfSize-HALFd
        //

        double exp, x;
        for(int lpoint=0; lpoint < samples; ++lpoint){
            x = ((double)lpoint) / HalfSize - HALFd;
            exp = Math.exp(-x * x / 2.0);
            for(int jquant=0; jquant < Nstate; jquant++)
                PsiArray[lpoint][jquant] = PSI_n_form(x, jquant, exp);                    
        }

        //
        //      Normalization of the wavefunctions
        //
        //   for(int j=0; j<controls; j++)
        //    {
        //      NormFact = 0.0;
        //      for(int l=1; l<212; l+=1)
        //      {
        //         NormFact = NormFact + PsiArray[l][j] * PsiArray[l][j];
        // 	  }
        //      NormFact =  Math.sqrt( NormFact );
        //      for(int l=1; l<212; l+=1)
        //      {
        //         PsiArray[l][j]=5.0 * PsiArray[l][j]/NormFact;
        //       }
        //      }

        //     for(int j=1;j<212;j++){
        // 		  py=(j-1.0)/HalfSize-HALFd;
        //        parab[j]=(int)(0.5*ScaleParab*py*py);
        //     }

        //     for(int j=1;j<11;j++){
        //     	  StraightLines[1][j]=(int)((Xdiff/2)-Math.sqrt((j-0.5) / 0.5 )*HalfSize +20);
        // 		  StraightLines[2][j]=(int)((Xdiff/2)+Math.sqrt((j-0.5) / 0.5)*HalfSize +20);
        //     }

        setGlauberState((int)( (100/10)*(1.8*1.8 )  )); // See the init of EnergScroll
    }


    //
    //  Mathematical Functions 

    //
    //    Constant factors used in alfan[] filled above
    //    fan(i) = 1/ (  sqrt( n!  2^i sqrt( Pi ) )
    public double fan(int i){ 
        double d=1;
        for(int j=1; j<=i; j++)
            d=d*j;
        return( 1/ ( Math.sqrt(d*Math.sqrt(Math.PI)*Math.pow(2.0,i+0.0) ) ) );
    }

    //
    //    Hermite Polynomial
    public double hermite(int n, double x){ 
        double h2 = 1.0d;
        double h1 = 2.0d * x;
        double hn = 1.0d;
        if(n==0) 
            return h2;
        if(n==1) 
            return h1;

        for(int i=2; i<=n; ++i){
            hn = 2.0d * x * h1 - 2.0d * ((double)i - 1.0d) * h2;
            h2 = h1; 
            h1 = hn;
        }
        return(hn);
    }

    //
    //    n-th state of Harm.Osc. in terms of Hermite Polynomial
    //    and the constant alfan[n+1]
    public double PSI_n_form(double x, int n, double exp){
        return(alfan[n] * hermite(n, x) * exp);
    }

//     public double PSI_n_form(double x, int n){
// //         return( alfan[n+1]*hermite(n,x)*Math.exp(-x*x/2.0));
// //         return(alfan[n]*hermite(n,x)*Math.exp(-x*x/2.0));
// //         System.out.println(x+"\t"+n+"\t"+alfan[n]+"\t"+hermite(n,x)
// //                            +"\t"+Math.exp(-x*x/2.0));
//         return(alfan[n] * hermite(n, x) * Math.exp(-x * x / 2.0));
//     }

    //
    //    Constructing the Wavefunction for a given  time
    //    and the constant alfan[n+1]
    public void PSI(double t, double x, int xx){  
        double Cn;
        double Re = 0;
        double Im = 0;
        for(int i=0;i<Nstate;i++){
            Cn = aryCn[i];
            if(Cn>0.0){
                Re += Math.cos((i-1+0.5)*t)*Cn*PsiArray[xx+1][i];  //  PSI_n_form(x,i-1);  // *PsiArray[xx+1][i];
                Im += Math.sin((i-1+0.5)*t)*Cn*PsiArray[xx+1][i];  //  PSI_n_form(x,i-1); // *PsiArray[xx+1][i];
            }
        }
        wtx=Scale*(Re*Re+Im*Im);
    }

    public int getEnergy(){
        return energyControl;
    }

    public void setEnergy(int energyControl){
        this.energyControl = energyControl;
        setGlauberState(energyControl);
    }

    public int getControls(){
        return controlvalues.length;
    }

    public int getControl(int index){
        assert index < controlvalues.length;
        return controlvalues[index];
    }

    public void setControl(int index, int value){
        System.out.println("control "+index+": "+value);
        assert index < controlvalues.length;
        assert value <= 100;
        assert value >= 0;
        controlvalues[index] = value;
        updateControlValues();
    }

    //  Amplitude controls start

    // Normalize amplitudes
    public void normalizeAmplitudes(){
        double arySum = 0.0001d;
        for(int i=0;i<Nstate;i++)
            arySum = arySum + rawCn[i] * rawCn[i];
        double aryFact  = 1.0d / Math.sqrt(arySum);

        for(int i=0;i<Nstate;i++){
            aryCn[i] = rawCn[i] * aryFact;
            rawCn[i] = rawCn[i] * aryFact;
        }

        // Evaluate AverageEnerg
        AverageEnerg = EnergyConstant;
        for(int i=0; i<controls; i++)
            AverageEnerg += ((double)i) * aryCn[i] * aryCn[i];
        energyControl = (int)(100-(100/10)*(AverageEnerg-EnergyConstant));
    }

    // Setting of amplitudes by scrollers
    public void updateControlValues(){
        double arySum;   
        double aryFact;
//         GlauberFlag=0;
        // CONTROLS.L_Superp.setText(" Superposed State");   // GlauberFlag == 0
        // CONTROLS.L_Glaub.setText("--");                   //
        // CONTROLS.L_Glaub.setText(" Glauber State ");      // GlauberFlag == 1
        // CONTROLS.L_Superp.setText("--");                  //

        for(int i=0;i<controlvalues.length;++i){
            if(controlvalues[i] >= 99) // todo: find out why this is coded this way
                rawCn[i] = 0;
            else
                rawCn[i] = (double)(100 - controlvalues[i] / 100);
//                    if (CONTROLS.AmplitScroll[whichstate].getValue()==99) rawCn[whichstate]=0;
//                    else
//                    rawCn[whichstate]=(double)(100-CONTROLS.AmplitScroll[whichstate].getValue())/100;
        }

        normalizeAmplitudes();
//         CONTROLS.EnergScroll.setValue( (int)( 100-(100/10)*(AverageEnerg-EnergyConstant))  );
    }

    //    Setting of amplitudes for a single state by buttons
    public void setSingleState(int Nvalue){
        System.out.println("single state "+Nvalue);
        double arySum;
        double aryFact;

//         GlauberFlag=-1;
        // CONTROLS.L_Superp.setText(" Single State");   // GlauberFlag == 0
        // CONTROLS.L_Glaub.setText("--");                   //
        // CONTROLS.L_Glaub.setText(" Glauber State ");      // GlauberFlag == 1
        // CONTROLS.L_Superp.setText("--");                  //

        for(int i=0; i<controls; ++i)
            rawCn[i] = 0.0;
        rawCn[Nvalue] = 1.0;

        for(int i=0; i<Nstate; ++i)
            aryCn[i] = 0.0;

        normalizeAmplitudes();

        for(int i=0; i<controls; ++i){
            rawCn[i] = 100.0d * aryCn[i];
            controlvalues[i] = 100 - (int)rawCn[i];
//                 CONTROLS.AmplitScroll[whichstate].setValue(100-(int)rawCn[whichstate]);
        }

// 	   CONTROLS.EnergScroll.setValue( (int)( 100-(100/10)*(AverageEnerg-EnergyConstant) )  );

    }

    //    Setting of amplitudes for Glauber State by energy selector scroller
    public void setGlauberState(int Nvalue){
        System.out.println("glauber state "+Nvalue);
        energyControl = Nvalue;

        double arySum;   
        double aryFact; 
        double Xx; 
        double Xx2;

//         GlauberFlag = 1;
        // CONTROLS.L_Superp.setText(" Superposed State");   // GlauberFlag == 0
        // CONTROLS.L_Glaub.setText("--");                   //
//         CONTROLS.L_Glaub.setText(" Glauber State ");          // GlauberFlag == 1
//         CONTROLS.L_Superp.setText("--");                      //

        AverageEnerg = EnergyConstant+0.0001+(10.0-0.1*(double)Nvalue);  // SETENERGY
        Xx = Math.sqrt(AverageEnerg-EnergyConstant);

        for(int i=0; i<Nstate; ++i){
            //  Matlab values
            //  for N=2:10,fac(N)=fac(N-1)*(N-1);end
            //  for N=0:9,coef(N+1)=x^N/sqrt(fac(N+1))*exp(-0.5*x^2);end
            Xx2 = 0.5d * Math.pow(Xx, 2);
            aryCn[i] = Math.pow(Xx,i) / Math.sqrt(nfact[i]) * Math.exp(-Xx2);
        }

        for(int i=0; i<controls; i++)
            rawCn[i]=aryCn[i];

        for(int i=0; i<controls; ++i){
            rawCn[i]=100*aryCn[i];
            controlvalues[i] = 100 - (int)rawCn[i];
        }

    }

    //  Amplitude controls end

    jfftw.real.Plan fft;
    java.io.ByteArrayOutputStream out;
    //     java.io.FileOutputStream out;
    double scale = 0.01d;

    public void initfft(){
        //         fft = new jfftw.real.Plan(samples);
        fft = new jfftw.real.Plan(samples, jfftw.real.Plan.BACKWARD
                                  //                                   | jfftw.real.Plan.COMPLEX_TO_REAL
                                  | jfftw.real.Plan.REAL_TO_COMPLEX 
                                  | jfftw.real.Plan.IN_PLACE
                                  );
        try{
            //             out = new java.io.FileOutputStream("glauber.out");
            out = new java.io.ByteArrayOutputStream();
        }catch(Exception exc){exc.printStackTrace();}
    }

    public void fft(double[] values){
        try{
            values = fft.transform(values);
            for(int i=0; i<values.length; ++i)
                out.write((int)(values[i]*scale));
            //             int max = 0, min = 0, value;
            //             for(int i=0; i<values.length; ++i){
            //                 value = (int)(values[i]*scale);
            //                 out.write(value);
            //                 if(value > max)
            //                     max = value;
            //                 else if(value < min)
            //                     min = value;
            //             }
            //             System.out.println("max "+max+" min "+min);
            out.flush();
        }catch(Exception exc){exc.printStackTrace();}
    }

    public void closefft(){
        try{
            float sampleRate = 8000.0f;
            //8000,11025,16000,22050,44100
            javax.sound.sampled.AudioFormat format = 
                new javax.sound.sampled.AudioFormat(sampleRate, 8, 1, true, true);
            // float sampleRate, float sampleSizeInBits, int channels, 
            // boolean signed, boolean bigEndian
            byte[] data = out.toByteArray();
            int length = data.length / 8;
            System.out.println("format "+format);
            System.out.println("frame size "+format.getFrameSize());
            System.out.println("data "+data.length+" samples "+length);
            java.io.ByteArrayInputStream bytestream = 
                new java.io.ByteArrayInputStream(data);
            javax.sound.sampled.AudioInputStream stream = 
                new javax.sound.sampled.AudioInputStream(bytestream, format, length);
            javax.sound.sampled.AudioSystem.write(stream, javax.sound.sampled.AudioFileFormat.Type.WAVE, new java.io.File("glauber.wav"));
        }catch(Exception exc){exc.printStackTrace();}
    }

    public double[] calculate() {  
//         int EnerPos = (int)(AverageEnerg*ScaleParab);  // Convert energy to position

        PSI(t,0,0);

        //
        //	Drawing the line of the wavefunction in samples points
        values = new double[samples];
        values[0] = wtx;
        for(int i=1; i<samples; ++i){
            PSI(t, (i) / HalfSize - HALFd, i);
            values[i] = wtx;
        }

//         fft(values);
        return values;
    }

    public void increment(){
        t += dt;
    }

    public void dump(){
        for(int j=0;j<HeightConstant;j++)
            System.out.println("alfan["+j+"] "+alfan[j]);
        for(int j=0;j<HeightConstant;j++)
            System.out.println("nfact["+j+"] "+nfact[j]);

        for(int lpoint=0; lpoint < samples; ++lpoint)
            for(int jquant=0; jquant < Nstate; jquant++)
                System.out.println("PsiArray["+lpoint+"]["+jquant+"]"+PsiArray[lpoint][jquant]);
    }

    public static void main(String[] args)
        throws Exception {
        int width = 512;
        int height = 200;

//         Box content = Box.createHorizontalBox();
        Box content = Box.createVerticalBox();
        Dimension dim = new Dimension(width, height);

        HarmonicOscillator osc = new HarmonicOscillator(width);
//         osc.dump();
        HarmonicOscillatorControlPanel control = 
            new HarmonicOscillatorControlPanel(osc);
        control.setPreferredSize(dim);
        control.setMinimumSize(dim);
//         control.setMaximumSize(dim);

//         control.setSize(width, height);
        OscillatorPanel panel = new OscillatorPanel(width, height);
//         panel.setSize(width, height);
        content.add(control);
        content.add(panel);

        panel.setData(osc.calculate());

        JFrame frame = new JFrame("harmonic oscillator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height * 2);
        frame.setContentPane(content);
        frame.setVisible(true);

        for(;;){
            Thread.sleep(10);
            osc.increment();
            panel.setData(osc.calculate());
        }

//         for(int i=0; i<500; ++i){
//             Thread.sleep(10);
//             osc.increment();
//             panel.setData(osc.calculate());
//         }
//         osc.setControl(9, 80);
//         for(int i=0; i<500; ++i){
//             Thread.sleep(10);
//             osc.increment();
//             panel.setData(osc.calculate());
//         }
//         osc.setControl(5, 99);
//         osc.setControl(4, 0);
//         for(int i=0; i<500; ++i){
//             Thread.sleep(10);
//             osc.increment();
//             panel.setData(osc.calculate());
//         }
//         osc.setGlauberState(3);
//         for(int i=0; i<500; ++i){
//             Thread.sleep(10);
//             osc.increment();
//             panel.setData(osc.calculate());
//         }
//         osc.setSingleState(3);
//         for(int i=0; i<500; ++i){
//             Thread.sleep(10);
//             osc.increment();
//             panel.setData(osc.calculate());
//         }

//         double[] values = osc.calculate();
//         for(int i=0; i<values.length; i+=2)
//             System.out.println(values[i]+"\t"+values[i+1]);
    }
}


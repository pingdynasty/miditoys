package com.pingdynasty.midi;


public class ScaleMapper {

    private String[] scalenames = new String[]{
        "C minor blues scale",
        "C major blues scale",
        "Ionian mode",
        "Dorian mode",
        "Phrygian mode",
        "Lydian mode",
        "Mixolydian mode",
        "Aeolian mode",
        "Locrian mode"};

    private int[][] scales = new int[][]{
        // C minor blues scale: C Eb F F# G Bb C
        {0, 3, 5, 6, 7, 10},
        // C major blues scale: C D D# E G A C
        {0, 2, 3, 4, 7, 9},
        // the seven modes of the diatonic major scale and added-note scales.
        // Ionian mode 	C D E F G A B C 	(associated with C Major 7 chord)
        {0,2,4,5,7,9,11},
        // Dorian mode 	C D Eb F G A Bb C 	(associated with C-7 chord)
        {0,2,3, 5,7,9,12},
        // Phrygian mode C Db Eb F G Ab Bb C 	(associated with C Phrygian chord)
        {0,1, 3, 5,7,10,12},
        // Lydian mode 	C D E F# G A B C 	(associated with C Maj7 #4 chord)
        {0,2,4,6, 7,9,11},
        // Mixolydian mode C D E F G A Bb C 	(associated with C7 chord)
        {0,2,4,5,7,9,12},
        // Aeolian mode D Eb F G Ab Bb C 	(associated with C-7 b6 chord)
        {2,3, 5,7,8, 12},
        // Locrian mode	C Db Eb F Gb Ab Bb C 	(associated with C-7b5 chord)
        {0,1, 3, 5,6, 8, 12}
    };
    int scaleindex = 0;

    public ScaleMapper(){}

    public String[] getScaleNames(){
        return scalenames;
    }

    public void setScale(int scaleindex){
        this.scaleindex = scaleindex;
    }

    public void setScale(String name){
        int i=0;
        for(; i<scales.length && !scalenames[i].equals(name); ++i);
        if(i == scales.length)
            throw new IllegalArgumentException("no such scale: "+name);
        scaleindex = i;
    }

    /** map an arbitrary number to a MIDI note on the current scale */
    public int getNote(int key){
        int note = scales[scaleindex][key % scales[scaleindex].length];
//         System.out.println("key "+key+" ("+key % scales[scaleindex].length+"): "+note);
//         System.out.println("key / scales[scaleindex].length = "+key / scales[scaleindex].length);
        note += (key / scales[scaleindex].length) * 12;
        System.out.println("note "+note);
        // 12 is the length of an octave in midi notes
        return note;
    }

    /** approximate reverse mapping from MIDI note (though not all notes are represented on all scales) to key */
    public int getKey(int note){
        int key  = note % 12;
        key += (note / 12) * scales[scaleindex].length;
        return key;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bill.metronome.utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;



/**
 *
 * @author Bill
 */
public class MidiHandler 
{
    private Sequencer sequencer;
    private Sequence seq;
    private float newTempoFactor;
    private String filePath;
    
    public MidiHandler()
    {
        try 
        {
            sequencer = MidiSystem.getSequencer();
            if (sequencer == null)
            {
                System.err.println("Sequencer not supported");
            }
            sequencer.open();
        } 
        catch (MidiUnavailableException ex) 
        {
            Logger.getLogger(MidiHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setAudioTrack(String filePath)
    {
        this.filePath=filePath;
        try 
        {
            seq= MidiSystem.getSequence(new File(filePath));
            sequencer.setSequence(seq);
        } 
        catch (InvalidMidiDataException | IOException ex) 
        {
            Logger.getLogger(MidiHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void playTrack(float bpm) throws InterruptedException
    {
        try 
        {
            seq=editEvents();//editEvents() method pushes all midi events 100 ticks forward
            sequencer.setSequence(seq);
        } 
        catch (InvalidMidiDataException | IOException ex) 
        {
            Logger.getLogger(MidiHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

         sequencer.start();
         sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
         newTempoFactor=bpm/120;
         System.out.println("New tempo factor: "+newTempoFactor);
         sequencer.setTempoFactor(newTempoFactor);//Default tempo is 120bpm --> Tempo factor =1 
         sequencer.setLoopStartPoint(100);//Shift the loop start/end by 100 ticks
         sequencer.setLoopEndPoint(seq.getTickLength()); 
    }
    
    public void stopTrack()
    {
        System.out.println("Stopping");
        sequencer.stop();
        sequencer.setTickPosition(0);//Reset midi track
    }
    
     public Sequence editEvents() throws InvalidMidiDataException, IOException
    {
        Sequence seq= this.seq;
        try 
        {
            seq = MidiSystem.getSequence(new File(this.filePath));
            for (Track track :  seq.getTracks()) 
            {
                for (int i=0; i < track.size(); i++) 
                { 
                    MidiEvent event = track.get(i);
                    event.setTick(event.getTick()+100);  
                }
            }
        } 
        catch (InvalidMidiDataException | IOException ex) 
        {
            Logger.getLogger(MidiHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return seq;
    }    
    
    public void setBPM(float bpm)
    {
        newTempoFactor=bpm/120;
        System.out.println("New tempo factor: "+newTempoFactor);
        sequencer.setTempoFactor(newTempoFactor);
    }
    
}

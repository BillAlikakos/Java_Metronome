/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bill.metronome.utils;

import com.bill.metronome.ui.MainWindow;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
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
    private MainWindow windowRef;
    
    /**
     * The MidiHandler class is used to load and play the metronome's midi tracks
     * @param window the instance of the main window that is displayed
     */
    public MidiHandler(MainWindow window)
    {
        this.windowRef=window; 
        try 
        {
            sequencer = MidiSystem.getSequencer();
            this.addMetaListener();
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
    
    /**
     * Loads the appropriate midi file from the project resources 
     * @param fileName the file name of the midi track that will be loaded
     */
    public void setAudioTrack(String fileName)
    {
        try 
        {
            seq=MidiSystem.getSequence(MidiHandler.class.getClassLoader().getResource(fileName));
            sequencer.setSequence(seq);
        } 
        catch (InvalidMidiDataException | IOException ex) 
        {
            Logger.getLogger(MidiHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     *Sets up the midi events for playback, adds custom meta messages 
     * for noteOn/Off detection and attaches the meta event listener 
     * in order to update the UI's visualizer
     */
    public void setupHandler()
    {
        try
        {
            seq=editEvents();//editEvents() method pushes all midi events 100 ticks forward
            addCustomControlCommands(seq);
            sequencer.setSequence(seq);
        } 
        catch (InvalidMidiDataException ex) 
        {
            Logger.getLogger(MidiHandler.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    /**
     *Attaches the Meta Event Listener to the sequencer, in order to catch the custom noteOn and noteOff events
     */
    public void addMetaListener()
    {
        sequencer.addMetaEventListener(new MetaEventListener(){
            @Override
            public void meta(MetaMessage meta)
            { 
                    if(meta.getType()==10)//If the meta event is the custom noteOn event
                    {
                        handleNoteEvent(windowRef.getNextBeat(),true);
                    }
                    else if(meta.getType()==11)
                    {
                        handleNoteEvent(windowRef.getNextBeat(),false);
                    }
            }
        });
    }
    
    /**
     *Begins the playback loop for the selected midi file and bpm
     * @param bpm the desired beats per minute value
     */
    public void playTrack(float bpm)
    {
        sequencer.setTickPosition(0);
        sequencer.start();
        sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        newTempoFactor=bpm/120;
        System.out.println("New tempo factor: "+newTempoFactor);
        sequencer.setTempoFactor(newTempoFactor);//Default tempo is 120bpm --> Tempo factor =1 
        sequencer.setLoopStartPoint(100);//Shift the loop start/end by 100 ticks
        sequencer.setLoopEndPoint(seq.getTickLength());  
    }
    
    
    /**
     * 
     * @return a reference to the MidiHandler's Sequencer object
     */
    public Sequencer getSequencer()
    {
        return this.sequencer;
    }

    private void handleNoteEvent(int beat, boolean noteOn)
    {
        if(noteOn)
        {
            System.out.println(beat+" NoteOn");
            windowRef.setIcon(beat, true);
        }
        else
        {
            System.out.println(beat+" NoteOff");
            windowRef.setIcon(beat, false);
        }
    }
    
    /**
     * Stops the playback loop and resets the Sequencer tick position to the start of the file
     */
    public void stopTrack()
    {
        System.out.println("Stopping");
        sequencer.stop();
        sequencer.setTickPosition(0);//Reset midi track
    }
    
    private Sequence editEvents() //Pushes all midi events 100 ticks ahead to compensate for midi sequencer startup delay
    {
        Sequence sequence= this.seq;
        {
            for (Track track :  sequence.getTracks()) 
            {
                for (int i=0; i < track.size(); i++) 
                { 
                    MidiEvent event = track.get(i);
                    event.setTick(event.getTick()+100);
                }
            }
        }    
        return sequence;
    }    
    
    /**
     *Sets the metronome's bpm
     * @param bpm the desired beats per minute value
     */
    public void setBPM(float bpm)
    {
        newTempoFactor=bpm/120;
        System.out.println("New tempo factor: "+newTempoFactor);
        sequencer.setTempoFactor(newTempoFactor);
    }
    
    private void addCustomControlCommands(Sequence sequence) throws InvalidMidiDataException //Adds custom meta messages for the detection of noteOn and noteOff events
    {
        for (Track track :  sequence.getTracks()) 
        {
            for (int i=0; i < track.size(); i++) 
            { 
                MidiEvent event = track.get(i);
                final MidiMessage message = event.getMessage();//Get the midi event's message
                if (message instanceof ShortMessage) 
                {
                    final ShortMessage shortMessage = (ShortMessage) message;
                    final int command = shortMessage.getCommand();
                    int customCom = -1;
                    if (command == ShortMessage.NOTE_ON)//If it's a note on event,set the according control flag
                    {
                        customCom=10;
                        //System.out.println("Note on: "+event.getTick());
                    }
                    else if (command == ShortMessage.NOTE_OFF) //If it's a note off event set the according control flag
                    {
                        customCom=11;
                       //System.out.println("Note off: "+event.getTick());
                    }
                    if (customCom > 0) //If it's a note on or off event, add a custom meta event (with the appropriate flag) in order to signify the event to the meta event listener
                    {        
                        byte[] b = shortMessage.getMessage();
                        int length =0;
                        if(b!= null)
                        {
                            length =b.length;
                        }
                        MetaMessage metaMessage = new MetaMessage(customCom, b, length);
                        MidiEvent me2 = new MidiEvent(metaMessage, event.getTick());
                        track.add(me2);  
                    }
                }
            }
        }
    }
    
}

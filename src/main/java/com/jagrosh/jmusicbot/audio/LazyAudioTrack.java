package com.jagrosh.jmusicbot.audio;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

public class LazyAudioTrack extends DelegatedAudioTrack
{

    private boolean loaded = false;
    private AudioTrack track;
    private final Function<AudioTrackInfo, AudioItem> loadItem;

    public LazyAudioTrack(@NotNull AudioTrackInfo trackInfo, Function<AudioTrackInfo, AudioItem> loadItem)
    {
        super(trackInfo);
        this.loadItem = loadItem;
    }

    private void load() throws Exception
    {
        AudioItem item = loadItem.apply(trackInfo);
        if (item == null)
        {
            throw new Exception("Failed to load item from source manager");
        }

        if (item instanceof AudioTrack)
        {
            this.track = (AudioTrack) item;
        }
        else if (item instanceof AudioPlaylist)
        {
            this.track = ((AudioPlaylist)item).getTracks().get(0);
        }
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception
    {
        if (!loaded)
        {
            load();
            loaded = true;
        }

        super.processDelegate((BaseAudioTrack) this.track, localExecutor);
    }
}
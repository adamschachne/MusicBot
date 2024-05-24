package com.jagrosh.jmusicbot.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifySourceManager extends YoutubeAudioSourceManager
{
    private final static Logger log = LoggerFactory.getLogger(SpotifySourceManager.class);

    
    private final String spotifyRegex = "(?:https?:\\/\\/)?(?:www\\.)?(?:open\\.)?spotify\\.com\\/(track|album|playlist)\\/([a-zA-Z0-9]+).*";
    private final Pattern pattern = Pattern.compile(spotifyRegex);

    private SpotifyClient spotify;
    private boolean enabled;


    public SpotifySourceManager(int maxPlaylistPageCount)
    {
        super(true);
        this.enabled = false;
        super.setPlaylistPageCount(maxPlaylistPageCount);
        Config config = ConfigFactory.load();

        if (config.hasPath("spotify.clientid") && config.hasPath("spotify.clientsecret"))
        {
            String clientId = config.getString("spotify.clientid");
            String clientSecret = config.getString("spotify.clientsecret");
            this.spotify = new SpotifyClient(clientId, clientSecret);

            try 
            {
                spotify.ensureAccessToken();
            }
            catch (Exception ex)
            {
                log.warn("Invalid Spotify credentials", ex);
            }
            
            this.enabled = true;
        }
    }

    @Override
    public String getSourceName()
    {
        return "spotify";
    }

    private AudioItem loadItemFromYoutube(AudioPlayerManager apm, String artist, String title)
    {
        return super.loadItem(apm, new AudioReference(String.format("ytsearch:%s - %s", artist, title), title));
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager apm, AudioReference ar) 
    {
        if (ar.identifier == null) 
        {
            return null;
        }

        try
        {
            String url = ar.identifier;

            // determine if it is a track, album, or playlist from regex groups
            Matcher matcher = pattern.matcher(url);
            if (matcher.matches())
            {
                String type = matcher.group(1);
                String identifier = matcher.group(2);
                log.info(type + ": " + identifier);
                
                if ("track".equals(type)) 
                {   
                    Track track = spotify.fetchTrackInfo(identifier);
                    String artist = track.getArtists()[0].getName();
                    String title = track.getName();
                    return loadItemFromYoutube(apm, artist, title);
                }
                else if ("playlist".equals(type))
                {
                    Playlist playlistTracks = spotify.fetchPlaylistInfo(identifier);
                    String playlistName = playlistTracks.getName();
                    List<AudioTrack> tracks = new ArrayList<>();
                    for (PlaylistTrack playlistTrack : playlistTracks.getTracks().getItems())
                    {   
                        IPlaylistItem track = playlistTrack.getTrack();
                        String artist = "";
                        String title = "";
                        if (track == null)
                        {
                            continue;
                        }
                        if (track instanceof Track)
                        {
                            artist = ((Track) track).getArtists()[0].getName();
                            title = ((Track) track).getName();
                        } 
                        else if (track instanceof TrackSimplified)
                        {
                            artist = ((TrackSimplified) track).getArtists()[0].getName();
                            title = ((TrackSimplified) track).getName();
                        }
                        AudioTrackInfo info = new AudioTrackInfo(title, artist, track.getDurationMs(), title, false, "");
                        LazyAudioTrack ytTrack = new LazyAudioTrack(info, audioTrackInfo -> loadItemFromYoutube(apm, audioTrackInfo.title, audioTrackInfo.author));
                        tracks.add(ytTrack);
                    }
                    return new BasicAudioPlaylist(playlistName, tracks, null, false);
                } 
                else if ("album".equals(type)) 
                {
                    Album albumTracks = spotify.fetchAlbumInfo(identifier);
                    List<AudioTrack> tracks = new ArrayList<>();
                    for (TrackSimplified track : albumTracks.getTracks().getItems()) 
                    {
                        String artist = track.getArtists()[0].getName();
                        AudioItem item = loadItemFromYoutube(apm, artist, track.getName());
                        if (item instanceof AudioTrack) 
                        {
                            tracks.add((AudioTrack) item);
                        } 
                        else if (item instanceof AudioPlaylist) 
                        {
                            AudioPlaylist playlist = (AudioPlaylist) item;
                            tracks.add(playlist.getTracks().get(0));
                        }
                    }
                    return new BasicAudioPlaylist(albumTracks.getName(), tracks, null, false);
                }
                    
                return null;
            } 
            else 
            {
                return null;
            }

        }
        catch (Exception ex)
        {
            log.warn("Exception when trying to load item", ex);
        }
        return null;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

package com.jagrosh.jmusicbot.audio;

import java.io.IOException;
import java.time.Instant;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

public class SpotifyClient 
{
    private final SpotifyApi spotifyApi;
    private Instant tokenExpiryTime;

    public SpotifyClient(String clientId, String clientSecret) 
    {
        this.spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .build();
    }

    private void refreshAccessToken() throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException
    {
        ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
        ClientCredentials clientCredentials = clientCredentialsRequest.execute();
        spotifyApi.setAccessToken(clientCredentials.getAccessToken());
        tokenExpiryTime = Instant.now().plusSeconds(clientCredentials.getExpiresIn());
    }

    public void ensureAccessToken() throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException
    {
        if (tokenExpiryTime == null || Instant.now().isAfter(tokenExpiryTime))
        {
            refreshAccessToken();
        }
    }

    public Track fetchTrackInfo(String identifier) 
    {
        try 
        {
            ensureAccessToken();
            GetTrackRequest getTrackRequest = spotifyApi.getTrack(identifier).build();
            Track track = getTrackRequest.execute();
            return track;
        }
        catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e)
        {
            return null;
        }

    }

    public Playlist fetchPlaylistInfo(String identifier)
    {   
        try 
        {
            ensureAccessToken();
            GetPlaylistRequest getPlaylistRequest = spotifyApi.getPlaylist(identifier).build();
            Playlist playlist = getPlaylistRequest.execute();
            return playlist;
        }
        catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) 
        {
            return null;
        }
    }

    public Album fetchAlbumInfo(String identifier) 
    {
        try 
        {
            ensureAccessToken();
            GetAlbumRequest getAlbumRequest = spotifyApi.getAlbum(identifier).build();
            Album album = getAlbumRequest.execute();
            return album;
        }
        catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) 
        {
            return null;
        }
    }
}

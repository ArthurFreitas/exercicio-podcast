package br.ufpe.cin.if710.podcast.services;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;

/**
 * Created by Arthur on 04/10/2017.
 */

public class DownloadService extends IntentService{

    public DownloadService() {
        super("DownloadService");
    }

    public static final String DOWNLOAD_COMPLETE = "br.ufpe.cin.if710.services.action.DOWNLOAD_COMPLETE";

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            //checar se tem permissao... Android 6.0+
            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            root.mkdirs();
            File output = new File(root, intent.getData().getLastPathSegment());
            if (output.exists()) {
                output.delete();
            }
            URL url = new URL(intent.getData().toString());
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            FileOutputStream fos = new FileOutputStream(output.getPath());
            BufferedOutputStream out = new BufferedOutputStream(fos);
            try {
                InputStream in = c.getInputStream();
                byte[] buffer = new byte[8192];
                int len = 0;
                while ((len = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
            finally {
                fos.getFD().sync();
                out.close();
                c.disconnect();
            }
            //updating db

            //assumindo que eu posso usar o download link do ep como atributo único, para atualizar o bd
            String downloadLink = intent.getStringExtra("download_link");
            URI fileUri = output.toURI();
            updateDBWithFileURI(downloadLink,fileUri);

            Intent broadcastIntent = new Intent(DOWNLOAD_COMPLETE);
            broadcastIntent.putExtra("position", intent.getIntExtra("position",-1));

            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        } catch (IOException e2) {
            Log.e(getClass().getName(), "Exception durante download", e2);
        }
    }

    private void updateDBWithFileURI(String downloadLink, URI fileURI){
        ContentResolver contentResolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(PodcastProviderContract.EPISODE_URI,fileURI.toString());

        int result = contentResolver.update(
                PodcastProviderContract.EPISODE_LIST_URI,
                values,
                PodcastProviderContract.DOWNLOAD_LINK + " =?",
                new String[]{downloadLink}
        );
    }
}

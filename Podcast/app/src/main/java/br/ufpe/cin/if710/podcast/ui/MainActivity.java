package br.ufpe.cin.if710.podcast.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastProvider;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.domain.XmlFeedParser;
import br.ufpe.cin.if710.podcast.services.DownloadService;
import br.ufpe.cin.if710.podcast.ui.adapter.XmlFeedAdapter;

import static br.ufpe.cin.if710.podcast.services.DownloadService.DOWNLOAD_COMPLETE;

public class MainActivity extends Activity {

    //ao fazer envio da resolucao, use este link no seu codigo!
    private final String RSS_FEED = "http://leopoldomt.com/if710/fronteirasdaciencia.xml";
    //TODO teste com outros links de podcast

    private ContentResolver contentResolver;
    private ListView items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentResolver = getContentResolver();
        items = (ListView) findViewById(R.id.items);

        askForPermissions();

        /*items.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                XmlFeedAdapter adapter = (XmlFeedAdapter) parent.getAdapter();
                ItemFeed item = adapter.getItem(position);
                String msg = item.getTitle() + " " + item.getLink();
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this,SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    BroadcastReceiver onDownload = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra("position",-1);
            if(position == -1){
                Log.d("error","position com valor -1");
            }else{
                Button actionBtn = items.getChildAt(position).findViewById(R.id.item_action);
                actionBtn.setText("Play");
                actionBtn.setEnabled(true);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        //registra intentfilter
        IntentFilter f=new IntentFilter(DownloadService.DOWNLOAD_COMPLETE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onDownload, f);
        new DownloadXmlTask().execute(RSS_FEED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onDownload);
        XmlFeedAdapter adapter = (XmlFeedAdapter) items.getAdapter();
        adapter.clear();
    }

    //pede permissões
    private void askForPermissions(){
        //TODO: ask for permissions if android 6.0+
    }

    private class DownloadXmlTask extends AsyncTask<String, Void, List<ItemFeed>> {

        private ContentResolver contentResolver = getContentResolver();

        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "iniciando...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<ItemFeed> doInBackground(String... params) {
            List<ItemFeed> itemList = new ArrayList<>();
            try {
                itemList = XmlFeedParser.parse(getRssFeed(params[0]));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return itemList;
        }

        @Override
        protected void onPostExecute(List<ItemFeed> feed) {
            Toast.makeText(getApplicationContext(), "terminando...", Toast.LENGTH_SHORT).show();
            if(feed.size() > 0){
                // downloaded something from site
                updateDBValues(feed);
            }

            //consulta o banco e coloca os dados como source da lista
            Cursor cursor = contentResolver.query(PodcastProviderContract.EPISODE_LIST_URI,
                    PodcastProviderContract.ALL_COLUMNS, null, null, null,null);
            feed = loadValuesFromCursor(cursor);

            //Adapter Personalizado
            XmlFeedAdapter adapter = new XmlFeedAdapter(getApplicationContext(), R.layout.itemlista, feed);

            //atualizar o list view
            items.setAdapter(adapter);
            items.setTextFilterEnabled(true);
        }
    }

    //coloca valores novos no banco de dados, evita duplicatas
    private void updateDBValues(List<ItemFeed> feed){

        List<ItemFeed> valuesToBeInserted = new ArrayList<ItemFeed>();
        List<String> downloadLinks = new ArrayList<String>();//lista usada como indentificador de itens no db

        //consulta
        List<ItemFeed> DBFeed = loadValuesFromCursor(contentResolver.query(PodcastProviderContract.EPISODE_LIST_URI,
                PodcastProviderContract.ALL_COLUMNS, null, null, null,null));

        for (ItemFeed feedItem : DBFeed) {
            downloadLinks.add(feedItem.getDownloadLink());
        };

        if(downloadLinks.size() > 0){
            for (ItemFeed feedItem : feed) {
                if(!downloadLinks.contains(feedItem.getDownloadLink())){//se não ta no db
                    valuesToBeInserted.add(feedItem);
                }
            }
        }else{
            valuesToBeInserted = feed;
        }

        insertValuesIntoDB(valuesToBeInserted);
    }

    //insere os valores da lista de feed no db
    private void insertValuesIntoDB(List<ItemFeed> feed){

        if(feed.size() > 0){

            ContentValues[] contentValues = new ContentValues[feed.size()];
            for (int i = 0; i < feed.size(); i++){
                contentValues[i] = feed.get(i).toContentValues();
            }
            contentResolver.bulkInsert(PodcastProviderContract.EPISODE_LIST_URI,contentValues);

        }
    }

    //carrega valores do db, caso não haja internet
    private List<ItemFeed> loadValuesFromCursor(Cursor cursor){
        List<ItemFeed> items = new ArrayList<ItemFeed>();
        for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            items.add(new ItemFeed(cursor));
        }
        return items;
    }

    //TODO Opcional - pesquise outros meios de obter arquivos da internet
    private String getRssFeed(String feed) throws IOException {
        InputStream in = null;
        String rssFeed = "";
        try {
            URL url = new URL(feed);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int count; (count = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, count);
            }
            byte[] response = out.toByteArray();
            rssFeed = new String(response, "UTF-8");
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return rssFeed;
    }
}

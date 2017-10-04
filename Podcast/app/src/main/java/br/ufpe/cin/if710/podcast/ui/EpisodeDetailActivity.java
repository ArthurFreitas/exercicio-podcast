package br.ufpe.cin.if710.podcast.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;

public class EpisodeDetailActivity extends Activity {

    private TextView title;
    private TextView link;
    private TextView date;
    private TextView description;
    private TextView downloadLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode_detail);

        title = findViewById(R.id.ep_title);
        link = findViewById(R.id.ep_link);
        date = findViewById(R.id.ep_date);
        description = findViewById(R.id.ep_description);
        downloadLink = findViewById(R.id.ep_downloadLink);

        //seta os valores da tela de detalhos do ep com o obj passado no intent
        ItemFeed item = (ItemFeed) getIntent().getSerializableExtra("item");
        title.setText("Title: "+ item.getTitle());
        link.setText("Link: "+ item.getLink());
        date.setText("Date: "+ item.getPubDate());
        description.setText("Description: " + item.getDescription());
        downloadLink.setText("Download Link: " + item.getDownloadLink());
    }
}

package br.ufpe.cin.if710.podcast.ui.adapter;

import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.services.DownloadService;
import br.ufpe.cin.if710.podcast.ui.EpisodeDetailActivity;

public class XmlFeedAdapter extends ArrayAdapter<ItemFeed> {

    int linkResource;
    private static String PLAY = "Play";
    private static String DOWNLOAD = "Download";
    private static MediaPlayer mPlayer; //provavelmente ta errado isso...
    private ContentResolver resolver; //isso tbm...

    public XmlFeedAdapter(Context context, int resource, List<ItemFeed> objects) {
        super(context, resource, objects);
        linkResource = resource;
        resolver = getContext().getContentResolver();
    }

    /**
     * public abstract View getView (int position, View convertView, ViewGroup parent)
     * <p>
     * Added in API level 1
     * Get a View that displays the data at the specified position in the data set. You can either create a View manually or inflate it from an XML layout file. When the View is inflated, the parent View (GridView, ListView...) will apply default layout parameters unless you use inflate(int, android.view.ViewGroup, boolean) to specify a root view and to prevent attachment to the root.
     * <p>
     * Parameters
     * position	The position of the item within the adapter's data set of the item whose view we want.
     * convertView	The old view to reuse, if possible. Note: You should check that this view is non-null and of an appropriate type before using. If it is not possible to convert this view to display the correct data, this method can create a new view. Heterogeneous lists can specify their number of view types, so that this View is always of the right type (see getViewTypeCount() and getItemViewType(int)).
     * parent	The parent that this view will eventually be attached to
     * Returns
     * A View corresponding to the data at the specified position.
     */


	/*
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.itemlista, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.item_title);
		textView.setText(items.get(position).getTitle());
	    return rowView;
	}
	/**/

    //http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
    static class ViewHolder {
        TextView item_title;
        TextView item_date;
        Button download_button;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(getContext(), linkResource, null);
            holder = new ViewHolder();
            holder.item_title = (TextView) convertView.findViewById(R.id.item_title);
            holder.item_date = (TextView) convertView.findViewById(R.id.item_date);
            holder.download_button = (Button) convertView.findViewById(R.id.item_action);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.item_title.setText(getItem(position).getTitle());
        holder.item_date.setText(getItem(position).getPubDate());
        holder.download_button.setText(getButtonText(getItem(position)));

        //starta o service de download e passa informações importantes para atualizar o botão na tela quando o service terminar
        holder.download_button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View src){
                if(holder.download_button.getText().equals(DOWNLOAD)){
                    holder.download_button.setEnabled(false);
                    Context context = getContext();
                    Intent downloadService = new Intent(context, DownloadService.class);
                    downloadService.setData(Uri.parse(getItem(position).getDownloadLink()));
                    downloadService.putExtra("position",position);
                    downloadService.putExtra("download_link",getItem(position).getDownloadLink());
                    context.startService(downloadService);

                }else if(holder.download_button.getText().equals(PLAY)){
                    ItemFeed item = getItem(position);
                    if(!item.hasBeenDownloaded()) {
                        //consultar no banco atualizar o item e dar play no mplayer, assumindo que download link seja único
                        Cursor cursor = resolver.query(PodcastProviderContract.EPISODE_LIST_URI,
                                PodcastProviderContract.ALL_COLUMNS,
                                PodcastProviderContract.DOWNLOAD_LINK + "=?",
                                new String[]{item.getDownloadLink()},
                                null,
                                null
                        );
                        cursor.moveToFirst();
                        if (!cursor.isAfterLast()) {
                            item = new ItemFeed(cursor);
                        }
                    }
                    if (mPlayer != null && mPlayer.isPlaying()) {
                        mPlayer.stop();
                    }
                    mPlayer = MediaPlayer.create(getContext(),Uri.parse(item.getFile_uri()));
                    mPlayer.start();
                }
            }
        });

        //adiciona on click listener para os itens da lista
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //XmlFeedAdapter adapter = (XmlFeedAdapter) parent.getAdapter();
                ItemFeed item = getItem(position);
                Context context = getContext();
                //passa o objeto no intent
                context.startActivity(new Intent(context, EpisodeDetailActivity.class).putExtra("item",item));

                /*String msg = item.getTitle() + " " + item.getLink();
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();*/
            }
        });

        return convertView;
    }

    private String getButtonText(ItemFeed item){
        if(item.hasBeenDownloaded()){
            return PLAY;
        }
        //TODO: resume se já estava sendo tocado
        return DOWNLOAD;
    }
}
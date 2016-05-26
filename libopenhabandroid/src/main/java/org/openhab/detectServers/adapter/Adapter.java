package org.openhab.detectServers.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.openhab.R;
import org.openhab.detectServers.CheckConnectivity;
import org.openhab.detectServers.DiscoverListener;
import org.openhab.detectServers.OpenHABServer;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenHab server adapter for recyclerviews.
 */
public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> implements DiscoverListener, CheckConnectivity.OpenHABConnectivityListener, View.OnClickListener {
    List<OpenHABServer> servers = new ArrayList<>();
    Drawable drawables[] = new Drawable[]{null, null, null};
    ColorStateList colorStateLists[] = new ColorStateList[3];
    int selected = -1;

    onSelectionChange observer;

    public Adapter(Context context) {
        Resources.Theme theme = context.getTheme();
        Resources resources = context.getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            colorStateLists[0] = resources.getColorStateList(R.color.openhab_server_http, theme);
            colorStateLists[1] = resources.getColorStateList(R.color.openhab_server_https, theme);
            colorStateLists[2] = resources.getColorStateList(R.color.openhab_server_no_connection, theme);
        } else {
            colorStateLists[0] = resources.getColorStateList(R.color.openhab_server_http);
            colorStateLists[1] = resources.getColorStateList(R.color.openhab_server_https);
            colorStateLists[2] = resources.getColorStateList(R.color.openhab_server_no_connection);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawables[0] = resources.getDrawable(R.drawable.ic_lock_open_black_24dp, theme);
            drawables[1] = resources.getDrawable(R.drawable.ic_lock_outline_black_24dp, theme);
            drawables[2] = resources.getDrawable(R.drawable.ic_error_black_24dp, theme);

            assert drawables[0] != null;
            assert drawables[1] != null;
            assert drawables[2] != null;

            drawables[0].setTintList(colorStateLists[0]);
            drawables[1].setTintList(colorStateLists[1]);
            drawables[2].setTintList(colorStateLists[2]);
        } else {
            drawables[0] = resources.getDrawable(R.drawable.ic_lock_open_black_24dp);
            drawables[1] = resources.getDrawable(R.drawable.ic_lock_outline_black_24dp);
            drawables[2] = resources.getDrawable(R.drawable.ic_error_black_24dp);
        }


    }

    public void setOnSelectionChangeListener(onSelectionChange observer) {
        this.observer = observer;
    }

    @Override
    public void openhabConnectivityChanged(@NonNull OpenHABServer server, int index, boolean changed) {
        if (changed)
            notifyItemChanged(index);
    }

    public OpenHABServer getSelected() {
        if (selected == -1)
            return null;
        return servers.get(selected);
    }


    @Override
    public void onClick(View v) {
        Integer position = (Integer) v.getTag();
        if (position == null)
            return;
        int last = selected;
        selected = position;
        notifyItemChanged(position);
        if (last != selected) {
            if (last != -1) {
                notifyItemChanged(last);
            }
            notifyItemChanged(position);

            if (observer != null)
                observer.onSelectionChanged();
        }
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.openhab_server_entry, parent, false);
        return new ViewHolder(itemLayoutView, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        OpenHABServer server = servers.get(position);
        Context c = viewHolder.txtTitle.getContext();
        viewHolder.txtTitle.setText(server.getHost());
        viewHolder.clickableView.setTag(position);
        viewHolder.clickableView.setPressed(position == selected);
        viewHolder.txtSubtitle.setText(server.getDetailsString(c));
        switch (server.getConnectivity()) {
            case NotReachable:
            case ConnectionError:
            case ReachableAccessDenied:
                viewHolder.imgViewIcon.setImageDrawable(drawables[2]);
                viewHolder.imgViewIcon.setImageDrawable(drawables[2]);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    viewHolder.cardView.setBackgroundTintList(colorStateLists[2]);
//                }
                break;
            case Reachable:
                if (server.isSecure()) {
                    viewHolder.imgViewIcon.setImageDrawable(drawables[1]);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        viewHolder.cardView.setBackgroundTintList(colorStateLists[1]);
//                    }
                } else {
                    viewHolder.imgViewIcon.setImageDrawable(drawables[0]);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        viewHolder.cardView.setBackgroundTintList(colorStateLists[0]);
//                    }
                }
                break;
        }
    }

    @Override
    public void onOpenHABDiscovered(OpenHABServer server) {
        int index = -1;
        for (int i=0;i<servers.size();++i) {
            if (servers.get(i).getUrl().equals(server.getUrl())) {
                server = servers.get(i);
                index = i;
                break;
            }
        }

        if (index == -1) {
            index = servers.size();
            servers.add(server);
            notifyItemInserted(index);
        }

        server.updateConnectivity(index, this);
    }

    @Override
    public void onOpenHABDiscoveryStarted() {

    }

    @Override
    public void onOpenHABDiscoveryFinished(FinishedState state) {

    }

    // inner class to hold a reference to each item of RecyclerView
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View clickableView;
        public final View cardView;
        public final TextView txtTitle;
        public final TextView txtSubtitle;
        public final ImageView imgViewIcon;

        public ViewHolder(View itemLayoutView, View.OnClickListener checkedChange) {
            super(itemLayoutView);
            txtTitle = (TextView) itemLayoutView.findViewById(R.id.radioTitle);
            txtSubtitle = (TextView) itemLayoutView.findViewById(R.id.subtitle);
            imgViewIcon = (ImageView) itemLayoutView.findViewById(R.id.image);
            cardView = itemLayoutView.findViewById(R.id.card);
            clickableView = itemLayoutView.findViewById(R.id.clickable_view);
            clickableView.setOnClickListener(checkedChange);
        }
    }
}

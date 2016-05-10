package org.openhab_node.bootstrapopenhabnodes.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.openhab_node.bootstrapopenhabnodes.DetectActivity;
import org.openhab_node.bootstrapopenhabnodes.R;
import org.openhab_node.bootstrapopenhabnodes.openhab.OpenHABDiscoverListener;
import org.openhab_node.bootstrapopenhabnodes.openhab.OpenHABReachability;
import org.openhab_node.bootstrapopenhabnodes.openhab.OpenHABServer;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by david on 25.04.16.
 */
public class OpenHABAdapter extends RecyclerView.Adapter<OpenHABAdapter.ViewHolder> implements OpenHABDiscoverListener, OpenHABReachability.OpenHABReachabilityListener, CompoundButton.OnCheckedChangeListener {
    List<OpenHABServer> servers = new ArrayList<>();
    Drawable d[] = new Drawable[]{null,null,null};
    int selected = -1;
    boolean onBind = false;

    onSelectionChange observer;
    public void setOnSelectionChangeListener(onSelectionChange observer) {
        this.observer = observer;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OpenHABAdapter(Context context) {
        d[0] = context.getResources().getDrawable(R.drawable.ic_lock_open_black_24dp,context.getTheme());
        d[1] = context.getResources().getDrawable(R.drawable.ic_lock_outline_black_24dp,context.getTheme());
        d[2] = context.getResources().getDrawable(R.drawable.ic_error_black_24dp,context.getTheme());
    }

    @Override
    public void openhabReachable(@NonNull OpenHABServer server, int index) {
        notifyItemChanged(index);
    }

    public OpenHABServer getSelected() {
        if (selected == -1)
            return null;
        return servers.get(selected);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (onBind)
            return;
        if (!b)
            return;
        Integer position = (Integer)compoundButton.getTag();
        if (position == null)
            return;
        int last = selected;
        selected = position;
        if (last != -1) {
            notifyItemChanged(last);
        }
        notifyItemChanged(position);

        if (observer != null)
            observer.onSelectionChanged();
    }


    // inner class to hold a reference to each item of RecyclerView
    public static class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.radioTitle) public RadioButton txtTitle;
        @Bind(R.id.subtitle) public TextView txtSubtitle;
        @Bind(R.id.image) public ImageView imgViewIcon;

        public ViewHolder(View itemLayoutView, CompoundButton.OnCheckedChangeListener checkedChange) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
            txtTitle.setOnCheckedChangeListener(checkedChange);
        }
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public OpenHABAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.openhab_server_entry, parent, false);
        return new ViewHolder(itemLayoutView, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        OpenHABServer server = servers.get(position);
        Context c = viewHolder.txtTitle.getContext();
        viewHolder.txtTitle.setText(server.host);
        viewHolder.txtTitle.setTag(position);
        onBind = true;
        viewHolder.txtTitle.setChecked(position == selected);
        onBind = false;
        viewHolder.txtSubtitle.setText(server.password == null ? c.getString(R.string.openhab_no_password) :
                c.getString(R.string.openhab_password, server.password));
        if (!server.reachable)
            viewHolder.imgViewIcon.setImageDrawable(d[2]);
        else
            viewHolder.imgViewIcon.setImageDrawable(server.url.startsWith("https") ? d[1] : d[0]);
    }


    @Override
    public void onOpenHABDiscovered(OpenHABServer server) {
        for (int i=0;i<servers.size();++i) {
            if (servers.get(i).url.equals(server.url)) {
                if (!servers.get(i).reachable)
                    new OpenHABReachability(servers.get(i), i, this);
                return;
            }
        }

        servers.add(server);
        notifyItemInserted(servers.size()-1);
        new OpenHABReachability(server, servers.size()-1, this);
    }

    @Override
    public void onOpenHABDiscoveryStarted() {

    }

    @Override
    public void onOpenHABDiscoveryFinished(FinishedState state) {

    }
}

package kore.botssdk.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.DrawableImageViewTarget;

import java.util.ArrayList;

import kore.botssdk.R;
import kore.botssdk.listener.InvokeGenericWebViewInterface;
import kore.botssdk.models.LiveSearchResultsModel;
import kore.botssdk.utils.BundleConstants;
import kore.botssdk.utils.StringUtils;
import kore.botssdk.utils.markdown.MarkdownUtil;

public class LiveSearchCyclerAdapter extends RecyclerView.Adapter<LiveSearchCyclerAdapter.ViewHolder> {
    private final ArrayList<LiveSearchResultsModel> model;
    private final Context context;
    private final InvokeGenericWebViewInterface invokeGenericWebViewInterface;

    public LiveSearchCyclerAdapter(Context context, ArrayList<LiveSearchResultsModel> model, int from, InvokeGenericWebViewInterface invokeGenericWebViewInterface) {
        this.model = model;
        this.context = context;
        this.invokeGenericWebViewInterface = invokeGenericWebViewInterface;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.live_search_pages_findly_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final LiveSearchResultsModel liveSearchResultsModel = model.get(position);

        holder.ivPagesCell.setVisibility(View.GONE);
        holder.ivSuggestedPage.setVisibility(View.GONE);

        holder.llPages.setVisibility(VISIBLE);
        holder.llTask.setVisibility(GONE);

        if (liveSearchResultsModel.getQuestion() != null)
        {
            holder.tvTitle.setMaxLines(1);
            holder.tvTitle.setText(liveSearchResultsModel.getQuestion());
            holder.tvDescription.setText(liveSearchResultsModel.getAnswer());
            holder.tvFullDescription.setText(liveSearchResultsModel.getAnswer());
        }
        else if (liveSearchResultsModel.getPage_title() != null) {
            holder.tvTitle.setMaxLines(2);
            holder.ivPagesCell.setVisibility(View.VISIBLE);
            holder.tvTitle.setText(Html.fromHtml(MarkdownUtil.processMarkDown(liveSearchResultsModel.getPage_title())));
            holder.tvDescription.setText(Html.fromHtml(MarkdownUtil.processMarkDown(liveSearchResultsModel.getPage_preview())));

            if (!StringUtils.isNullOrEmpty(liveSearchResultsModel.getPage_image_url()))
                Glide.with(context).load(liveSearchResultsModel.getPage_image_url())
                        .error(R.drawable.ic_launcher)
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)).into(new DrawableImageViewTarget(holder.ivPagesCell));
        }


        holder.tvTitle.setTag(true);
        holder.tvTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (liveSearchResultsModel. getSys_content_type().equalsIgnoreCase(BundleConstants.FAQ)) {
                    if ((boolean) view.getTag()) {
                        holder.tvDescription.setVisibility(View.GONE);
                        holder.tvFullDescription.setVisibility(View.VISIBLE);
                        view.setTag(false);
                    } else {
                        holder.tvDescription.setVisibility(View.VISIBLE);
                        holder.tvFullDescription.setVisibility(View.GONE);
                        view.setTag(true);
                    }
                }
            }
        });

        holder.llPages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(invokeGenericWebViewInterface != null)
                {
                    if(!StringUtils.isNullOrEmpty(liveSearchResultsModel.getUrl()))
                        invokeGenericWebViewInterface.invokeGenericWebView(liveSearchResultsModel.getUrl());
                    else if(!StringUtils.isNullOrEmpty(liveSearchResultsModel.getPage_url()))
                        invokeGenericWebViewInterface.invokeGenericWebView(liveSearchResultsModel.getPage_url());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return model.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvFullDescription, tvPageTitle, tvTaskName;
        ImageView ivPagesCell, ivSuggestedPage, ivTaskCell;
        LinearLayout llPages, llTask;

        public ViewHolder(View itemView) {
            super(itemView);
            this.ivPagesCell = (ImageView) itemView.findViewById(R.id.ivPagesCell);
            this.tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
            this.tvDescription = (TextView) itemView.findViewById(R.id.tvDescription);
            this.tvFullDescription = (TextView) itemView.findViewById(R.id.tvFullDescription);
            this.tvPageTitle = (TextView) itemView.findViewById(R.id.tvPageTitle);
            this.ivSuggestedPage = (ImageView) itemView.findViewById(R.id.ivSuggestedPage);
            this.llPages = (LinearLayout) itemView.findViewById(R.id.llPages);
            this.ivTaskCell = (ImageView) itemView.findViewById(R.id.ivTaskCell);
            this.tvTaskName = (TextView) itemView.findViewById(R.id.tvTaskName);
            this.llTask = (LinearLayout) itemView.findViewById(R.id.llTask);
        }
    }
}

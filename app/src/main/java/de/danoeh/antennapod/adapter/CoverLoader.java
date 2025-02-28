package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;

import java.lang.ref.WeakReference;

import com.bumptech.glide.request.transition.Transition;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.glide.ApGlideSettings;

public class CoverLoader {
    private String uri;
    private String fallbackUri;
    private TextView txtvPlaceholder;
    private ImageView imgvCover;
    private MainActivity mainActivity;
    private int errorResource = -1;

    public CoverLoader(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public CoverLoader withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public CoverLoader withFallbackUri(String uri) {
        fallbackUri = uri;
        return this;
    }

    public CoverLoader withCoverView(ImageView coverView) {
        imgvCover = coverView;
        return this;
    }

    public CoverLoader withError(int errorResource) {
        this.errorResource = errorResource;
        return this;
    }

    public CoverLoader withPlaceholderView(TextView placeholderView) {
        txtvPlaceholder = placeholderView;
        return this;
    }

    public void load() {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate();

        if (errorResource != -1) {
            options = options.error(errorResource);
        }

        RequestBuilder<Drawable> builder = Glide.with(mainActivity)
                .load(uri)
                .apply(options);

        if (fallbackUri != null && txtvPlaceholder != null && imgvCover != null) {
            builder = builder.error(Glide.with(mainActivity)
                    .load(fallbackUri)
                    .apply(options));
        }

        builder.into(new CoverTarget(txtvPlaceholder, imgvCover));
    }

    static class CoverTarget extends CustomViewTarget<ImageView, Drawable> {
        private final WeakReference<TextView> placeholder;
        private final WeakReference<ImageView> cover;

        public CoverTarget(TextView txtvPlaceholder, ImageView imgvCover) {
            super(imgvCover);
            placeholder = new WeakReference<>(txtvPlaceholder);
            cover = new WeakReference<>(imgvCover);
        }

        @Override
        public void onLoadFailed(Drawable errorDrawable) {

        }

        @Override
        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
            TextView txtvPlaceholder = placeholder.get();
            if (txtvPlaceholder != null) {
                txtvPlaceholder.setVisibility(View.INVISIBLE);
            }
            ImageView ivCover = cover.get();
            ivCover.setImageDrawable(resource);
        }

        @Override
        protected void onResourceCleared(@Nullable Drawable placeholder) {
            ImageView ivCover = cover.get();
            ivCover.setImageDrawable(placeholder);
        }
    }
}
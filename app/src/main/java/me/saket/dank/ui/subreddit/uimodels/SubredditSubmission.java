package me.saket.dank.ui.subreddit.uimodels;

import android.support.annotation.CheckResult;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.List;
import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.SpannableWithTextEquality;
import me.saket.dank.data.SwipeEvent;
import me.saket.dank.ui.subreddit.SubmissionSwipeActionsProvider;
import me.saket.dank.ui.subreddit.events.SubredditSubmissionClickEvent;
import me.saket.dank.ui.subreddit.events.SubredditSubmissionThumbnailClickEvent;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.glide.GlideCircularTransformation;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeDirection;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public interface SubredditSubmission {

  enum PartialChange {
    TITLE,
    BYLINE,
    THUMBNAIL,
    SAVE_STATUS,
    SWIPE_ACTIONS
  }

  @AutoValue
  abstract class UiModel implements SubredditScreenUiModel.SubmissionRowUiModel {
    @Override
    public Type type() {
      return Type.SUBMISSION;
    }

    @Override
    public abstract long adapterId();

    public abstract Optional<UiModel.Thumbnail> thumbnail();

    public abstract boolean isThumbnailClickable();

    public abstract SpannableWithTextEquality title();

    public abstract SpannableWithTextEquality byline();

    public abstract Optional<Integer> backgroundDrawableRes();

    public abstract Submission submission();

    public abstract boolean isSaved();

    public abstract SwipeActions swipeActions();

    public static Builder builder() {
      return new AutoValue_SubredditSubmission_UiModel.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder adapterId(long id);

      public abstract Builder thumbnail(Optional<UiModel.Thumbnail> thumbnail);

      public abstract Builder isThumbnailClickable(boolean clickable);

      abstract Builder title(SpannableWithTextEquality title);

      abstract Builder byline(SpannableWithTextEquality byline);

      public Builder title(CharSequence title, Pair<Integer, VoteDirection> votes) {
        return title(SpannableWithTextEquality.wrap(title, votes));
      }

      public Builder byline(CharSequence byline, Integer commentsCount) {
        return byline(SpannableWithTextEquality.wrap(byline, commentsCount));
      }

      public abstract Builder backgroundDrawableRes(Optional<Integer> backgroundRes);

      public abstract Builder submission(Submission submission);

      public abstract Builder isSaved(boolean isSaved);

      public abstract Builder swipeActions(SwipeActions swipeActions);

      public abstract UiModel build();
    }

    @AutoValue
    public abstract static class Thumbnail {
      public abstract Optional<Integer> staticRes();

      public abstract Optional<String> remoteUrl();

      public abstract Optional<Integer> backgroundRes();

      public abstract ImageView.ScaleType scaleType();

      public abstract Optional<Integer> tintColor();

      public abstract String contentDescription();

      public static Builder builder() {
        return new AutoValue_SubredditSubmission_UiModel_Thumbnail.Builder();
      }

      @AutoValue.Builder
      public abstract static class Builder {
        public abstract Builder staticRes(Optional<Integer> resId);

        public abstract Builder remoteUrl(Optional<String> url);

        public abstract Builder backgroundRes(Optional<Integer> backgroundRes);

        public abstract Builder scaleType(ImageView.ScaleType scaleType);

        public abstract Builder tintColor(Optional<Integer> tintColor);

        public abstract Builder contentDescription(String description);

        public abstract Thumbnail build();
      }
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    private final ImageView thumbnailView;
    private final TextView titleView;
    private final TextView bylineView;
    private UiModel uiModel;

    protected ViewHolder(View itemView) {
      super(itemView);
      thumbnailView = itemView.findViewById(R.id.submission_item_icon);
      titleView = itemView.findViewById(R.id.submission_item_title);
      bylineView = itemView.findViewById(R.id.submission_item_byline);
    }

    public void setUiModel(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      titleView.setText(uiModel.title());
      bylineView.setText(uiModel.byline());

      Glide.with(thumbnailView).clear(thumbnailView);
      setThumbnail(uiModel.thumbnail());

      if (uiModel.backgroundDrawableRes().isPresent()) {
        itemView.setBackgroundResource(uiModel.backgroundDrawableRes().get());
      } else {
        itemView.setBackground(null);
      }
    }

    public void renderPartialChanges(List<Object> payloads) {
      for (Object payload : payloads) {
        //noinspection unchecked
        for (PartialChange partialChange : (List<PartialChange>) payload) {
          switch (partialChange) {
            case TITLE:
              titleView.setText(uiModel.title());
              break;

            case BYLINE:
              bylineView.setText(uiModel.byline());
              break;

            case THUMBNAIL:
              setThumbnail(uiModel.thumbnail());
              break;

            case SAVE_STATUS:
            case SWIPE_ACTIONS:
              getSwipeableLayout().setSwipeActions(uiModel.swipeActions());
              break;

            default:
              throw new UnsupportedOperationException("Unknown partial change: " + partialChange);
          }
        }
      }
    }

    private void setThumbnail(Optional<UiModel.Thumbnail> optionalThumbnail) {
      thumbnailView.setVisibility(uiModel.thumbnail().isPresent() ? View.VISIBLE : View.GONE);

      optionalThumbnail.ifPresent(thumb -> {
        thumbnailView.setBackgroundResource(thumb.backgroundRes().orElse(0));
        thumbnailView.setScaleType(thumb.scaleType());
        thumbnailView.setContentDescription(thumb.contentDescription());

        if (thumb.tintColor().isPresent()) {
          thumbnailView.setColorFilter(thumb.tintColor().get());
        } else {
          thumbnailView.setColorFilter(null);
        }

        if (thumb.staticRes().isPresent()) {
          thumbnailView.setImageResource(thumb.staticRes().get());
        } else {
          Glide.with(itemView)
              .load(thumb.remoteUrl().get())
              .apply(RequestOptions.bitmapTransform(GlideCircularTransformation.INSTANCE))
              .transition(DrawableTransitionOptions.withCrossFade())
              .into(thumbnailView);
        }
      });
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }

  class Adapter implements SubredditScreenUiModel.SubmissionRowUiChildAdapter<UiModel, ViewHolder> {
    private final PublishRelay<SubredditSubmissionClickEvent> submissionClicks = PublishRelay.create();
    private final PublishRelay<SubredditSubmissionThumbnailClickEvent> thumbnailClicks = PublishRelay.create();
    private final SubmissionSwipeActionsProvider swipeActionsProvider;

    @Inject
    public Adapter(SubmissionSwipeActionsProvider swipeActionsProvider) {
      this.swipeActionsProvider = swipeActionsProvider;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = new ViewHolder(inflater.inflate(itemLayoutRes(), parent, false));
      holder.itemView.setOnClickListener(o ->
          submissionClicks.accept(SubredditSubmissionClickEvent.create(holder.uiModel.submission(), holder.itemView, holder.getItemId()))
      );
      holder.thumbnailView.setOnClickListener(o -> {
        if (holder.uiModel.isThumbnailClickable()) {
          thumbnailClicks.accept(SubredditSubmissionThumbnailClickEvent.create(holder.uiModel.submission(), holder.itemView, holder.thumbnailView));
        } else {
          holder.itemView.performClick();
        }
      });

      SwipeableLayout swipeableLayout = holder.getSwipeableLayout();
      swipeableLayout.setSwipeActionIconProvider((imageView, oldAction, newAction) ->
          swipeActionsProvider.showSwipeActionIcon(imageView, oldAction, newAction, holder.uiModel.submission())
      );
      swipeableLayout.setOnPerformSwipeActionListener((action, swipeDirection) ->
          swipeActionsProvider.performSwipeAction(action, holder.uiModel.submission(), swipeableLayout, swipeDirection)
      );
      return holder;
    }

    @LayoutRes
    protected int itemLayoutRes() {
      return R.layout.list_item_submission;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.setUiModel(uiModel);
      holder.getSwipeableLayout().setSwipeActions(uiModel.swipeActions());
      holder.render();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      holder.setUiModel(uiModel);
      holder.renderPartialChanges(payloads);
    }

    @CheckResult
    public Observable<SubredditSubmissionClickEvent> submissionClicks() {
      return submissionClicks;
    }

    @CheckResult
    public Observable<SubredditSubmissionThumbnailClickEvent> thumbnailClicks() {
      return thumbnailClicks;
    }

    @CheckResult
    public Observable<SwipeEvent> swipeEvents() {
      return swipeActionsProvider.getSwipeEvents();
    }
  }
}

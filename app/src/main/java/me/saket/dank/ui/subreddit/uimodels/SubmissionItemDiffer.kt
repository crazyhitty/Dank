package me.saket.dank.ui.subreddit.uimodels

import android.support.v7.util.DiffUtil
import me.saket.dank.ui.subreddit.uimodels.SubredditScreenUiModel.SubmissionRowUiModel
import java.util.*

object SubmissionItemDiffer : DiffUtil.ItemCallback<SubmissionRowUiModel>() {

  override fun areItemsTheSame(oldItem: SubmissionRowUiModel, newItem: SubmissionRowUiModel): Boolean {
    return oldItem.adapterId() == newItem.adapterId()
  }

  override fun areContentsTheSame(oldItem: SubmissionRowUiModel, newItem: SubmissionRowUiModel): Boolean {
    return oldItem == newItem
  }

  override fun getChangePayload(oldItem: SubmissionRowUiModel, newItem: SubmissionRowUiModel): Any? {
    return when (oldItem.type()) {
      SubmissionRowUiModel.Type.SUBMISSION -> {
        val oldSubmission = oldItem as SubredditSubmission.UiModel
        val newSubmission = newItem as SubredditSubmission.UiModel

        val partialChanges = mutableListOf<SubredditSubmission.PartialChange>()
        if (oldSubmission.title() != newSubmission.title()) {
          partialChanges.add(SubredditSubmission.PartialChange.TITLE)
        }
        if (oldSubmission.byline() != newSubmission.byline()) {
          partialChanges.add(SubredditSubmission.PartialChange.BYLINE)
        }
        if (oldSubmission.thumbnail() != newSubmission.thumbnail()) {
          partialChanges.add(SubredditSubmission.PartialChange.THUMBNAIL)
        }
        if (oldSubmission.isSaved != newSubmission.isSaved) {
          partialChanges.add(SubredditSubmission.PartialChange.SAVE_STATUS)
        }
        if (oldSubmission.swipeActions() != newSubmission.swipeActions()) {
          partialChanges.add(SubredditSubmission.PartialChange.SWIPE_ACTIONS)
        }
        partialChanges
      }

      SubmissionRowUiModel.Type.PAGINATION_FOOTER -> {
        super.getChangePayload(oldItem, newItem)
      }

      else -> throw AssertionError()
    }
  }
}

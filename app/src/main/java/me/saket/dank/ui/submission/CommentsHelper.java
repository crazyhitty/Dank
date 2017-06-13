package me.saket.dank.ui.submission;


import com.jakewharton.rxrelay2.PublishRelay;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.saket.dank.utils.SafeConsumer;

/**
 * Helps in flattening a comments tree with collapsed child comments ignored.
 */
public class CommentsHelper {

  // Comments that are collapsed.
  private Set<String> collapsedCommentNodeIds = new HashSet<>();

  // Comments for which more replies are being fetched.
  private Set<String> loadingMoreCommentNodeIds = new HashSet<>();

  private CommentNode rootCommentNode;

  private PublishRelay<List<SubmissionCommentsRow>> commentUpdates = PublishRelay.create();

  public Observable<List<SubmissionCommentsRow>> updates() {
    return commentUpdates;
  }

  /**
   * Set the root comment of a submission.
   */
  public SafeConsumer<Submission> setup() {
    return submission -> {
      rootCommentNode = submission.getComments();
      commentUpdates.accept(constructComments());
    };
  }

  public void reset() {
    rootCommentNode = null;
    collapsedCommentNodeIds.clear();
  }

  public Consumer<CommentNode> toggleCollapse() {
    return commentNode -> {
      if (isCollapsed(commentNode)) {
        collapsedCommentNodeIds.remove(commentNode.getComment().getId());
      } else {
        collapsedCommentNodeIds.add(commentNode.getComment().getId());
      }
      commentUpdates.accept(constructComments());
    };
  }

  private boolean isCollapsed(CommentNode commentNode) {
    return collapsedCommentNodeIds.contains(commentNode.getComment().getId());
  }

  public Consumer<CommentNode> setMoreCommentsLoading(boolean loading) {
    return commentNode -> {
      if (loading) {
        loadingMoreCommentNodeIds.add(commentNode.getComment().getId());
      } else {
        loadingMoreCommentNodeIds.remove(commentNode.getComment().getId());
      }
      commentUpdates.accept(constructComments());
    };
  }

  public boolean areMoreCommentsLoadingFor(CommentNode commentNode) {
    return loadingMoreCommentNodeIds.contains(commentNode.getComment().getId());
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionCommentsRow> constructComments() {
    return constructComments(new ArrayList<>(rootCommentNode.getTotalSize()), rootCommentNode);
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionCommentsRow> constructComments(List<SubmissionCommentsRow> flattenComments, CommentNode nextNode) {
//    String indentation = "";
//    if (nextNode.getDepth() != 0) {
//      for (int step = 0; step < nextNode.getDepth(); step++) {
//        indentation += "  ";
//      }
//    }

    boolean isCommentNodeCollapsed = isCollapsed(nextNode);
    if (nextNode.getDepth() != 0) {
//            Timber.i("%s(%s) %s: %s", indentation, nextNode.getComment().getId(), nextNode.getComment().getAuthor(), nextNode.getComment().getBody());
      flattenComments.add(DankCommentNode.create(nextNode, isCommentNodeCollapsed));
    }

    if (nextNode.isEmpty() && !nextNode.hasMoreComments()) {
      return flattenComments;

    } else {
      // Ignore collapsed children.
      if (!isCommentNodeCollapsed) {
        List<CommentNode> childCommentsTree = nextNode.getChildren();
        for (CommentNode node : childCommentsTree) {
          constructComments(flattenComments, node);
        }

        if (nextNode.hasMoreComments()) {
//                    Timber.d("%s(%s) %s has %d MORE ---------->",
//                            indentation, nextNode.getComment().getId(), nextNode.getComment().getAuthor(), nextNode.getMoreChildren().getCount()
//                    );
//                    Timber.d("%s %s", indentation, nextNode.getMoreChildren().getChildrenIds());
          flattenComments.add(LoadMoreCommentsItem.create(nextNode, areMoreCommentsLoadingFor(nextNode)));
        }
      }
      return flattenComments;
    }
  }

}

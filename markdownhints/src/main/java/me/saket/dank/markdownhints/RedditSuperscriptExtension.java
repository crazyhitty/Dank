package me.saket.dank.markdownhints;

import com.vladsch.flexmark.ast.DelimitedNode;
import com.vladsch.flexmark.internal.Delimiter;
import com.vladsch.flexmark.internal.inline.AsteriskDelimiterProcessor;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.Superscript;
import com.vladsch.flexmark.superscript.internal.SuperscriptDelimiterProcessor;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

/**
 * Doesn't work :/
 */
public class RedditSuperscriptExtension implements Parser.ParserExtension {

  public static RedditSuperscriptExtension create() {
    return new RedditSuperscriptExtension();
  }

  @Override
  public void parserOptions(MutableDataHolder options) {}

  @Override
  public void extend(Parser.Builder parserBuilder) {
    parserBuilder.customDelimiterProcessor(new P());
  }

  class P extends AsteriskDelimiterProcessor {

    public P() {
      super(false);
    }

    @Override
    public char getOpeningCharacter() {
      return '^';
    }

    @Override
    public char getClosingCharacter() {
      return ' ';
    }

    @Override
    public void process(Delimiter opener, Delimiter closer, int delimitersUsed) {
      DelimitedNode emphasis = delimitersUsed == 1
          ? new Superscript(opener.getTailChars(delimitersUsed), BasedSequence.NULL, closer.getLeadChars(delimitersUsed))
          : new Superscript(opener.getTailChars(delimitersUsed), BasedSequence.NULL, closer.getLeadChars(delimitersUsed));

      opener.moveNodesBetweenDelimitersTo(emphasis, closer);
    }
  }

  public class RedditSuperscriptDelimiterProcessor extends SuperscriptDelimiterProcessor {
    @Override
    public char getClosingCharacter() {
      return ' ';
    }

    @Override
    public boolean canBeOpener(boolean leftFlanking, boolean rightFlanking, boolean beforeIsPunctuation, boolean afterIsPunctuation,
        boolean beforeIsWhitespace, boolean afterIsWhiteSpace)
    {
      // No idea what this does.
      return leftFlanking;
    }

    @Override
    public boolean canBeCloser(boolean leftFlanking, boolean rightFlanking, boolean beforeIsPunctuation, boolean afterIsPunctuation,
        boolean beforeIsWhitespace, boolean afterIsWhiteSpace)
    {
      //Timber.i(
      //    "canBeCloser() -> leftFlanking: %s, rightFlanking: %s, beforeIsPunctuation: %s, afterIsPunctuation: %s, beforeIsWhitespace: %s, afterIsWhiteSpace: %s",
      //    leftFlanking, rightFlanking, beforeIsPunctuation, afterIsPunctuation, beforeIsWhitespace, afterIsWhiteSpace
      //);
      // No idea what this does.
      return rightFlanking;
    }
  }
}

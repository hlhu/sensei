package com.senseidb.search.query;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;

import com.browseengine.bobo.api.BoboIndexReader;

public class ScoreAugmentQuery extends AbstractScoreAdjuster
{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  public static interface ScoreAugmentFunction{
    public void initialize(BoboIndexReader reader) throws IOException;
    public float newScore(float rawScore, int docID);
    public String getExplainString();
  }
  
  private static class AugmentScorer extends Scorer{
    private final ScoreAugmentFunction _func;
    private final Scorer _innerScorer;

    protected AugmentScorer(BoboIndexReader reader,Scorer innerScorer,ScoreAugmentFunction func) throws IOException
    {
      super(innerScorer.getSimilarity());
      _innerScorer = innerScorer;
      _func = func;
      _func.initialize(reader);
    }

    @Override
    public float score()
        throws IOException
    {
      float rawScore = _innerScorer.score();
      return _func.newScore(rawScore, _innerScorer.docID());
    }

    @Override
    public int advance(int target)
        throws IOException
    {
      return _innerScorer.advance(target);
    }

    @Override
    public int docID()
    {
      return _innerScorer.docID();
    }

    @Override
    public int nextDoc()
        throws IOException
    {
      return _innerScorer.nextDoc();
    }
    
  }
  
  private transient ScoreAugmentFunction _func;

  public ScoreAugmentQuery(Query query,ScoreAugmentFunction func)
  {
    super(query);
    _func = func;
    if (_func == null) throw new IllegalArgumentException("augment function cannot be null");
  }

  @Override
  protected Scorer createScorer(Scorer innerScorer,
                                IndexReader reader,
                                boolean scoreDocsInOrder,
                                boolean topScorer)
      throws IOException
  {
    if (reader instanceof BoboIndexReader){
      return new AugmentScorer((BoboIndexReader)reader,innerScorer,_func);
    }
    else{
      throw new IllegalStateException("reader not instance of "+BoboIndexReader.class);
    }
  }
  
  @Override
  protected Explanation createExplain(Explanation innerExplain,
                                      IndexReader reader,
                                      int doc) throws IOException
  {
    if (reader instanceof BoboIndexReader ){
      Explanation finalExpl = new Explanation();
      finalExpl.addDetail(innerExplain);
      
      float innerValue = innerExplain.getValue();
      float value = _func.newScore(innerValue, doc);
      finalExpl.setValue(value);
      finalExpl.setDescription("Custom score: "+ _func.getExplainString() );
      return finalExpl;
    }
    else{
      throw new IllegalStateException("reader not instance of "+BoboIndexReader.class);
    }
  }

}

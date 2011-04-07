package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborSimilarityFunction with a pattern defined to accept
 * Strings that define a non-negative Integer.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has SharedNearestNeighborsPreprocessor
 * @apiviz.uses Instance oneway - - «create»
 * 
 * @param <O> object type
 * @param <D> distance type
 */
// todo arthur comment class
public class FractionalSharedNearestNeighborSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractIndexBasedSimilarityFunction<O, SharedNearestNeighborIndex<O>, TreeSetDBIDs, DoubleDistance> implements NormalizedSimilarityFunction<O, DoubleDistance> {
  /**
   * Constructor.
   * 
   * @param indexFactory Index factory.
   */
  public FractionalSharedNearestNeighborSimilarityFunction(IndexFactory<O, SharedNearestNeighborIndex<O>> indexFactory) {
    super(indexFactory);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database) {
    SharedNearestNeighborIndex<O> indexi = indexFactory.instantiate((Database<O>) database);
    return (Instance<T, D>) new Instance<O, D>((Database<O>) database, indexi, indexi.getNumberOfNeighbors());
  }

  /**
   * Actual instance for a dataset.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.uses SharedNearestNeighborIndex
   * 
   * @param <O>
   * @param <D>
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractIndexBasedSimilarityFunction.Instance<O, SharedNearestNeighborIndex<O>, TreeSetDBIDs, DoubleDistance> {
    /**
     * Holds the number of nearest neighbors to be used.
     */
    private final int numberOfNeighbors;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     * @param numberOfNeighbors Neighborhood size
     */
    public Instance(Database<O> database, SharedNearestNeighborIndex<O> preprocessor, int numberOfNeighbors) {
      super(database, preprocessor);
      this.numberOfNeighbors = numberOfNeighbors;
    }

    static protected int countSharedNeighbors(TreeSetDBIDs neighbors1, TreeSetDBIDs neighbors2) {
      int intersection = 0;
      Iterator<DBID> iter1 = neighbors1.iterator();
      Iterator<DBID> iter2 = neighbors2.iterator();
      DBID neighbors1ID = null;
      DBID neighbors2ID = null;
      if(iter1.hasNext()) {
        neighbors1ID = iter1.next();
      }
      if(iter2.hasNext()) {
        neighbors2ID = iter2.next();
      }
      while((iter1.hasNext() || iter2.hasNext()) && neighbors1ID != null && neighbors2ID != null) {
        if(neighbors1ID.equals(neighbors2ID)) {
          intersection++;
          if(iter1.hasNext()) {
            neighbors1ID = iter1.next();
          }
          else {
            neighbors1ID = null;
          }
          if(iter2.hasNext()) {
            neighbors2ID = iter2.next();
          }
          else {
            neighbors2ID = null;
          }
        }
        else if(neighbors2ID.compareTo(neighbors1ID) > 0) {
          if(iter1.hasNext()) {
            neighbors1ID = iter1.next();
          }
          else {
            neighbors1ID = null;
          }
        }
        else // neighbors1ID > neighbors2ID
        {
          if(iter2.hasNext()) {
            neighbors2ID = iter2.next();
          }
          else {
            neighbors2ID = null;
          }
        }
      }
      return intersection;
    }

    @Override
    public DoubleDistance similarity(DBID id1, DBID id2) {
      TreeSetDBIDs neighbors1 = index.getNearestNeighborSet(id1);
      TreeSetDBIDs neighbors2 = index.getNearestNeighborSet(id2);
      int intersection = countSharedNeighbors(neighbors1, neighbors2);
      return new DoubleDistance((double) intersection / numberOfNeighbors);
    }

    @Override
    public DoubleDistance getDistanceFactory() {
      return DoubleDistance.FACTORY;
    }
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject, D extends Distance<D>> extends AbstractIndexBasedSimilarityFunction.Parameterizer<SharedNearestNeighborIndex.Factory<O, SharedNearestNeighborIndex<O>>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configIndexFactory(config, SharedNearestNeighborIndex.Factory.class, SharedNearestNeighborPreprocessor.Factory.class);
    }

    @Override
    protected FractionalSharedNearestNeighborSimilarityFunction<O, D> makeInstance() {
      return new FractionalSharedNearestNeighborSimilarityFunction<O, D>(factory);
    }
  }
}
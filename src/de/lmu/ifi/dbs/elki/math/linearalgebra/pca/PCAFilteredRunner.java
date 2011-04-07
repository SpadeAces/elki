package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * PCA runner that will do dimensionality reduction. PCA is computed as with the
 * regular runner, but afterwards, an {@link EigenPairFilter} is applied.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses PCAFilteredResult oneway - - «create»
 * @apiviz.composedOf EigenPairFilter
 * 
 * @param <V> Vector class to use
 * @param <D> Distance type
 */
public class PCAFilteredRunner<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends PCARunner<V, D> {
  /**
   * Parameter to specify the filter for determination of the strong and weak
   * eigenvectors, must be a subclass of {@link EigenPairFilter}.
   * <p/>
   * Default value: {@link PercentageEigenPairFilter}
   * </p>
   * <p/>
   * Key: {@code -pca.filter}
   * </p>
   */
  public static final OptionID PCA_EIGENPAIR_FILTER = OptionID.getOrCreateOptionID("pca.filter", "Filter class to determine the strong and weak eigenvectors.");

  /**
   * Parameter to specify a constant big value to reset high eigenvalues, must
   * be a double greater than 0.
   * <p>
   * Default value: {@code 1.0}
   * </p>
   * <p>
   * Key: {@code -pca.big}
   * </p>
   */
  public static final OptionID BIG_ID = OptionID.getOrCreateOptionID("pca.big", "A constant big value to reset high eigenvalues.");

  /**
   * Parameter to specify a constant small value to reset low eigenvalues, must
   * be a double greater than 0.
   * <p>
   * Default value: {@code 0.0}
   * </p>
   * <p>
   * Key: {@code -pca.small}
   * </p>
   */
  public static final OptionID SMALL_ID = OptionID.getOrCreateOptionID("pca.small", "A constant small value to reset low eigenvalues.");

  /**
   * Holds the instance of the EigenPairFilter specified by
   * {@link #PCA_EIGENPAIR_FILTER}.
   */
  private EigenPairFilter eigenPairFilter;

  /**
   * Holds the value of {@link #BIG_ID}.
   */
  private double big;

  /**
   * Holds the value of {@link #SMALL_ID}.
   */
  private double small;

  /**
   * Constructor.
   * 
   * @param covarianceMatrixBuilder
   * @param eigenPairFilter
   * @param big
   * @param small
   */
  public PCAFilteredRunner(CovarianceMatrixBuilder<V, D> covarianceMatrixBuilder, EigenPairFilter eigenPairFilter, double big, double small) {
    super(covarianceMatrixBuilder);
    this.eigenPairFilter = eigenPairFilter;
    this.big = big;
    this.small = small;
  }

  /**
   * Run PCA on a collection of database IDs
   * 
   * @param ids a collection of ids
   * @param database the database used
   * @return PCA result
   */
  @Override
  public PCAFilteredResult processIds(DBIDs ids, Database<? extends V> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processIds(ids, database));
  }

  /**
   * Run PCA on a QueryResult Collection
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return PCA result
   */
  @Override
  public PCAFilteredResult processQueryResult(Collection<DistanceResultPair<D>> results, Database<? extends V> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processQueryResults(results, database));
  }

  /**
   * Process an existing Covariance Matrix
   * 
   * @param covarMatrix the matrix used for performing PCA
   */
  @Override
  public PCAFilteredResult processCovarMatrix(Matrix covarMatrix) {
    // TODO: add support for a different implementation to do EVD?
    EigenvalueDecomposition evd = covarMatrix.eig();
    return processEVD(evd);
  }

  /**
   * Process an existing eigenvalue decomposition
   * 
   * @param evd eigenvalue decomposition to use
   */
  @Override
  public PCAFilteredResult processEVD(EigenvalueDecomposition evd) {
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    FilteredEigenPairs filteredEigenPairs = eigenPairFilter.filter(eigenPairs);
    return new PCAFilteredResult(eigenPairs, filteredEigenPairs, big, small);
  }

  /**
   * Retrieve the {@link EigenPairFilter} to be used. For derived PCA Runners
   * 
   * @return eigenpair filter configured.
   */
  protected EigenPairFilter getEigenPairFilter() {
    return eigenPairFilter;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends PCARunner.Parameterizer<V, D> {
    /**
     * Holds the instance of the EigenPairFilter specified by
     * {@link #PCA_EIGENPAIR_FILTER}.
     */
    protected EigenPairFilter eigenPairFilter;

    /**
     * Holds the value of {@link #BIG_ID}.
     */
    protected double big;

    /**
     * Holds the value of {@link #SMALL_ID}.
     */
    protected double small;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<EigenPairFilter> EIGENPAIR_FILTER_PARAM = new ObjectParameter<EigenPairFilter>(PCA_EIGENPAIR_FILTER, EigenPairFilter.class, PercentageEigenPairFilter.class);
      if(config.grab(EIGENPAIR_FILTER_PARAM)) {
        eigenPairFilter = EIGENPAIR_FILTER_PARAM.instantiateClass(config);
      }

      DoubleParameter BIG_PARAM = new DoubleParameter(BIG_ID, new GreaterConstraint(0), 1.0);
      if(config.grab(BIG_PARAM)) {
        big = BIG_PARAM.getValue();

      }

      DoubleParameter SMALL_PARAM = new DoubleParameter(SMALL_ID, new GreaterEqualConstraint(0), 0.0);
      if(config.grab(SMALL_PARAM)) {
        small = SMALL_PARAM.getValue();
      }

      // global constraint small <--> big
      config.checkConstraint(new LessGlobalConstraint<Double>(SMALL_PARAM, BIG_PARAM));
    }

    @Override
    protected PCAFilteredRunner<V, D> makeInstance() {
      return new PCAFilteredRunner<V, D>(covarianceMatrixBuilder, eigenPairFilter, big, small);
    }
  }
}
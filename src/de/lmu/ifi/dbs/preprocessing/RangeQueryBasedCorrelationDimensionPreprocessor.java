package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the correlation dimension of objects of a certain database.
 * The PCA is based on epsilon range queries.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RangeQueryBasedCorrelationDimensionPreprocessor extends CorrelationDimensionPreprocessor {

  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "preprocessorEpsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the specified distance function";

  /**
   * Epsilon.
   */
  protected String epsilon;

   /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database based on a range query.
   */
  public RangeQueryBasedCorrelationDimensionPreprocessor() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see CorrelationDimensionPreprocessor#objectIDsForPCA(Integer, de.lmu.ifi.dbs.database.Database, boolean)
   */
  protected List<Integer> objectIDsForPCA(Integer id, Database<DoubleVector> database, boolean verbose) {
    pcaDistanceFunction.setDatabase(database, verbose);
    
    List<QueryResult<DoubleDistance>> knns = database.rangeQuery(id, epsilon, pcaDistanceFunction);

    List<Integer> ids = new ArrayList<Integer>(knns.size());
    for (QueryResult knn : knns) {
      ids.add(knn.getID());
    }

    return ids;
  }

  /**
   * Sets the value for the required parameter k.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      pcaDistanceFunction.valueOf(optionHandler.getOptionValue(EPSILON_P));
      epsilon = optionHandler.getOptionValue(EPSILON_P);
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = result.get(0);
    attributeSettings.addSetting(EPSILON_P, pcaDistanceFunction.valueOf(optionHandler.getOptionValue(EPSILON_P)).toString());

    return result;
  }

  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(RangeQueryBasedCorrelationDimensionPreprocessor.class.getName());
    description.append(" computes the correlation dimension of objects of a certain database.\n");
    description.append("The PCA is based on epsilon range queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }
}

package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.application.cache.CacheDoubleDistanceKNNLists;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.database.ids.generic.DoubleDistanceDBIDPairKNNList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Preprocessor that loads an existing cached kNN result.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class CachedDoubleDistanceKNNPreprocessor<O> extends AbstractMaterializeKNNPreprocessor<O, DoubleDistance, DoubleDistanceKNNList> {
  /**
   * File to load.
   */
  private File filename;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param distanceFunction Distance function
   * @param k K
   * @param file File to load
   */
  public CachedDoubleDistanceKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, DoubleDistance> distanceFunction, int k, File file) {
    super(relation, distanceFunction, k);
    this.filename = file;
  }

  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CachedDoubleDistanceKNNPreprocessor.class);

  @Override
  protected void preprocess() {
    createStorage();
    // open file.
    try (RandomAccessFile file = new RandomAccessFile(filename, "rw");
        FileChannel channel = file.getChannel()) {
      // check magic header
      int header = file.readInt();
      if (header != CacheDoubleDistanceKNNLists.KNN_CACHE_MAGIC) {
        throw new AbortException("Cache magic number does not match.");
      }
      MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY, 4, file.length() - 4);
      for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        int dbid = ByteArrayUtil.readUnsignedVarint(buffer);
        int nnsize = ByteArrayUtil.readUnsignedVarint(buffer);
        if (nnsize < k) {
          throw new AbortException("kNN cache contains fewer than k objects!");
        }
        ArrayList<DoubleDistanceDBIDPair> col = new ArrayList<>(nnsize);
        for (int i = 0; i < nnsize; i++) {
          int nid = ByteArrayUtil.readUnsignedVarint(buffer);
          double dist = buffer.getDouble();
          col.add(DBIDUtil.newDistancePair(dist, DBIDUtil.importInteger(nid)));
        }
        storage.put(DBIDUtil.importInteger(dbid), new DoubleDistanceDBIDPairKNNList(col, nnsize));
      }
      if (buffer.hasRemaining()) {
        LOG.warning("kNN cache has " + buffer.remaining() + " bytes remaining!");
      }
    } catch (IOException e) {
      throw new AbortException("I/O error in loading kNN cache: " + e.getMessage(), e);
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public String getLongName() {
    return "cached-knn";
  }

  @Override
  public String getShortName() {
    return "cached-knn";
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.landmark
   * @apiviz.stereotype factory
   * @apiviz.uses MaterializeKNNPreprocessor oneway - - «create»
   * 
   * @param <O> The object type
   */
  public static class Factory<O> extends AbstractMaterializeKNNPreprocessor.Factory<O, DoubleDistance, DoubleDistanceKNNList> {
    /**
     * Filename to load.
     */
    private File filename;

    /**
     * Index factory.
     * 
     * @param k k parameter
     * @param distanceFunction distance function
     * @param filename Cache file
     */
    public Factory(int k, DistanceFunction<? super O, DoubleDistance> distanceFunction, File filename) {
      super(k, distanceFunction);
      this.filename = filename;
    }

    @Override
    public CachedDoubleDistanceKNNPreprocessor<O> instantiate(Relation<O> relation) {
      CachedDoubleDistanceKNNPreprocessor<O> instance = new CachedDoubleDistanceKNNPreprocessor<>(relation, distanceFunction, k, filename);
      return instance;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<O> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O, DoubleDistance> {
      /**
       * Option ID for the kNN file.
       */
      public static final OptionID CACHE_ID = new OptionID("external.knnfile", "Filename with the precomputed k nearest neighbors.");

      /**
       * Filename to load.
       */
      private File filename;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);

        // Input file parameter
        final FileParameter cpar = new FileParameter(CACHE_ID, FileParameter.FileType.INPUT_FILE);
        if (config.grab(cpar)) {
          filename = cpar.getValue();
        }
      }

      @Override
      protected Factory<O> makeInstance() {
        return new Factory<>(k, distanceFunction, filename);
      }
    }
  }
}

/*
 * Copyright (C) 2008-2014 by Holger Arndt
 *
 * This file is part of the Java Data Mining Package (JDMP).
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * JDMP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * JDMP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with JDMP; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package org.jdmp.core.dataset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jdmp.core.dataset.wrappers.DataSetInputMatrixWrapper;
import org.jdmp.core.dataset.wrappers.DataSetPredictedMatrixWrapper;
import org.jdmp.core.dataset.wrappers.DataSetTargetMatrixWrapper;
import org.jdmp.core.sample.Sample;
import org.jdmp.core.util.ObservableMap;
import org.jdmp.core.variable.Variable;
import org.jdmp.core.variable.VariableFactory;
import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.util.MathUtil;

public class DefaultDataSet extends AbstractDataSet {
	private static final long serialVersionUID = -2887879051530049677L;

	public DefaultDataSet() {
		super();
		Matrix inputMatrix = new DataSetInputMatrixWrapper(this);
		Variable input = VariableFactory.labeledVariable(INPUT);
		input.add(inputMatrix);
		getVariableMap().put(INPUT, input);

		Matrix targetMatrix = new DataSetTargetMatrixWrapper(this);
		Variable target = VariableFactory.labeledVariable("Target");
		target.add(targetMatrix);
		getVariableMap().put(TARGET, target);
		Matrix predictedMatrix = new DataSetPredictedMatrixWrapper(this);
		Variable predicted = VariableFactory.labeledVariable("Predicted");
		predicted.add(predictedMatrix);
		getVariableMap().put(PREDICTED, predicted);

	}

	public Matrix getClassDistribution() {
		Matrix m = Matrix.Factory.zeros(getClassCount(), 1);

		Map<Integer, Double> map = new HashMap<Integer, Double>();

		for (Sample s : getSampleMap()) {
			int c = s.getTargetClass();
			Double d = map.get(c);
			if (d == null) {
				d = 0.0;
			}
			d++;
			map.put(c, d);
		}

		for (int i = 0; i < getClassCount(); i++) {
			Double d = map.get(i);
			if (d == null) {
				d = 0.0;
			}
			m.setAsDouble(d / getSampleMap().getSize(), i, 0);
		}
		return m;
	}

	public double getEarlyStoppingErrorCount(int numberOfSteps) {
		int index = getEarlyStoppingIndex(numberOfSteps);
		if (index >= 0) {
			return getErrorCountVariable().get(index).doubleValue();
		}
		return -1;
	}

	public final long getInputFeatureCount() {
		Matrix m = getInputMatrix();
		if (m != null) {
			return m.getColumnCount();
		} else {
			return 0;
		}
	}

	public final void standardize(int dimension) {
		getInputVariable().getLast().standardize(Ret.ORIG, dimension);
	}

	public final void center(int dimension) {
		getInputVariable().getLast().center(Ret.ORIG, dimension, true);
	}

	public void addMissingValues(int dimension, double percentMissing) {
		getInputVariable().getLast().addMissing(Ret.ORIG, dimension, percentMissing);
	}

	public int getFeatureCount() {
		return (int) getSampleMap().values().iterator().next().getMatrix(INPUT)
				.toRowVector(Ret.NEW).getRowCount();
	}

	public DefaultDataSet clone() {
		DefaultDataSet ds = null;
		try {
			ds = this.getClass().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (Sample s : getSampleMap()) {
			ds.getSampleMap().add(s.clone());
		}
		return ds;
	}

	public Variable getRMSEVariable() {
		return getVariableMap().get(RMSE);
	}

	public Variable getTargetVariable() {
		return getVariableMap().get(TARGET);
	}

	public final long getTargetFeatureCount() {
		Matrix m = getTargetMatrix();
		if (m != null) {
			return m.getColumnCount();
		} else {
			return 0;
		}
	}

	public void appendRMSEMatrix(Matrix m) {
		getRMSEVariable().add(m);
	}

	public Matrix getTargetMatrix() {
		return getTargetVariable().getLast();
	}

	public double getEarlyStoppingRMSE(int numberOfSteps) {
		int index = getEarlyStoppingIndex(numberOfSteps);
		if (index >= 0) {
			return getRMSEVariable().get(index).getEuklideanValue();
		}
		return -1;
	}

	public boolean isEarlyStoppingReached(int numberOfSteps) {
		return getEarlyStoppingIndex(numberOfSteps) >= 0;
	}

	public int getEarlyStoppingIndex(int numberOfSteps) {
		Variable v = getRMSEVariable();
		if (v.size() <= numberOfSteps) {
			return -1;
		}

		double minRMSE = Double.MAX_VALUE;
		int position = -1;
		for (int i = 0; i < v.size(); i++) {
			double e = v.get(i).getEuklideanValue();
			if (e < minRMSE) {
				minRMSE = e;
				position = i;
			}
			if (i == position + numberOfSteps) {
				return position;
			}
		}
		return -1;
	}

	public Variable getPredictedVariable() {
		return getVariableMap().get(PREDICTED);
	}

	public Matrix getPredictedMatrix() {
		return getPredictedVariable().getLast();
	}

	public double getRMSE() {
		Matrix m = getRMSEMatrix();
		if (m == null) {
			return Double.NaN;
		} else {
			return m.getEuklideanValue();
		}
	}

	public Matrix getRMSEMatrix() {
		return getRMSEVariable().getLast();
	}

	public int getClassCount() {
		return (int) getSampleMap().getElementAt(0).getMatrix(Sample.TARGET).toRowVector(Ret.NEW)
				.getRowCount();
	}

	public Variable getConfusionVariable() {
		return getVariableMap().get(CONFUSION);
	}

	public Variable getErrorCountVariable() {
		return getVariableMap().get(ERRORCOUNT);
	}

	public int getErrorCount() {
		return (int) getErrorCountVariable().getLast().doubleValue();
	}

	public Variable getAccuracyVariable() {
		return getVariableMap().get(ACCURACY);
	}

	public void appendConfusionMatrix(Matrix m) {
		getConfusionVariable().add(m);
	}

	public void appendAccuracyMatrix(Matrix m) {
		getAccuracyVariable().add(m);
	}

	public void appendErrorCountMatrix(Matrix m) {
		getErrorCountVariable().add(m);
	}

	public double getAccuracy() {
		return getAccuracyVariable().getLast().doubleValue();
	}

	public double getMaxAccuracy() {
		return getAccuracyVariable().getAsListMatrix().getMaxValue();
	}

	public List<DataSet> splitByClass() {
		List<DataSet> returnDataSets = new ArrayList<DataSet>();

		for (int i = 0; i < getClassCount(); i++) {
			DataSet ds = DataSetFactory.labeledDataSet("Class " + i);
			for (Sample s : getSampleMap()) {
				if (s.getTargetClass() == i) {
					ds.getSampleMap().add(s.clone());
				}
			}
			returnDataSets.add(ds);
		}

		return returnDataSets;
	}

	public List<DataSet> splitForStratifiedCV(int numberOfCVSets, int idOfCVSet, long randomSeed) {
		List<DataSet> returnDataSets = new ArrayList<DataSet>();
		Random rand = new Random(randomSeed);
		int classCount = getClassCount();

		// create empty lists
		List<List<Sample>> sortedSamples = new ArrayList<List<Sample>>();
		for (int i = 0; i < classCount; i++) {
			sortedSamples.add(new ArrayList<Sample>());
		}

		// add samples to lists according to class
		for (Sample s : getSampleMap()) {
			int targetClass = s.getTargetClass();
			sortedSamples.get(targetClass).add(s);
		}

		// shuffle lists
		Collections.shuffle(sortedSamples, rand);
		for (int i = 0; i < classCount; i++) {
			Collections.shuffle(sortedSamples.get(i), rand);
		}

		// create sample lists
		List<List<Sample>> cvSets = new ArrayList<List<Sample>>();
		for (int cvSet = 0; cvSet < numberOfCVSets; cvSet++) {
			List<Sample> samples = new ArrayList<Sample>();
			cvSets.add(samples);
		}

		// distribute the samples
		int fromPointer = 0;
		int toPointer = 0;
		while (!allEmpty(sortedSamples)) {

			for (toPointer = 0; toPointer < cvSets.size(); toPointer++) {

				List<Sample> to = cvSets.get(toPointer);

				while (to.size() < (double) getSampleMap().getSize() / numberOfCVSets
						&& fromPointer < sortedSamples.size()) {
					List<Sample> from = sortedSamples.get(fromPointer);

					if (!from.isEmpty()) {
						to.add(from.remove(0));
					}

					fromPointer++;

				}

				fromPointer = 0;

			}

		}

		// create the data sets
		DataSet train = DataSetFactory.labeledDataSet("TrainingSet " + idOfCVSet + "/"
				+ numberOfCVSets + "(" + randomSeed + ")");
		DataSet test = DataSetFactory.labeledDataSet("TestSet " + idOfCVSet + "/" + numberOfCVSets
				+ "(" + randomSeed + ")");

		test.getSampleMap().addAll(cvSets.remove(idOfCVSet));

		for (List<Sample> list : cvSets) {
			train.getSampleMap().addAll(list);
		}

		returnDataSets.add(train);
		returnDataSets.add(test);

		return returnDataSets;
	}

	private static boolean allEmpty(List<List<Sample>> lists) {
		for (List<Sample> list : lists) {
			if (!list.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public double getEarlyStoppingAccuracy(int numberOfSteps) {
		int index = getEarlyStoppingIndex(numberOfSteps);
		if (index >= 0) {
			return getAccuracyVariable().get(index).doubleValue();
		}
		return -1;
	}

	public DataSet bootstrap(int numberOfSamples) {
		DataSet ds = DataSetFactory.labeledDataSet("Bootstrap of " + getLabel());
		ObservableMap<Sample> sampleList = getSampleMap();
		for (int i = 0; i < numberOfSamples; i++) {
			int rand = MathUtil.nextInteger(0, sampleList.getSize() - 1);
			ds.getSampleMap().add(sampleList.getElementAt(rand).clone());
		}
		return ds;
	}

	public boolean isDiscrete() {
		for (Sample s : getSampleMap().values()) {
			Matrix input = s.getMatrix(INPUT);
			for (long[] c : input.availableCoordinates()) {
				if (!MathUtil.isDiscrete(input.getAsDouble(c))) {
					return false;
				}
			}
		}
		return true;
	}

}

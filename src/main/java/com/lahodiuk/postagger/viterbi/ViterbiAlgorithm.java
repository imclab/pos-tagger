package com.lahodiuk.postagger.viterbi;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class ViterbiAlgorithm {

	public static void main(String[] args) {
		Iterable<String> path = new ViterbiAlgorithm().getMostProbablePath(
				new String[] { "1", "0" },
				Arrays.asList(
						new double[] { 0.6, 0.5 },
						new double[] { 0.6, 0.81 },
						new double[] { 0.4, 0.5 },
						new double[] { 0.7, 0.5 }),
				new TransitionProbability() {
					@Override
					public double transition(String fromState, String toState) {
						return fromState.equals(toState) ? 0.8 : 0.2;
					}

					@Override
					public double start(String state) {
						return 1;
					}

					@Override
					public double end(String state) {
						return 1;
					}
				});

		for (String s : path) {
			System.out.println(s);
		}
	}

	/**
	 * Complexity: O(R * S^2) <br/>
	 * R - number of observed items <br/>
	 * S - number of hidden states
	 */
	public Iterable<String> getMostProbablePath(String[] states, List<double[]> observed, TransitionProbability transitionProbability) {
		int statesCount = states.length;
		int observedCount = observed.size();

		Cell[][] matrix = this.createMatrix(states, statesCount, observedCount);

		// Initializing first column of the hidden-states matrix (taking into
		// account probability, that any state - can be the starting state of
		// the sequence of observations)
		int firstObservedIndex = 0;
		double[] firstObserved = observed.get(firstObservedIndex);
		for (int stateIndex = 0; stateIndex < statesCount; stateIndex++) {
			double stateProbability = firstObserved[stateIndex];
			String state = states[stateIndex];
			double stateStartProbability = transitionProbability.start(state);

			double logStartProbability = this.log(stateProbability) + this.log(stateStartProbability);
			matrix[stateIndex][firstObservedIndex].setValue(logStartProbability);
		}

		// Iterating over all the columns (which corresponding to observations),
		// and maximizing probability of the hidden states (in greedy way)
		for (int observedIndex = firstObservedIndex + 1; observedIndex < observedCount; observedIndex++) {

			double[] currentObserved = observed.get(observedIndex);

			// For each hidden state (of current observed item)
			for (int currStateIndex = 0; currStateIndex < statesCount; currStateIndex++) {

				String currentState = states[currStateIndex];

				Cell bestParent = null;
				double bestParentLogProbability = Double.NEGATIVE_INFINITY;

				// Check all hidden states of previous observed item
				for (int prevStateIndex = 0; prevStateIndex < statesCount; prevStateIndex++) {

					String parentState = states[prevStateIndex];
					double previousToCurrentTransitionProbability = transitionProbability.transition(parentState, currentState);

					Cell parent = matrix[prevStateIndex][observedIndex - 1];
					// Find such previous state, which will maximize expression:
					// previous_state_probability * transition_probability
					double parentLogProbability = parent.getValue() + this.log(previousToCurrentTransitionProbability);

					if (parentLogProbability > bestParentLogProbability) {
						bestParentLogProbability = parentLogProbability;
						bestParent = parent;
					}
				}

				Cell current = matrix[currStateIndex][observedIndex];
				double currentStateProbability = currentObserved[currStateIndex];
				double currentLogProbability = bestParentLogProbability + this.log(currentStateProbability);

				current.setParent(bestParent);
				current.setValue(currentLogProbability);
			}
		}

		// Changing last column of the matrix (taking into
		// account probability, that any state - can be the ending state of
		// the sequence of observations)
		int lastObservedIndex = observedCount - 1;
		for (int stateIndex = 0; stateIndex < statesCount; stateIndex++) {
			String state = states[stateIndex];
			double stateEndProbability = transitionProbability.end(state);

			Cell cell = matrix[stateIndex][lastObservedIndex];
			double logEndProbability = cell.getValue() + this.log(stateEndProbability);

			cell.setValue(logEndProbability);
		}

		// Finding the cell from last column - which have the highest
		// probability
		Cell lastCell = matrix[0][lastObservedIndex];
		for (int lastStateIndex = 1; lastStateIndex < statesCount; lastStateIndex++) {
			Cell cell = matrix[lastStateIndex][lastObservedIndex];

			if (cell.getValue() > lastCell.getValue()) {
				lastCell = cell;
			}
		}

		Stack<String> path = this.backtrace(lastCell);
		return path;
	}

	private Stack<String> backtrace(Cell lastCell) {
		Stack<String> path = new Stack<>();

		while (lastCell != null) {
			path.push(lastCell.getState());
			lastCell = lastCell.getParent();
		}
		return path;
	}

	private Cell[][] createMatrix(String[] states, int statesCount, int observedCount) {
		Cell[][] matrix = new Cell[statesCount][observedCount];
		for (int stateIndex = 0; stateIndex < statesCount; stateIndex++) {
			for (int observedIndex = 0; observedIndex < observedCount; observedIndex++) {
				String state = states[stateIndex];
				matrix[stateIndex][observedIndex] = new Cell(state);
			}
		}
		return matrix;
	}

	private double log(double x) {
		return Math.log(x + 1e-5);
	}

	private static class Cell {
		private String state;
		private Cell parent;
		private double value;

		public Cell(String state) {
			this.state = state;
		}

		public void setParent(Cell parent) {
			this.parent = parent;
		}

		public void setValue(double value) {
			this.value = value;
		}

		public Cell getParent() {
			return this.parent;
		}

		public double getValue() {
			return this.value;
		}

		public String getState() {
			return this.state;
		}
	}

	public static interface TransitionProbability {
		double transition(String fromState, String toState);

		double start(String state);

		double end(String state);
	}
}

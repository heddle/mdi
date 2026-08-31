/**
 * Generic, maximization-oriented genetic-algorithm contracts and simulation
 * support. Problems provide populations and finite fitness values; operators
 * select, recombine, mutate, and replace individuals while solutions provide
 * self-typed independent copies for snapshots and elitism. Replacement policy
 * owns survivor retention and declares how many offspring it requires.
 */
package edu.cnu.mdi.sim.ga;

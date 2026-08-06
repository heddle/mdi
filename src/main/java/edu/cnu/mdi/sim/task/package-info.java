/**
 * Typed one-shot background execution built on MDI's simulation engine.
 *
 * <p>Use {@link edu.cnu.mdi.sim.task.BackgroundTasks#create} when listeners or
 * controls must be attached before execution, then call
 * {@link edu.cnu.mdi.sim.task.TaskHandle#start()}. Use
 * {@link edu.cnu.mdi.sim.task.BackgroundTasks#submit} for fire-and-observe
 * work. Task bodies remain independent of Swing and communicate through
 * {@link edu.cnu.mdi.sim.task.TaskContext}.</p>
 */
package edu.cnu.mdi.sim.task;

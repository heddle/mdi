package edu.cnu.mdi.sim.ga.triimage;


/**
* Narrow reset contract for the image evolution demo.
* <p>
* Implemented by {@link ImageEvolutionDemoView} so that
* {@link ImageEvolutionControlPanel} can request a reset without coupling to
* the view's concrete type — exactly the same pattern used by
* {@code IResettable} in the network decluttering demo.
* </p>
*/
public interface IImageEvolutionResettable {

   /**
    * Request a reset with the given parameters.
    * <p>
    * Implementations should stop the current engine, build a new simulation
    * with the supplied parameters, and restart. This should only be called
    * when the simulation is in a safe state (READY, PAUSED, TERMINATED, or
    * FAILED); the control panel is responsible for enforcing this via button
    * enable/disable logic.
    * </p>
    *
    * @param numTriangles   number of triangles per chromosome (must be &gt; 0)
    * @param populationSize number of individuals in the population (must be &gt; 0)
    */
   void requestReset(int numTriangles, int populationSize);
}

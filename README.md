# MDI Project

MDI (Multi-Document Interface) is a lightweight Java framework for building
modular scientific and visualization applications.  
It is designed so that each window or tool (a “document”) can run independently
while sharing a common model, controller, and messaging infrastructure.

---

# splot
splot is a Swing-based plotting library. As such, all UI notifications and rendering must occur
on the Swing Event Dispatch Thread (EDT). Internally, curve updates may invalidate cached fits
and trigger repaints, so care is taken to preserve this contract. We believe splot is thread-safe and
that plot added can be added from any thread. Several of the example plots test the thead safety.
The add and addall methods for the curces extending the abstract ACurve class can be called from any thread.
If called on the EDT the new data are applied immediately and a single DATA change event is fired.
If called off the EDT (i.e., on a background worker thread) the data are enqueued into a lock-free
staging queue, and a coalesced drain pass is scheduled on the EDT. The drain applies data in batches
and fires one consolidated DATA change event, preventing repaint storms.

---

## 🛠 Building

This project uses **Maven**.

To build the JAR:

```bash
mvn clean package

To run tests:
mvn test
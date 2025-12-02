# MDI Project

MDI (Multi-Document Interface) is a lightweight Java framework for building
modular scientific and visualization applications.  
It is designed so that each window or tool (a “document”) can run independently
while sharing a common model, controller, and messaging infrastructure.

This repository currently contains the initial project skeleton and will expand
as the core functionality is implemented.

---

## 📁 Project Structure

mdi/
├── pom.xml
├── src/
│ ├── main/
│ │ ├── java/
│ │ │ └── edu/cnu/mdi/
│ │ │ ├── control/
│ │ │ ├── model/
│ │ │ ├── vis/
│ │ │ └── demo/
│ │ └── resources/
│ └── test/
│ └── java/
└── lib/ (optional — used only for local JARs)

- **model** — Application state, data structures, physics or geometry models  
- **control** — Controller logic, event routing, UI coordination  
- **vis** — Visualization components (2D/3D, rendering, panels, views)  
- **demo** — Simple examples and test windows that exercise the framework  

---

## 🛠 Building

This project uses **Maven**.

To build the JAR:

```bash
mvn clean package

To run tests:
mvn test
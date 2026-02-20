# MDI – Modular Desktop Interface for Scientific Applications

MDI is a Java framework for building scientific desktop applications with:

- Interactive plotting  
- Simulation engines  
- Multi-view modular architecture  
- Extensible tools and layered drawing  
- Distribution via Maven Central  

It is built on pure Swing for long-term JVM stability and zero external runtime dependencies.

---

## Why MDI?

Scientific desktop applications have different needs than typical GUI apps:

- Long-running simulations  
- Real-time data visualization  
- Multi-document workflows  
- Precise rendering control  
- Stability across Java versions  

MDI provides architectural infrastructure for these use cases.

It is not just a widget toolkit.  
It is a foundation for building complete scientific applications.

---

## Key Features

### Multi-Document Architecture

Each window (“view”) operates independently while sharing:

- Messaging infrastructure  
- Common models  
- Simulation engine integration  
- Extensible toolbars  
- Layered drawing support  

---

### Integrated Plotting (splot)

The built-in plotting module provides:

- Thread-safe curve updates  
- Swing EDT-safe rendering  
- Curve fitting  
- Lock-free staging queues for background updates  
- Coalesced repaint events  

Plots can safely receive data from worker threads without repaint storms.

---

### Built-In Simulation Framework

MDI includes:

- Step-based simulation engines  
- Cancel support  
- Reset hooks  
- Coordinated view refresh  
- Background execution integration  

Ideal for:

- Physics demonstrations  
- Optimization visualizations  
- Network simulations  
- Educational tools  

---

### Layered Drawing System

Views support:

- Items  
- Layers  
- Mouse interaction  
- Selection tools  
- Virtual desktop behavior  

This makes it easy to build:

- Network graphs  
- Geometric editors  
- Data overlays  
- Interactive teaching tools  

---

## Installation

MDI is available on Maven Central:

```xml
<dependency>
    <groupId>io.github.heddle</groupId>
    <artifactId>mdi</artifactId>
    <version>1.0.0</version>
</dependency>

---

## Hello MDI

<img src="docs/images/helloMDI.png" width="800">

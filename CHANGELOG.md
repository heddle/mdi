# Changelog

All notable changes to the MDI Scientific Visualization Framework are documented
in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

No changelog was kept during development prior to this file, so the entry below
for 1.2.2 has been reconstructed from the commit history between the `v1.2.1`
tag and release. For versions 1.2.1 and earlier, see the corresponding `vX.Y.Z`
git tags.

## [Unreleased]

## [1.2.2] - 2026-08-31

### Added

- **Mapping subsystem** (`edu.cnu.mdi.mapping`), new in this release: 2D map
  views with four built-in projections (Mercator, Orthographic, Mollweide,
  Lambert Azimuthal Equal-Area); country-fill, ETOPO5 terrain, graticule, and
  country-boundary layers, plus an optional city layer; a capability-driven
  layer-style editor; a map control panel for switching projection and theme;
  support for registering custom application-supplied projections; shapefile
  loading and rendering, including a Shapefiles menu, a DBF attribute-field
  selector dialog, and per-layer style editing; and multi-tick color-scale
  legends.
- **Simulation subsystem** (`edu.cnu.mdi.sim`): substantially expanded genetic
  algorithm and simulated-annealing support, including an image-evolution demo
  problem, a robust-statistics-based initial-temperature heuristic
  (`EnergyDistributionHeuristic`), a `TaskControlPanel` for one-shot background
  tasks, and a network-decluttering force-directed layout demo.
- New Swing components: `VerticalFlowLayout`, `CheckBoxArray`,
  `LabeledTextField`, an aspect-ratio-preserving panel host, and a
  FlatLaf-aware font-factory helper (`Fonts`).
- `StartupWindow`: an optional lightweight startup window showing application
  metadata and live log history while the main frame comes up.
- `BaseMDIApplication.exitOnClose()`: opt-in hook to terminate the JVM when the
  application frame closes.
- Clipboard support for `TakePicture`, alongside file export.
- Support for reversed world axes in container coordinate systems.
- `CreationSupport.defaultConfigureItem` is now public, so applications can
  reuse the framework's default item-configuration flags directly.

### Changed

- Tiled view layout controls improved; application views now stay above the
  virtual-desktop navigator instead of behind it.
- The JSON view is hidden by default.
- `ShapefileCountryLoader.load()` now throws `IOException` on a `.shp`/`.dbf`
  record-count mismatch instead of silently substituting an empty attribute
  row and dropping the affected countries with no warning.
- `IpField.setText()` now sets the given text verbatim, like the standard
  `JTextField` contract, instead of silently substituting a hardcoded
  `127.0.0.1` for any text that fails validation; validity is still tracked
  via `validText()`.
- `VerticalFlowLayout.minimumLayoutSize()` is now computed from the contained
  components' actual minimum sizes, instead of returning the parent's
  current (possibly not-yet-laid-out) size.
- `APointerButton`'s gesture-start dispatch now correctly distinguishes
  click-based rubberband policies (Polygon, Polyline, Line, RadArc,
  TwoClickLine) from drag-based ones.
- `RubberTwoClickLine` now correctly reports its own policy instead of
  inheriting `RubberLine`'s.
- `DraggableRectangle`/plot overlays: dragging state is only considered
  "moved" once an actual drag begins, not on every mouse release — fixes
  `ExtraText` losing its default position after the first click anywhere on
  a plot canvas.
- `HistoCurve.clearData()` now honors the documented `ACurve` contract (safe
  to call off the Swing EDT) instead of throwing when called off-EDT.
- `Histo2DData`'s backing bin storage is no longer publicly mutable, closing
  a gap in its documented thread-safety guarantee.

### Fixed

- Adaptive graticule spacing bug on the map view.
- Polygon map rendering across the antimeridian seam.
- Plot image capture (`TakePicture`) losing the title, legend, and axis
  labels.
- Null-frame-size guard in `BaseView`'s `FRACTION`-based sizing.
- Color-scale legend layout now respects component borders.
- Startup painting hardened on macOS; the virtual desktop's initial paint is
  finalized correctly on startup.

### Documentation & Testing

- Full pass over the public/protected API for Javadoc accuracy and
  completeness: corrected roughly 65 instances of missing, stale, or
  actively misleading documentation across every package (wrong documented
  defaults, claimed exceptions that don't occur, stale field/class
  references, and copy-pasted descriptions that no longer matched the code
  they described).
- Added roughly 110 new unit tests targeting previously-untested core logic,
  including the plot coordinate-transform pipeline (`PlotCanvas
  .setWorldSystem()`), great-circle geodesic math, genetic-algorithm and
  simulated-annealing operators, gesture-classification priority, and
  histogram peak-finding.
- Chapters 1–9 of the MDI reference book were independently verified against
  the current source and corrected where they had drifted.

[Unreleased]: https://github.com/heddle/mdi/compare/v1.2.2...develop
[1.2.2]: https://github.com/heddle/mdi/compare/v1.2.1...v1.2.2

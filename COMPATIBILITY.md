# Compatibility policy

What you can rely on when you build on MDI and upgrade it. This policy applies
to releases after 1.2.4. (1.2.4 predates it: it added features and removed two
dependencies under a patch number.)

## Version numbers

MDI uses `major.minor.patch`.

| Release | May contain | Existing code that uses the public API |
| --- | --- | --- |
| Patch (1.2.**5**) | Fixes, and any small addition a fix needs | Keeps compiling and running |
| Minor (1.**3**.0) | New features and deprecations | Keeps compiling and running |
| Major (**2**.0.0) | Anything, including removals | May need changes |

## What is public API

Every `public` and `protected` class, method and field under `edu.cnu.mdi`.
Protected members count because applications extend MDI classes.

These are examples, not API, and may change in any release:

- `edu.cnu.mdi.app.DemoApp` and `edu.cnu.mdi.app.HelloMDI`
- `edu.cnu.mdi.sim.demo.network`
- `edu.cnu.mdi.sim.ga.triimage`
- `edu.cnu.mdi.sim.simanneal.tspdemo`
- `edu.cnu.mdi.splot.example`
- `edu.cnu.mdi.view.demo.layout`

`edu.cnu.mdi.view.demo.geoslice` is treated as public despite its name, because
MDI-3D uses it.

## Also treated as a breaking change

These need a major release, like removing a method:

- removing a runtime dependency, or changing one in a way applications can see
  (applications may use it through MDI);
- raising the minimum Java version (currently 17).

Saved files: a newer release is intended to read plot files and layout files
written by earlier releases of the same major version. This is not yet tested.

## What subclasses can rely on

**Item change notifications.** A layer's `ItemChangeListener`s hear:

| Event | When |
| --- | --- |
| `MOVED`, `RESIZED`, `ROTATED` | A user gesture ends, or is undone or redone |
| `MODIFIED` | A style or text edit is undone or redone |
| `ADDED`, `DELETED` | An item joins or leaves a layer, including through undo and redo |
| `SELECTED`, `DESELECTED` | Selection changes |

Changing an item from your own code, for example with `translateWorld` or a
setter, does not send `MOVED`, `RESIZED`, `ROTATED` or `MODIFIED`.

**Undo is inherited.** A subclass of an undoable item is undoable too, but only
the inherited state is saved. If your item has editable state of its own,
include it by overriding `captureState()` and `restoreState(ItemState)`, or
return `null` from `captureState()` to opt out.

**Threading.** Create and change views, containers, layers and items on the
Swing event dispatch thread.

**New inherited behavior is announced.** A change that existing subclasses pick
up without asking for it (as undo was) goes in a minor release, not a patch, and
the changelog says that it affects subclasses.

**Not promised:** how often or in what order MDI calls an overridable method,
pixel-exact rendering, and the wording or position of menu items.

## Deprecation

Before anything public is removed, it is marked `@Deprecated` with a Javadoc
`@deprecated` note naming the replacement. It stays for at least one minor
release and is removed only in a major release.

## The automatic check

`mvn verify`, locally and in CI, compares the public API with the last release
(`api.baseline.version` in `pom.xml`) using
[japicmp](https://siom79.github.io/japicmp/). An incompatible change fails the
build. The report is in `target/japicmp/`.

The check compares signatures. It does **not** notice changes in behavior,
removed dependencies or file format changes; those depend on tests and review.

### Making an intentional breaking change

This belongs in a major release. Add the specific class or member to the
plugin's `<excludes>` in `pom.xml`, with a comment saying why (a member is
written `edu.cnu.mdi.item.AItem#methodName(int)`), and record it in the
changelog. Remove the exclusion after the release.

### After each release

Once the release is published on Maven Central, set `api.baseline.version` to
it. Doing so earlier makes CI fail, because the baseline cannot be downloaded.

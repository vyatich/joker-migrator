---
name: openrewrite-recipe-author
description: Writes, tests and publishes OpenRewrite recipes that migrate the users of a Java library to its new major version — once, for every team that uses the library. Use this skill when a library gets a breaking release, when migration reports list "recipe candidates" (before/after examples of repeated hand fixes), when someone asks to automate an upgrade of an internal library, to write an OpenRewrite recipe, or to work out what changed between two versions of a library from its jars — even if they only say "write a migration for v3" or hand over two jar files.
---

# Recipes for internal libraries

A recipe is written once, by the team that owns the library, and run by every team that uses it. That is the whole point: a fix written into a recipe never has to be made by hand — or by an LLM — again. So the recipe lives with the library, is versioned with it and is published to the company's artifact server next to it, not copied into each service.

What can go wrong is also worth knowing up front. A recipe that leaves a compile error behind costs one developer ten minutes. A recipe that compiles but quietly changes behaviour — picks the look-alike method, keeps a call whose semantics changed — ships to every team at once. Most of this skill is about not doing the second.

## 1. Gather the inputs

Ask for, or find:

- **Both versions of every artifact the users depend on** (not only the core jar: data formats, modules, BOM), plus third-party jars they need to load.
- **Sources jars of both versions.** Signatures say what changed; Javadoc says what a method *does* and what replaces a deprecated one. Two methods with the same arguments (`transformedList` / `transformingList`) differ only in their Javadoc.
- **Changelog, release notes, migration guide** of the new major, and the `@deprecated` notes of the last old minor — projects often document replacements there. Removed methods without a same-signature successor, changed defaults and changed behaviour are only in text.
- **One or two services that use the library**, on their current commit, for step 5. Migration reports' "recipe candidates" (the `openrewrite-migrate` skill writes them) are the best test material: real code from real users.

Without sources and changelog you can still write a recipe that compiles; you cannot know that it behaves the same.

## 2. Find the changes

Run the API diff on the jars:

```
java <skill>/scripts/ApiDiff.java --old old-a.jar,old-b.jar --new new-a.jar,new-b.jar \
     --map com.example.old=com.example.api [--types renames.txt] [--cp third-party.jar] [--out api-diff]
```

It prints a one-line summary and writes `report.md` plus recipe fragments that use the old type names. How to treat each part:

| Report section | What to do |
| --- | --- |
| class files Java X -> Y | users must build on Java Y: add a Java version step and check it in a real build (step 5) |
| Moved or renamed types → `safe-types.yml` | take as is; easy to forget while busy with renames |
| Missing types, candidates by method similarity | decide each one (changelog, Javadoc, superclass hint), write `old=new` lines into a `--types` file and run again, so members of renamed types get compared too |
| Incompatible members → `review-renames.yml` | **candidates, not answers.** Confirm each with the Javadoc of the new method. Entries marked `LOOK-ALIKES` or "existed in the old version too" need Javadoc or a probe (see `reference/pitfalls.md`) |
| Enum constants → `safe-constants.yml` | take; constants reported as removed often were renamed (check the changelog) |
| Feature defaults that changed | behaviour changes: list them in the recipe description; do not flip them silently |
| Checked exceptions dropped | `catch` and `throws` around library calls stop compiling: an imperative recipe (`reference/pitfalls.md`) |
| Kept members deprecated in the new version | they compile; people migrate them anyway. Rename where the Javadoc names a replacement with the same behaviour |

Then go through the changelog or migration guide item by item: each item is either already in the report, or becomes a recipe step, or goes into the description's "does not cover". Things only text can tell you — a replacement for a removed method, a default that changed, a method that now throws where it returned `null` — are exactly the ones that hurt in production.

Before writing anything, read `reference/pitfalls.md`: every item there broke a real recipe.

## 3. Pick the simplest kind of recipe for each change

| Change | Kind | Reference |
| --- | --- | --- |
| Package, type, method, field, enum constant renamed; dependency coordinates or version | declarative YAML from ready-made recipes | `reference/declarative.md` |
| An expression rewritten the same way everywhere (wrap an argument, replace a call by another call) | Refaster template | `reference/refaster-and-imperative.md` |
| Depends on context (several statements, conditions, types of surrounding code): mutable object became immutable + builder, checked exceptions became unchecked, return type changed from `Iterator` to `Collection` | imperative visitor in Java | `reference/refaster-and-imperative.md` |
| Changed behaviour, nothing to rewrite mechanically | none — explain it in the migration notes and the recipe description | — |

Climb the ladder only when the step below cannot express the change: a declarative recipe has no code to maintain, an imperative one is a small program with its own bugs.

## 4. Write them in the library's recipe module

- One module next to the library, published as `<group>:<library>-recipes:<library version>`.
- One composite recipe per major upgrade (`<group>.MigrateTo<Library><Major>`) that lists all the small ones, so users run a single name. Order inside it matters (`reference/pitfalls.md`, "Order and collisions").
- Its `description` says what it covers and, explicitly, what it does **not** and which **behaviour changes** remain — the migrating agent reads that to know what is left for it, and a developer reads it to know what to test.

## 5. Test them

- **Unit tests** with `RewriteTest` for every non-declarative recipe: the before/after examples from step 1, plus a case that must stay unchanged.
- **On real code**, one or two services on their current (pre-upgrade) commit:
  1. *Control:* only the dependency bump, no code changes. The compile errors it leaves are the size of the migration.
  2. *Recipe:* run the composite recipe (`java <openrewrite-migrate>/scripts/RunRecipes.java --project <dir> --recipes <name> --artifacts <g:a:v>`), then a real build with tests: `java <openrewrite-migrate>/scripts/BuildReport.java --project <dir> --task test`. A real build, not just compiling the sources: recipes break build files in ways javac never sees (orphaned versions, a stale Java property).
  3. Every remaining error is either fixed in the recipe or named in the description. Failing tests point at behaviour changes — check them against the defaults list from step 2.
- **If a public recipe for the same migration exists** (OpenRewrite catalog), run it on the same service and compare the results. Its released version may lag its repository by months; say which one you compared with.

## 6. Publish and announce

1. Publish the recipe artifact together with the library release.
2. Name the recipe and its artifact in the library's migration notes, with the list of changes it does not cover and the behaviour changes.
3. Add a line to the organisation's recipe catalog (`openrewrite-migrate/reference/internal-recipes.md` or wherever the platform team keeps it).

From then on, every migrating team connects the artifact to its recipe run and gets these changes for free.

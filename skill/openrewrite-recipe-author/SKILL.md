---
name: openrewrite-recipe-author
description: Writes, tests and publishes OpenRewrite recipes that migrate the users of an internal Java library to its new major version — once, for every team that uses the library. Use this skill when a library gets a breaking release, when migration reports list "recipe candidates" (before/after examples of repeated hand fixes), or when someone asks to automate an upgrade of an internal library or to write an OpenRewrite recipe.
---

# Recipes for internal libraries

A recipe is written once, by the team that owns the library, and run by every team that uses it. That is the whole point: a fix written into a recipe never has to be made by hand — or by an LLM — again. So the recipe lives with the library, is versioned with it and is published to the company's artifact server next to it, not copied into each service.

## 1. Collect the changes

Make a list of every breaking change between the old and the new major version, from:

- the library's own changelog or `MIGRATION.md`;
- the public API itself: `javap -public` on both jars, or a binary-compatibility tool such as japicmp if the build has it;
- "Recipe candidates" from migration reports of services (the `openrewrite-migrate` skill writes them) — they are real code from real users, the best test material there is.

## 2. Pick the simplest kind of recipe for each change

| Change | Kind | Reference |
| --- | --- | --- |
| Package, type, method, field renamed; dependency coordinates or version | declarative YAML from ready-made recipes | `reference/declarative.md` |
| An expression rewritten the same way everywhere (wrap an argument, replace a call by another call) | Refaster template | `reference/refaster-and-imperative.md` |
| Depends on context (several statements, conditions, types of surrounding code) | imperative visitor in Java | `reference/refaster-and-imperative.md` |
| Changed behaviour, nothing to rewrite mechanically | none — explain it in the migration notes | — |

Climb the ladder only when the step below cannot express the change: a declarative recipe has no code to maintain, an imperative one is a small program with its own bugs.

## 3. Write them in the library's recipe module

- One module next to the library, published as `<group>:<library>-recipes:<library version>`.
- One composite recipe per major upgrade (`<group>.MigrateTo<Library><Major>`) that lists all the small ones, so users run a single name.
- Its `description` says what it covers and, explicitly, what it does **not** — the migrating agent reads that to know what is left for it.

## 4. Test them

- **Unit tests** with `RewriteTest` for every non-declarative recipe: the before/after examples from step 1, plus a case that must stay unchanged.
- **On real code:** a dry run (`rewriteDryRun` / `rewrite-maven-plugin:dryRun`) of the composite recipe on one or two services that use the library, on their current (pre-upgrade) commit — they still compile there, which the plugin needs. The patch must contain the expected changes and nothing else; after applying it and bumping the library, the service must compile as far as the uncovered changes allow.

## 5. Publish and announce

1. Publish the recipe artifact together with the library release.
2. Name the recipe and its artifact in the library's migration notes, with the list of changes it does not cover.
3. Add a line to the organisation's recipe catalog (`openrewrite-migrate/reference/internal-recipes.md` or wherever the platform team keeps it).

From then on, every migrating team connects the artifact to its recipe run and gets these changes for free.

# Declarative recipes

A declarative recipe is YAML that combines ready-made recipes with parameters. No code, no build step: put the file under `src/main/resources/META-INF/rewrite/` of the recipe module and the jar is a recipe artifact. Both the Maven and the Gradle plugins find recipes there on the recipe classpath.

## Example

A fictional library `com.example:legacy-lib` 3.x → 4.0 moved its package, renamed a class and a method, and changed its coordinates' version:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.example.legacylib.MigrateToLegacyLib4
displayName: Migrate to legacy-lib 4.0
description: >-
  Package com.example.legacylib.old → com.example.legacylib.api, Session → Connection,
  Connection.close(boolean) → shutdown(boolean), dependency 3.x → 4.0.0.
  Does not cover the new retry configuration (see MIGRATION.md).
recipeList:
  - org.openrewrite.java.ChangePackage:
      oldPackageName: com.example.legacylib.old
      newPackageName: com.example.legacylib.api
      recursive: true
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: com.example.legacylib.api.Session
      newFullyQualifiedTypeName: com.example.legacylib.api.Connection
  - org.openrewrite.java.ChangeMethodName:
      methodPattern: com.example.legacylib.api.Connection close(boolean)
      newMethodName: shutdown
  - org.openrewrite.gradle.UpgradeDependencyVersion:
      groupId: com.example
      artifactId: legacy-lib
      newVersion: 4.0.0
  - org.openrewrite.maven.UpgradeDependencyVersion:
      groupId: com.example
      artifactId: legacy-lib
      newVersion: 4.0.0
```

Order matters: after `ChangePackage` the old class lives in the new package, so the following steps refer to the new package name. Include both the Gradle and the Maven version recipe — each one does nothing on the other build tool, and the artifact then works for every service.

## Building blocks that are often enough

| Need | Recipe |
| --- | --- |
| Package renamed | `org.openrewrite.java.ChangePackage` |
| Class renamed or moved | `org.openrewrite.java.ChangeType` |
| Method renamed | `org.openrewrite.java.ChangeMethodName` |
| Method moved to another class (static) | `org.openrewrite.java.ChangeMethodTargetToStatic` |
| Argument order changed | `org.openrewrite.java.ReorderMethodArguments` |
| Annotation replaced | `org.openrewrite.java.ChangeAnnotationAttributeName`, `org.openrewrite.java.ReplaceAnnotation` |
| Dependency coordinates changed | `org.openrewrite.java.dependencies.ChangeDependency` |
| Property key renamed | `org.openrewrite.properties.ChangePropertyKey`, `org.openrewrite.yaml.ChangePropertyKey` |

Check the exact name and options of each one in the OpenRewrite recipe catalog (`https://docs.openrewrite.org/recipes`) before using it — options change between versions.

## Module

The recipe module needs no dependencies at all if it is declarative only; publish it with the library's version:

```kotlin
// recipes/build.gradle.kts
plugins { `java-library`; `maven-publish` }
version = "4.0.0"
publishing {
    publications { create<MavenPublication>("recipes") { artifactId = "legacy-lib-recipes"; from(components["java"]) } }
}
```

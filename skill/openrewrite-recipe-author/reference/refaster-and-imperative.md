# Refaster templates and imperative recipes

Needed when a change is not a rename. Both are Java code in the recipe module and both need tests.

## Module setup

```kotlin
// recipes/build.gradle.kts
plugins { `java-library`; `maven-publish` }

dependencies {
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:<version>"))
    implementation("org.openrewrite:rewrite-java")
    annotationProcessor("org.openrewrite:rewrite-templating:<version>")   // generates recipes from Refaster templates
    compileOnly("com.google.errorprone:error_prone_core:<version>")       // Refaster annotations
    // the library itself, old and new API, for type attribution in templates and tests
    testImplementation("org.openrewrite:rewrite-test")
    testRuntimeOnly("org.openrewrite:rewrite-java-17")                    // parser for the Java version of the user code
}
```

Take the versions from Maven Central (see the version lookup in `openrewrite-migrate/reference/recipe-catalog.md`). The Java parser module must match a JDK the tests run on.

## Refaster template

One expression in, one expression out, with types checked. Example for a fictional `legacy-lib` whose `Client(String url, long timeoutMillis)` became `Client(String url, Duration timeout)`:

```java
public class ClientTimeoutToDuration {
    @BeforeTemplate
    Client before(String url, long timeoutMillis) {
        return new com.example.legacylib.old.Client(url, timeoutMillis);
    }

    @AfterTemplate
    Client after(String url, long timeoutMillis) {
        return new com.example.legacylib.api.Client(url, Duration.ofMillis(timeoutMillis));
    }
}
```

`rewrite-templating` turns it into a recipe named after the class; add that name to the composite recipe.

Old and new API in one template: both versions of the library must be on the compile classpath. If they share coordinates, add one of them as a file dependency or a test fixture.

## Imperative recipe

A `Recipe` with a `JavaIsoVisitor` — for changes that depend on surrounding code. Keep each recipe small and focused; reuse `JavaTemplate` for the code you insert, and call `maybeAddImport` / `maybeRemoveImport` instead of editing imports by hand.

## Tests

```java
class ClientTimeoutToDurationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ClientTimeoutToDurationRecipe())
            .parser(JavaParser.fromJavaVersion().classpath("legacy-lib"));
    }

    @Test
    void wrapsMillisInDuration() {
        rewriteRun(java(
            "class A { Client c = new Client(\"http://x\", 2000); }",
            "class A { Client c = new Client(\"http://x\", Duration.ofMillis(2000)); }"));
    }

    @Test
    void leavesDurationCallsAlone() {
        rewriteRun(java("class A { Client c = new Client(\"http://x\", Duration.ofSeconds(2)); }"));
    }
}
```

Use the before/after examples from migration reports as test cases — they are real code. Always add at least one case that must stay unchanged.

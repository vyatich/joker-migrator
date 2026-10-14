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

Things that cost time on the first build:

- `rewrite-recipe-bom` can reference a core `rewrite-bom` version that is not on Maven Central yet. If the import fails, import `org.openrewrite:rewrite-bom` with a published version directly and pin the recipe modules you use (`rewrite-java-dependencies`, `rewrite-migrate-java`).
- `rewrite-test` brings its own JUnit Platform; a different `junit-jupiter` version makes Surefire fail before any test runs (`NoClassDefFoundError: org/junit/platform/...`). Use the version `mvn dependency:tree` shows for `junit-jupiter-api`.
- The recipe runs on the users' code **before** the upgrade, so their code is type-attributed against the **old** library. Put the old library on the recipe's runtime classpath and give templates `JavaParser.fromJavaVersion().classpath("<old artifact>")`. Code that exists only in the new API can still be generated as text (its types stay unknown); often the old version already has the new style (Jackson 2.12+ has `JsonMapper.builder()`), and the package change later in the composite moves it.

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

- Guard the visitor with `Preconditions.check(new UsesType<>("<old package>..*", true), ...)` so files that do not use the library are skipped.
- A `JavaTemplate` applies at the cursor it is given: to replace a whole call chain, pass the chain's coordinates, not those of the expression inside it.
- When you replace a type in a tree by hand (`J.Identifier` for a catch type), set the type on the variables too, or `maybeRemoveImport` keeps the old import. It also keeps imports that are "used" only through method type information (thrown exceptions of old-API methods); remove those with `new RemoveImport<>(fqn, true)` after checking that no identifier names the type.
- Visit `J.MemberReference` as well as `J.MethodInvocation` when a method is renamed or replaced (`node::fields`).
- Make every rewrite idempotent: the recipe runs in cycles until nothing changes; `x.values()` → `x.values().iterator()` must not fire again on its own output.

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

If the expected output references types that are not on the test classpath (new-API code generated as text), relax type validation for that test: `spec.typeValidationOptions(TypeValidation.none())`. Keep it on everywhere else — it catches templates that lost their types.

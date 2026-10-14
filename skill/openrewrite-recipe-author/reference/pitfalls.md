# Pitfalls

Each item below broke a real recipe: written for Commons Lang 2→3, Commons Collections 3→4 and Jackson 2→3 and checked on real projects. Most compiled fine and changed behaviour, which is why they are worth reading before writing a recipe, not after.

## Contents
- Candidates that look right
- Same signature, different behaviour — and how to probe it
- Order and collisions
- Things easy to forget
- Exceptions that became unchecked
- Mutable objects that became immutable
- Changed return types
- Build files
- Behaviour changes in defaults

## Candidates that look right

`ApiDiff` proposes a replacement for a removed method when a new method takes the same arguments and has a similar name. That is evidence, not proof.

- **Look-alikes.** `TransformedList.decorate(list, t)` has two successors with the same arguments: `transformingList` (existing elements are *not* transformed — same as `decorate`) and `transformedList` (they *are*). The name of the class suggests the wrong one. Only the Javadoc tells them apart.
- **Same words, different meaning.** `getBidiMap()` → `inverseBidiMap()` shares two words and the return type; it returns the inverse map.
- **The successor existed already.** `chompLast(s)` → `chomp(s)`: `chomp` was there in the old version and strips `\r` too. When the target name existed before, compare both methods' Javadoc; a small difference may be acceptable, but say so in the description.

Rule: every entry of `review-renames.yml` gets a look at the new method's Javadoc; `LOOK-ALIKES` and "existed in the old version" entries get a probe.

## Same signature, different behaviour — and how to probe it

No diff shows this: `JsonNode.textValue()` exists in Jackson 2 and 3 with the same signature, returns `null` for a number node in 2.x and throws in 3.x. Keeping the call and renaming it to the "obvious" `stringValue()` both change behaviour; the faithful rewrite is `stringValue(null)`. Candidates for this are methods the changelog mentions under "behaviour", "improved", "consistent", and every look-alike decision.

A probe is a few lines run against each version:

```java
// Probe.java - java -cp <old jars> Probe.java   then   java -cp <new jars> Probe.java (with the new imports)
import com.example.old.Thing;
public class Probe {
    public static void main(String[] a) {
        try { System.out.println(Thing.of(1).textValue()); }
        catch (Exception e) { System.out.println("throws " + e.getClass().getSimpleName()); }
    }
}
```

Use the inputs users will actually pass: null, empty, a value of the "wrong" kind, an existing element in a collection being decorated.

## Order and collisions

- **Old names before the package change, or new names after it.** `ApiDiff` fragments use old names: put them before `ChangePackage`. Mixing both styles works but is hard to review.
- **A move rule catches more than you think.** `capitalise → capitalize` followed by "move `StringUtils.capitalize(String)` to `WordUtils`" (meant for `capitaliseAllWords`) moved every `capitalize` call — the renamed ones and the ones that were already there — to a method that capitalizes *every word*. Move with a pattern on the *old* method name first, then rename; or rename straight into the final name.
- **Split enums.** When one enum is split into several (`JsonParser.Feature` → `StreamReadFeature` + `JsonReadFeature`), replace each constant with `ReplaceConstantWithAnotherConstant` first, then `ChangeType` the enum type for the remaining references (parameters, variables).

## Things easy to forget

- **Moved types.** While busy with renamed methods it is easy to skip the plain moves (`JsonFactory` → `...core.json.JsonFactory`); `safe-types.yml` lists them all.
- **Method references.** `node::fields` is not a method invocation; an imperative recipe that only visits `J.MethodInvocation` leaves it. Declarative `ChangeMethodName` handles member references.
- **Constants and enum values** in `switch` labels and annotations, not only in expressions.
- **Factory types.** `new ObjectMapper(new YAMLFactory())` became illegal: the format-specific `new YAMLMapper()` is required.

## Exceptions that became unchecked

When a library's exceptions stop being checked (Jackson 3: `JacksonException extends RuntimeException`, no `IOException` on its methods):

- `catch (IOException e)` around code where only the library threw it does not compile ("never thrown"). Catch the library's base exception instead; if another clause already catches it, drop the `IOException` clause.
- To decide "only the library threw it", look at everything in the `try`: calls, `new`, `throw`, **resource initializers** (`try (Stream<Path> s = Files.walk(p))` throws `IOException`) and resource `close()`. Skip lambda bodies and nested classes.
- Code that passes the caught exception on as an `IOException` (`new UncheckedIOException(e)`) stops compiling once the catch type changes: wrap it (`new UncheckedIOException(new IOException(e))`) or keep the old catch type there.
- Overrides of library methods (`serialize(...) throws IOException`) may not declare a broader checked exception than the new base method: remove it from `throws` when the body has no other source of it.

## Mutable objects that became immutable

When configuration moved from setters to a builder (`ObjectMapper`, factories):

- Fold creation and the setter calls that follow it into one builder chain — in a method, in a constructor, and for a field plus the initializer block after it.
- Setter names change on the builder (`registerModule` → `addModule`, `setDateFormat` → `defaultDateFormat`); some change shape (`setSerializationInclusion(x)` → `changeDefaultPropertyInclusion(i -> i.withValueInclusion(x).withContentInclusion(x))` — the old setter set *both*, check what each old setter really did).
- Setter calls on an existing non-final instance: `m = m.rebuild()....build()`. On a final field that is not created in place: leave the call and name it in the description.
- Settings that were removed (`MapperFeature.AUTO_DETECT_FIELDS`) may map to a different builder call (visibility); look in the migration guide.

## Changed return types

`JsonNode.fields()` returned an `Iterator`, its successor `properties()` returns a `Set`; `values()` kept its name but went from `Iterator` to `Collection`. A rename alone fails to compile wherever the result was used as an `Iterator`: append `.iterator()`, and make the recipe idempotent (it must not append it twice on the next cycle).

## Build files

A recipe run passing tells nothing about the build; only a real build does. Seen in practice:

- **A BOM moved to a new group id** while some modules still declared old-group artifacts without a version: the versions came from the old BOM and are gone.
- **Java version raised** by turning `<source>${compileSource}</source>` into `<release>${compileSource}</release>` and leaving `compileSource=1.8`. Check where the Java version is really defined (properties, plugin configuration, parent pom, toolchains, CI).
- **One version property for several artifacts** (`${jackson.version}` used for core *and* annotations, which stayed on 2.x): changing the property breaks the other artifact. Give the moved artifacts their own version.
- **Version selectors** like `3.x` resolve to the newest release at run time; pin the version you tested with when reproducibility matters.

## Behaviour changes in defaults

`ApiDiff` lists features whose default flipped (Jackson 3 changed 18, among them `FAIL_ON_UNKNOWN_PROPERTIES`, `WRITE_DATES_AS_TIMESTAMPS`, `SORT_PROPERTIES_ALPHABETICALLY`). Do not flip them back silently in the recipe — teams usually want the new defaults — but list every one in the description, and look at them first when the service's tests fail after the migration. If the library offers a compatibility switch (`builderWithJackson2Defaults()`), name it in the migration notes.

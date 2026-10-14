import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Source-compatibility diff of two versions of a library, made for writing an OpenRewrite migration recipe.
 * Reads only the jars (no sources needed) and answers: which types moved or were renamed, which members
 * no longer accept the old calls and what could replace them, which enum constants moved to other enums,
 * which feature defaults changed, which checked exceptions disappeared, which kept members became
 * deprecated, and which Java version the new version needs.
 *
 * <pre>
 * java ApiDiff.java --old a.jar,b.jar --new c.jar,d.jar --map old.pkg=new.pkg[,old2=new2]
 *                   [--types renames.txt] [--cp extra.jar,...] [--out DIR]
 * </pre>
 *
 * --old/--new: every jar of the library in that version (all artifacts its users depend on).
 * --map: package renames; use the same package on both sides when the package did not change (a.b=a.b).
 * --types: your decisions about renamed types, one "old.Fqn=new.Fqn" per line ($ for nested types);
 *          run again with it, and members of those types are compared too.
 * --cp: third-party jars the library needs to load its classes (both versions), e.g. snakeyaml.
 * --out: directory for report.md and YAML fragments (default: api-diff/ in the current directory).
 *
 * Output: a short summary on stdout; in --out the full report.md and recipe fragments that use the OLD
 * type names (put them before the package change in the recipe):
 *   safe-types.yml          ChangeType for moved types and for the renames given in --types
 *   safe-constants.yml      ReplaceConstantWithAnotherConstant for enum constants found in another enum
 *   review-renames.yml      ChangeMethodName candidates chosen by signature and name similarity - every
 *                           entry needs confirmation (Javadoc of the new method, or a probe), never paste blindly
 * Everything here is a candidate list: names and signatures say nothing about behaviour. Needs JDK 17+.
 */
public class ApiDiff {

    static final List<String[]> MAPS = new ArrayList<>();
    static final Map<String, String> TYPE_MAP = new TreeMap<>();
    static ClassLoader newLoader;
    static Map<String, Class<?>> newTypes;

    public static void main(String[] args) throws Exception {
        Map<String, String> a = parseArgs(args);
        if (!a.containsKey("old") || !a.containsKey("new") || !a.containsKey("map")) {
            System.err.println("usage: java ApiDiff.java --old a.jar,... --new b.jar,... --map old.pkg=new.pkg,... [--types f] [--cp jars] [--out dir]");
            System.exit(2);
        }
        for (String m : a.get("map").split(",")) MAPS.add(m.trim().split("="));
        if (a.containsKey("types")) {
            for (String l : Files.readAllLines(Path.of(a.get("types")))) {
                if (l.isBlank() || l.startsWith("#") || !l.contains("=")) continue;
                TYPE_MAP.put(l.split("=")[0].trim(), l.split("=")[1].trim());
            }
        }
        List<String> extra = a.containsKey("cp") ? List.of(a.get("cp").split(",")) : List.of();
        Path out = Path.of(a.getOrDefault("out", "api-diff"));
        Files.createDirectories(out);

        List<String> oldJars = List.of(a.get("old").split(",")), newJars = List.of(a.get("new").split(","));
        Map<String, Class<?>> oldTypes = load(oldJars, extra, true);
        newTypes = load(newJars, extra, false);
        if (newTypes.isEmpty() || oldTypes.isEmpty()) {
            System.err.println("no public types found in scope: check --map against the jars' packages");
            System.exit(1);
        }
        newLoader = newTypes.values().iterator().next().getClassLoader();

        Report r = new Report();
        r.h1("API diff");
        r.line("old: " + names(oldJars) + "  ->  new: " + names(newJars));
        int oldJava = javaVersion(oldJars), newJava = javaVersion(newJars);
        r.line("class files: Java " + oldJava + " -> Java " + newJava + (newJava > oldJava ? "  (users must compile with --release " + newJava + " or later)" : ""));

        // --- pair types
        Map<String, List<String>> newBySimple = new TreeMap<>();
        newTypes.keySet().forEach(n -> newBySimple.computeIfAbsent(simple(n), k -> new ArrayList<>()).add(n));
        Map<String, String> pairs = new TreeMap<>();
        Map<String, String> moved = new TreeMap<>();
        List<String> missing = new ArrayList<>();
        for (String o : oldTypes.keySet()) {
            String m = map(o);
            if (newTypes.containsKey(m)) {
                pairs.put(o, m);
                if (TYPE_MAP.containsKey(o)) moved.put(o, m);
            } else if (newBySimple.getOrDefault(simple(o), List.of()).size() == 1) {
                String n = newBySimple.get(simple(o)).get(0);
                pairs.put(o, n);
                moved.put(o, n);
            } else {
                missing.add(o);
            }
        }
        Set<String> claimed = new HashSet<>(pairs.values());
        TYPE_MAP.putAll(moved);   // compare signatures through moved types too

        r.h2("Summary");
        r.line(String.format("old types in scope %d, new %d; same name after package map %d, moved or renamed %d, missing %d, new only %d",
                oldTypes.size(), newTypes.size(), pairs.size() - moved.size(), moved.size(), missing.size(), newTypes.size() - claimed.size()));

        r.h2("Moved or renamed types (safe-types.yml)");
        moved.forEach((o, n) -> r.line("- " + o + " -> " + n));

        r.h2("Missing types, with candidates by similarity of their methods (* = exists only in the new version)");
        Map<String, Set<String>> shapes = new HashMap<>();
        for (String o : missing) {
            Set<String> os = shape(oldTypes.get(o));
            List<Map.Entry<String, Double>> best = new ArrayList<>();
            for (String n : newTypes.keySet()) {
                double j = jaccard(os, shapes.computeIfAbsent(n, k -> shape(newTypes.get(k))));
                if (j >= 0.3) best.add(Map.entry(n, j + (claimed.contains(n) ? 0 : 0.05)));
            }
            best.sort((x, y) -> Double.compare(y.getValue(), x.getValue()));
            String sup = superFallback(oldTypes.get(o));
            r.line("- " + o + " " + kind(oldTypes.get(o)) + " [" + os.size() + " methods]"
                    + (best.isEmpty() ? "" : "\n    ~ " + best.stream().limit(3)
                    .map(e -> e.getKey() + (claimed.contains(e.getKey()) ? "" : "*") + String.format(" %.2f", Math.min(1, e.getValue())))
                    .collect(Collectors.joining("; ")))
                    + (sup == null ? "" : "\n    superclass still exists: " + sup));
        }

        r.h2("Incompatible members of paired types");
        List<String[]> renames = new ArrayList<>();          // old type, old method sig pattern, new name, score, note
        List<String> deprecatedInNew = new ArrayList<>();
        int droppedChecked = 0, keptChecked = 0;
        for (Map.Entry<String, String> e : pairs.entrySet()) {
            Class<?> oc = oldTypes.get(e.getKey()), nc = newTypes.get(e.getValue());
            List<String> lines = new ArrayList<>();
            for (Executable m : members(oc)) {
                Executable hit = find(m, nc);
                if (hit != null) {
                    if (Arrays.stream(m.getExceptionTypes()).anyMatch(ApiDiff::checked)) {
                        if (Arrays.stream(hit.getExceptionTypes()).anyMatch(ApiDiff::checked)) keptChecked++; else droppedChecked++;
                    }
                    if (hit.isAnnotationPresent(Deprecated.class) && !m.isAnnotationPresent(Deprecated.class) && Modifier.isPublic(hit.getModifiers())) {
                        deprecatedInNew.add(e.getKey() + "#" + sig(m));
                    }
                    continue;
                }
                String cand = candidates(m, nc, renames, e.getKey());
                lines.add("- " + sig(m) + cand);
            }
            for (Field f : oc.getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers()) || f.isEnumConstant()) continue;
                try { nc.getField(f.getName()); } catch (NoSuchFieldException x) { lines.add("- field " + f.getName()); }
            }
            String so = supertypes(oc, true), sn = supertypes(nc, false);
            if (!so.equals(sn)) lines.add("! supertypes " + so + " -> " + sn);
            if (checked(oc) && Throwable.class.isAssignableFrom(nc) && !checked(nc)) lines.add("! was a checked exception, now unchecked");
            if (!lines.isEmpty()) {
                r.line("\n### " + e.getKey() + (e.getValue().equals(map(e.getKey())) ? "" : "  (new: " + e.getValue() + ")"));
                lines.forEach(r::line);
            }
        }

        r.h2("Enum constants");
        List<String[]> constMoves = new ArrayList<>();
        Set<String> defaults = new LinkedHashSet<>();
        Map<String, List<Class<?>>> newEnumsByConstant = new HashMap<>();
        for (Class<?> c : newTypes.values()) {
            if (!c.isEnum()) continue;
            for (Object k : c.getEnumConstants()) newEnumsByConstant.computeIfAbsent(((Enum<?>) k).name(), x -> new ArrayList<>()).add(c);
        }
        for (Class<?> oc : oldTypes.values()) {
            if (!oc.isEnum()) continue;
            Class<?> nc = pairs.containsKey(oc.getName()) ? newTypes.get(pairs.get(oc.getName())) : null;
            Set<String> inSame = nc != null && nc.isEnum()
                    ? Arrays.stream(nc.getEnumConstants()).map(x -> ((Enum<?>) x).name()).collect(Collectors.toSet()) : Set.of();
            for (Object k : oc.getEnumConstants()) {
                String name = ((Enum<?>) k).name();
                Boolean oldDefault = enabledByDefault(k);
                List<Class<?>> where = inSame.contains(name) ? List.of(nc) : newEnumsByConstant.getOrDefault(name, List.of());
                if (!inSame.contains(name)) {
                    if (where.size() == 1) {
                        constMoves.add(new String[]{dotted(oc.getName()) + "." + name, dotted(where.get(0).getName()) + "." + name});
                        r.line("- " + simpleNested(oc) + "." + name + " -> " + dotted(where.get(0).getName()) + "." + name);
                    } else {
                        r.line("- " + simpleNested(oc) + "." + name + (where.isEmpty() ? " removed (look for a renamed constant: "
                                + similarConstants(name, nc) + ")" : " found in several enums: " + where.stream().map(Class::getSimpleName).toList()));
                    }
                }
                if (oldDefault != null && where.size() == 1) {
                    Object nk = Arrays.stream(where.get(0).getEnumConstants()).filter(x -> ((Enum<?>) x).name().equals(name)).findFirst().orElse(null);
                    Boolean newDefault = nk == null ? null : enabledByDefault(nk);
                    if (newDefault != null && !newDefault.equals(oldDefault)) {
                        defaults.add(simpleNested(where.get(0)) + "." + name + ": " + oldDefault + " -> " + newDefault);
                    }
                }
            }
        }

        r.h2("Feature defaults that changed (behaviour change: list them in the recipe description)");
        defaults.forEach(d -> r.line("- " + d));

        r.h2("Checked exceptions");
        r.line(droppedChecked + " kept members no longer declare a checked exception, " + keptChecked + " still do.");
        if (droppedChecked > 0) {
            r.line("Expect `catch (IOException)` that only the library could trigger to stop compiling, and `throws` clauses on overrides of library methods to be too broad.");
        }

        r.h2("Kept members that are deprecated in the new version (compile, but people migrate them; read their Javadoc)");
        deprecatedInNew.stream().limit(300).forEach(d -> r.line("- " + d));

        r.h2("Method rename candidates (review-renames.yml) - confirm each one");
        renames.forEach(x -> r.line("- " + x[0] + " " + x[1] + " -> " + x[2] + "  (score " + x[3] + ")" + x[4]));

        Files.writeString(out.resolve("report.md"), r.text());
        writeYaml(out.resolve("safe-types.yml"), "Moved or renamed types (old names; run before the package change)",
                moved.entrySet().stream().map(e -> String.format("  - org.openrewrite.java.ChangeType:%n      oldFullyQualifiedTypeName: %s%n      newFullyQualifiedTypeName: %s%n",
                        e.getKey(), e.getValue())).toList());
        writeYaml(out.resolve("safe-constants.yml"), "Enum constants that exist, under the same name, in exactly one other enum of the new version",
                constMoves.stream().map(c -> String.format("  - org.openrewrite.java.ReplaceConstantWithAnotherConstant:%n      existingFullyQualifiedConstantName: %s%n      fullyQualifiedConstantName: %s%n",
                        c[0], c[1])).toList());
        writeYaml(out.resolve("review-renames.yml"), "CANDIDATES chosen by signature and name similarity. Confirm every entry with the Javadoc of the new method\n# (or a probe that calls both versions) before using it: same arguments do not mean same behaviour.",
                renames.stream().map(x -> String.format("  # score %s%s%n  - org.openrewrite.java.ChangeMethodName:%n      methodPattern: %s %s%n      newMethodName: %s%n      matchOverrides: true%n",
                        x[3], x[4].replace("\n", " "), x[0], x[1], x[2])).toList());

        System.out.printf("Java %d -> %d | types: %d moved/renamed, %d missing | %d incompatible types | %d enum constants moved | %d defaults changed | %d checked exceptions dropped | %d deprecated in new | %d rename candidates%n",
                oldJava, newJava, moved.size(), missing.size(), r.count("### "), constMoves.size(), defaults.size(), droppedChecked, deprecatedInNew.size(), renames.size());
        System.out.println("report: " + out.resolve("report.md").toAbsolutePath() + "; fragments: safe-types.yml, safe-constants.yml, review-renames.yml");
    }

    // ---------------------------------------------------------------- candidates

    static String candidates(Executable m, Class<?> nc, List<String[]> renames, String oldType) {
        if (!(m instanceof Method om)) {
            String ctors = Arrays.stream(nc.getConstructors()).map(k -> params(k.getParameterTypes())).collect(Collectors.joining("; "));
            String factories = Arrays.stream(nc.getMethods()).filter(x -> Modifier.isStatic(x.getModifiers()) && nc.isAssignableFrom(x.getReturnType()))
                    .map(x -> x.getName() + params(x.getParameterTypes())).distinct().limit(4).collect(Collectors.joining("; "));
            return "\n    ctors: " + ctors + (factories.isEmpty() ? "" : "\n    static factories: " + factories);
        }
        boolean st = Modifier.isStatic(om.getModifiers());
        List<Method> all = allMethods(nc).stream().filter(x -> Modifier.isPublic(x.getModifiers()) && x.getDeclaringClass() != Object.class).toList();
        List<String> sameName = all.stream().filter(x -> x.getName().equals(om.getName())).map(ApiDiff::sig).distinct().limit(3).toList();
        List<Method> sameArgs = all.stream().filter(x -> Modifier.isStatic(x.getModifiers()) == st && x.getParameterCount() == om.getParameterCount()
                && !x.getName().equals(om.getName()) && accepts(x, om.getParameterTypes()) && returnCompatible(om, x)).toList();
        Map<String, Integer> scored = new LinkedHashMap<>();
        for (Method x : sameArgs) scored.merge(x.getName(), score(om, x), Math::max);
        List<Map.Entry<String, Integer>> ranked = scored.entrySet().stream().sorted((p, q) -> q.getValue() - p.getValue()).toList();
        StringBuilder b = new StringBuilder();
        if (!sameName.isEmpty()) b.append("\n    same name, other signature: ").append(String.join("; ", sameName));
        if (!ranked.isEmpty()) b.append("\n    same arguments: ").append(ranked.stream().limit(4).map(x -> x.getKey() + "(" + x.getValue() + ")").collect(Collectors.joining("; ")));
        List<String> otherReturn = all.stream().filter(x -> Modifier.isStatic(x.getModifiers()) == st && x.getParameterCount() == om.getParameterCount()
                        && !x.getName().equals(om.getName()) && accepts(x, om.getParameterTypes()) && !returnCompatible(om, x) && score(om, x) >= 50)
                .map(ApiDiff::sig).distinct().limit(3).toList();
        if (!otherReturn.isEmpty()) b.append("\n    similar name, different return type (rewrite by hand or imperative): ").append(String.join("; ", otherReturn));
        if (Modifier.isPublic(om.getModifiers()) && !ranked.isEmpty() && ranked.get(0).getValue() >= 50 && (ranked.size() == 1 || ranked.get(0).getValue() > ranked.get(1).getValue())) {
            String note = ranked.size() > 1 && ranked.get(1).getValue() >= 40 ? " - close second: " + ranked.get(1).getKey() : "";
            if (existedBefore(om.getDeclaringClass(), ranked.get(0).getKey())) note += " - this name existed in the old version too: compare the two methods' Javadoc";
            // look-alikes with the same arguments (transformedList / transformingList): the name cannot decide between them
            List<String> topWords = words(ranked.get(0).getKey());
            List<String> lookAlikes = ranked.stream().skip(1).map(Map.Entry::getKey)
                    .filter(n -> words(n).stream().anyMatch(w -> w.length() > 2 && topWords.contains(w))).limit(3).toList();
            if (!lookAlikes.isEmpty()) note += " - LOOK-ALIKES with the same arguments: " + String.join(", ", lookAlikes) + " (decide by Javadoc or a probe)";
            renames.add(new String[]{oldType, om.getName() + pattern(om.getParameterTypes()), ranked.get(0).getKey(), String.valueOf(ranked.get(0).getValue()), note});
        }
        return b.toString();
    }

    /**
     * Word overlap of camel-case names (0..100), +10 when the return types correspond. A static factory named
     * after its class (ListOrderedMap.listOrderedMap) scores 80: a common convention for replacing
     * decorate()/getInstance()/of() style factories.
     */
    static int score(Method o, Method n) {
        List<String> wo = words(o.getName()), wn = words(n.getName());
        long common = wo.stream().filter(wn::contains).count();
        int s = (int) (100 * common / Math.max(wo.size(), wn.size()));
        Class<?> r = translate(o.getReturnType());
        if (r != null && (r == n.getReturnType() || n.getReturnType().isAssignableFrom(r) || r.isAssignableFrom(n.getReturnType()))) s += 10;
        if (Modifier.isStatic(n.getModifiers()) && n.getName().equalsIgnoreCase(n.getDeclaringClass().getSimpleName())) s = Math.max(s, 80);
        return Math.min(s, 100);
    }

    /** The new method can stand where the old one's result was used: void stays void, otherwise the types correspond. */
    static boolean returnCompatible(Method o, Method n) {
        Class<?> r = translate(o.getReturnType());
        if (o.getReturnType() == void.class) return true;      // result unused by definition
        if (n.getReturnType() == void.class || r == null) return false;
        return r == n.getReturnType() || r.isAssignableFrom(n.getReturnType()) || n.getReturnType().isAssignableFrom(r)
                || (r.isPrimitive() && n.getReturnType().isPrimitive());
    }

    static boolean existedBefore(Class<?> oldClass, String name) {
        return allMethods(oldClass).stream().anyMatch(m -> m.getName().equals(name) && Modifier.isPublic(m.getModifiers()));
    }

    static List<String> words(String s) { return Arrays.stream(s.split("(?=[A-Z])")).map(String::toLowerCase).toList(); }

    static String similarConstants(String name, Class<?> e) {
        if (e == null || !e.isEnum()) return "no enum with that name";
        List<String> w = Arrays.asList(name.split("_"));
        return Arrays.stream(e.getEnumConstants()).map(x -> ((Enum<?>) x).name())
                .filter(n -> Arrays.stream(n.split("_")).filter(w::contains).count() * 2 >= w.size())
                .limit(3).collect(Collectors.joining(", "));
    }

    static Boolean enabledByDefault(Object constant) {
        try {
            Method m = constant.getClass().getMethod("enabledByDefault");
            if (m.getReturnType() != boolean.class) return null;
            m.setAccessible(true);
            return (Boolean) m.invoke(constant);
        } catch (Throwable t) {
            return null;
        }
    }

    static String superFallback(Class<?> c) {
        Class<?> s = c.getSuperclass();
        if (s == null || s == Object.class) return null;
        Class<?> t = translate(s);
        return t == null ? null : t.getName();
    }

    // ---------------------------------------------------------------- compatibility

    static Executable find(Executable m, Class<?> nc) {
        boolean st = Modifier.isStatic(m.getModifiers());
        Collection<? extends Executable> cands = m instanceof Method ? allMethods(nc) : Arrays.asList(nc.getDeclaredConstructors());
        for (Executable e : cands) {
            int mod = e.getModifiers();
            if (!(Modifier.isPublic(mod) || Modifier.isProtected(mod))) continue;
            if (m instanceof Method && (!e.getName().equals(m.getName()) || Modifier.isStatic(mod) != st)) continue;
            if (accepts(e, m.getParameterTypes())) return e;
        }
        return null;
    }

    static boolean accepts(Executable e, Class<?>[] args) {
        Class<?>[] p = e.getParameterTypes();
        if (p.length == args.length && IntStream.range(0, p.length).allMatch(i -> assignable(args[i], p[i]))) return true;
        if (e.isVarArgs() && args.length >= p.length - 1) {
            for (int i = 0; i < p.length - 1; i++) if (!assignable(args[i], p[i])) return false;
            Class<?> el = p[p.length - 1].getComponentType();
            for (int i = p.length - 1; i < args.length; i++) if (!assignable(args[i], el)) return false;
            return true;
        }
        return false;
    }

    static boolean assignable(Class<?> oldArg, Class<?> newParam) {
        Class<?> a = translate(oldArg);
        if (a == null) return false;
        if (newParam.isAssignableFrom(a)) return true;
        String w = "byte short char int long float double";
        return a.isPrimitive() && newParam.isPrimitive() && w.indexOf(a.getName()) >= 0 && w.indexOf(newParam.getName()) > w.indexOf(a.getName());
    }

    /** The new-version class an old-version class corresponds to (same class for JDK and third-party types). */
    static Class<?> translate(Class<?> c) {
        if (c.isArray()) {
            Class<?> e = translate(c.getComponentType());
            return e == null ? null : e.arrayType();
        }
        if (!inOldScope(c.getName())) return c;
        try {
            return Class.forName(map(c.getName()), false, newLoader);
        } catch (Throwable t) {
            return null;
        }
    }

    static boolean checked(Class<?> t) {
        return Throwable.class.isAssignableFrom(t) && !RuntimeException.class.isAssignableFrom(t) && !Error.class.isAssignableFrom(t);
    }

    // ---------------------------------------------------------------- reflection helpers

    static List<Executable> members(Class<?> c) {
        List<Executable> r = new ArrayList<>();
        try {
            for (Method m : c.getDeclaredMethods()) {
                int mod = m.getModifiers();
                if ((Modifier.isPublic(mod) || Modifier.isProtected(mod)) && !m.isSynthetic() && !m.isBridge()) r.add(m);
            }
            for (Constructor<?> k : c.getDeclaredConstructors()) {
                int mod = k.getModifiers();
                if ((Modifier.isPublic(mod) || Modifier.isProtected(mod)) && !k.isSynthetic()) r.add(k);
            }
        } catch (Throwable t) {
            // a class whose dependencies are not on the classpath: add them with --cp
        }
        r.sort(Comparator.comparing(ApiDiff::sig));
        return r;
    }

    static Collection<Method> allMethods(Class<?> c) {
        List<Method> r = new ArrayList<>();
        Deque<Class<?>> q = new ArrayDeque<>(List.of(c));
        Set<Class<?>> seen = new HashSet<>();
        while (!q.isEmpty()) {
            Class<?> x = q.poll();
            if (!seen.add(x)) continue;
            try { r.addAll(Arrays.asList(x.getDeclaredMethods())); } catch (Throwable t) { }
            if (x.getSuperclass() != null) q.add(x.getSuperclass());
            q.addAll(Arrays.asList(x.getInterfaces()));
        }
        return r;
    }

    static Set<String> shape(Class<?> c) {
        Set<String> s = new HashSet<>();
        try {
            for (Method m : c.getDeclaredMethods()) if (Modifier.isPublic(m.getModifiers()) && !m.isSynthetic()) s.add(m.getName() + "/" + m.getParameterCount());
        } catch (Throwable t) { }
        return s;
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        long i = a.stream().filter(b::contains).count();
        return i / (double) (a.size() + b.size() - i);
    }

    static String supertypes(Class<?> c, boolean old) {
        List<String> s = new ArrayList<>();
        if (c.getSuperclass() != null && c.getSuperclass() != Object.class) s.add("extends " + simple(old ? map(c.getSuperclass().getName()) : c.getSuperclass().getName()));
        Arrays.stream(c.getInterfaces()).map(i -> simple(old ? map(i.getName()) : i.getName())).sorted().forEach(s::add);
        return s.toString();
    }

    static String sig(Executable m) {
        String p = params(m.getParameterTypes());
        if (m instanceof Method x) return (Modifier.isStatic(x.getModifiers()) ? "static " : "") + x.getReturnType().getSimpleName() + " " + x.getName() + p;
        return "<init>" + p;
    }

    static String params(Class<?>[] p) { return Arrays.stream(p).map(Class::getSimpleName).collect(Collectors.joining(",", "(", ")")); }

    /** OpenRewrite method pattern arguments: fully qualified, nested types with $ replaced by '.'. */
    static String pattern(Class<?>[] p) {
        return Arrays.stream(p).map(c -> c.getTypeName().replace('$', '.')).collect(Collectors.joining(", ", "(", ")"));
    }

    static String kind(Class<?> c) {
        return c.isInterface() ? "(interface)" : c.isEnum() ? "(enum)" : Modifier.isAbstract(c.getModifiers()) ? "(abstract)" : "";
    }

    static String simple(String n) { return n.substring(n.lastIndexOf('.') + 1); }
    static String dotted(String n) { return n.replace('$', '.'); }
    static String simpleNested(Class<?> c) { return dotted(simple(c.getName())); }

    static boolean inOldScope(String n) { return MAPS.stream().anyMatch(m -> n.startsWith(m[0] + ".")); }
    static boolean inNewScope(String n) { return MAPS.stream().anyMatch(m -> n.startsWith(m[1] + ".")); }

    static String map(String n) {
        if (TYPE_MAP.containsKey(n)) return TYPE_MAP.get(n);
        String best = null;
        for (String[] m : MAPS) if (n.startsWith(m[0] + ".") && (best == null || m[0].length() > best.indexOf('='))) best = m[0] + "=" + m[1];
        if (best == null) return n;
        String[] m = best.split("=");
        return m[1] + n.substring(m[0].length());
    }

    static Map<String, Class<?>> load(List<String> jars, List<String> extra, boolean old) throws IOException {
        List<URL> urls = new ArrayList<>();
        for (String j : jars) urls.add(new File(j.trim()).toURI().toURL());
        for (String j : extra) urls.add(new File(j.trim()).toURI().toURL());
        URLClassLoader cl = new URLClassLoader(urls.toArray(URL[]::new), ApiDiff.class.getClassLoader());
        Map<String, Class<?>> r = new TreeMap<>();
        for (String j : jars) {
            try (JarFile jf = new JarFile(j.trim())) {
                for (JarEntry e : Collections.list(jf.entries())) {
                    String n = e.getName();
                    if (!n.endsWith(".class") || n.startsWith("META-INF") || n.contains("module-info") || n.contains("package-info")) continue;
                    String cn = n.substring(0, n.length() - 6).replace('/', '.');
                    if (old ? !inOldScope(cn) : !inNewScope(cn)) continue;
                    try {
                        Class<?> c = Class.forName(cn, false, cl);
                        boolean visible = Modifier.isPublic(c.getModifiers()) && !c.isAnonymousClass() && !c.isSynthetic()
                                && (c.getEnclosingClass() == null || Modifier.isStatic(c.getModifiers()) || c.isInterface() || c.isEnum());
                        if (visible) r.put(cn, c);
                    } catch (Throwable t) {
                        // not loadable without its dependencies
                    }
                }
            }
        }
        return r;
    }

    /** Highest class-file major version in the jars, as a Java version. */
    static int javaVersion(List<String> jars) throws IOException {
        int max = 0;
        for (String j : jars) {
            try (JarFile jf = new JarFile(j.trim())) {
                for (JarEntry e : Collections.list(jf.entries())) {
                    if (!e.getName().endsWith(".class") || e.getName().contains("module-info") || e.getName().startsWith("META-INF/versions")) continue;
                    try (InputStream in = jf.getInputStream(e)) {
                        byte[] h = in.readNBytes(8);
                        if (h.length == 8) max = Math.max(max, ((h[6] & 0xff) << 8) | (h[7] & 0xff));
                    }
                }
            }
        }
        return max - 44;
    }

    static String names(List<String> jars) { return jars.stream().map(j -> Path.of(j.trim()).getFileName().toString()).collect(Collectors.joining(", ")); }

    static void writeYaml(Path p, String comment, List<String> entries) throws IOException {
        Files.writeString(p, "# " + comment + "\n" + String.join("", entries));
    }

    static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) m.put(args[i].replaceFirst("^--", ""), args[i + 1]);
        return m;
    }

    static class Report {
        final StringBuilder b = new StringBuilder();
        void h1(String s) { b.append("# ").append(s).append("\n\n"); }
        void h2(String s) { b.append("\n## ").append(s).append("\n\n"); }
        void line(String s) { b.append(s).append('\n'); }
        int count(String prefix) { return (int) b.toString().lines().filter(l -> l.startsWith(prefix)).count(); }
        String text() { return b.toString(); }
    }
}

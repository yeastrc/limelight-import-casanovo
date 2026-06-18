package org.yeastrc.limelight.xml.casanovo.reader;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses a Casanovo YAML config file. The converter uses two pieces of information from it:
 * <ul>
 *   <li>the {@code residues} map (amino-acid / modification token &rarr; monoisotopic mass), and</li>
 *   <li>the {@code allowed_fixed_mods} declaration, which identifies the modifications that are fixed
 *       and therefore emitted as static modifications rather than per-peptide variable modifications.</li>
 * </ul>
 */
public class ConfigParser {

    private final Map<String, BigDecimal> residues;

    /** residue symbol (e.g. {@code "C"}, or {@code "nterm"}) &rarr; fixed-mod token (e.g. {@code "C[Carbamidomethyl]"}). */
    private final Map<String, String> fixedMods;

    public ConfigParser(String filePath) throws Exception {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));

        Map<String, Object> config;
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
            config = yaml.load(inputStream);
        }

        if (config == null || !config.containsKey("residues")) {
            throw new Exception("The config file must contain a 'residues' parameter.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rawResidues = (Map<String, Object>) config.get("residues");
        this.residues = Collections.unmodifiableMap(rawResidues.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new BigDecimal(e.getValue().toString()),
                        (a, b) -> b,
                        LinkedHashMap::new)));

        this.fixedMods = Collections.unmodifiableMap(parseModDeclaration(config.get("allowed_fixed_mods")));
    }

    /**
     * Parse a Casanovo mod-declaration string of the form {@code "C:C[Carbamidomethyl],nterm:[Acetyl]-"}
     * into a map of residue symbol &rarr; mod token.
     */
    private static Map<String, String> parseModDeclaration(Object declaration) {
        Map<String, String> result = new LinkedHashMap<>();
        if (declaration == null) {
            return result;
        }
        String text = declaration.toString().trim();
        if (text.isEmpty()) {
            return result;
        }
        for (String entry : text.split(",")) {
            entry = entry.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int colon = entry.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String residue = entry.substring(0, colon).trim();
            String token = entry.substring(colon + 1).trim();
            if (!residue.isEmpty() && !token.isEmpty()) {
                result.put(residue, token);
            }
        }
        return result;
    }

    public Map<String, BigDecimal> getResidues() {
        return residues;
    }

    /** Map of residue symbol (e.g. {@code "C"}, {@code "nterm"}) to its fixed-modification token (e.g. {@code "C[Carbamidomethyl]"}). */
    public Map<String, String> getFixedMods() {
        return fixedMods;
    }

    /** The set of fixed-modification tokens (e.g. {@code "C[Carbamidomethyl]"}), used to skip fixed mods during variable-mod parsing. */
    public Set<String> getFixedModTokens() {
        return Collections.unmodifiableSet(new HashSet<>(fixedMods.values()));
    }
}

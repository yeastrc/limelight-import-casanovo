package org.yeastrc.limelight.xml.casanovo.reader;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigParser {
    private Map<String, Object> config;

    public ConfigParser(String filePath) throws Exception {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));

        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
            this.config = yaml.load(inputStream);
        }

        if (!this.config.containsKey("residues")) {
            throw new Exception("The config file must contain a 'residues' parameter.");
        }

        // Convert residues to BigDecimal
        Map<String, Object> residues = (Map<String, Object>) this.config.get("residues");
        Map<String, BigDecimal> bigDecimalResidues = residues.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new BigDecimal(e.getValue().toString())
                ));
        this.config.put("residues", bigDecimalResidues);
    }

    public Object getParameter(String key) {
        return this.config.get(key);
    }

    public Map<String, Object> getAllParameters() {
        return this.config;
    }

    public Map<String, BigDecimal> getResidues() {
        return (Map<String, BigDecimal>) this.config.get("residues");
    }
}
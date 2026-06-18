package org.yeastrc.limelight.xml.casanovo.utils;

import org.junit.jupiter.api.Test;
import org.yeastrc.limelight.xml.casanovo.reader.ConfigParser;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that static (fixed) modifications are derived from the config rather than hard-coded. */
class StaticModificationDerivationTest {

    @Test
    void cysteineCarbamidomethylDerivedFromConfig() throws Exception {
        File config = new File(StaticModificationDerivationTest.class.getClassLoader()
                .getResource("casanovo/casanovo-config.yaml").toURI());
        ConfigParser cp = new ConfigParser(config.getAbsolutePath());

        Map<String, BigDecimal> staticMods = StaticModificationUtils.getFixedModificationMasses(cp);

        assertTrue(staticMods.containsKey("C"), "expected a static modification on C");
        // residues["C[Carbamidomethyl]"] (160.030649) - standard C (103.009185) = 57.021464
        assertEquals(57.021464, staticMods.get("C").doubleValue(), 1e-6);
        assertEquals(1, staticMods.size(), "carbamidomethyl on C is the only fixed mod in this config");
    }
}

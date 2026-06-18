package org.yeastrc.limelight.xml.casanovo.integration;

import org.yeastrc.limelight.limelight_import.api.xml_dto.LimelightInput;
import org.yeastrc.limelight.limelight_import.api.xml_dto.Psm;
import org.yeastrc.limelight.limelight_import.api.xml_dto.ReportedPeptide;
import org.yeastrc.limelight.xml.casanovo.builder.XMLBuilder;
import org.yeastrc.limelight.xml.casanovo.objects.CasanovoResults;
import org.yeastrc.limelight.xml.casanovo.objects.ConversionParameters;
import org.yeastrc.limelight.xml.casanovo.objects.ConversionProgramInfo;
import org.yeastrc.limelight.xml.casanovo.objects.SearchMetadata;
import org.yeastrc.limelight.xml.casanovo.reader.ConfigParser;
import org.yeastrc.limelight.xml.casanovo.reader.ResultsParser;
import org.yeastrc.limelight.xml.casanovo.reader.SearchMetadataParser;
import org.yeastrc.limelight.xml.casanovo.utils.EstimatedFDRCalculator;
import org.yeastrc.limelight.xml.casanovo.utils.StaticModificationUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/** Shared helpers for the conversion integration/smoke tests: run the full pipeline to a temp file and
 *  navigate the resulting {@link LimelightInput} DTO tree. */
final class ConversionTestSupport {

    static final String SINGLE = "casanovo/single-file.mztab";
    static final String TWO_FILES = "casanovo/two-files.mztab";
    static final String MEDIUM = "casanovo/medium.mztab";
    static final String CONFIG = "casanovo/casanovo-config.yaml";

    private ConversionTestSupport() {}

    /** Run the full conversion pipeline on a classpath fixture and return the produced Limelight XML temp file. */
    static File convert(String mztabResource, String configResource) throws Throwable {
        return convert(resource(mztabResource), resource(configResource));
    }

    /** Run the full conversion pipeline on the given files and return the produced Limelight XML temp file. */
    static File convert(File mztab, File config) throws Throwable {
        ConfigParser configParser = new ConfigParser(config.getAbsolutePath());
        SearchMetadata searchMetadata = new SearchMetadataParser().parse(mztab.getAbsolutePath());
        CasanovoResults results = ResultsParser.getResults(mztab, configParser);
        EstimatedFDRCalculator.generateEstimatedFDRMap(results);
        Map<String, BigDecimal> staticModifications = StaticModificationUtils.getFixedModificationMasses(configParser);

        File out = File.createTempFile("limelight-casanovo-test", ".xml");
        out.deleteOnExit();

        ConversionParameters params = new ConversionParameters();
        params.setConfigFile(config);
        params.setMztabFile(mztab);
        params.setLimelightXMLOutputFile(out);
        params.setConversionProgramInfo(ConversionProgramInfo.createInstance("characterization-test"));

        new XMLBuilder().buildAndSaveXML(params, searchMetadata, results, staticModifications);
        return out;
    }

    static LimelightInput unmarshal(File xml) throws Exception {
        JAXBContext context = JAXBContext.newInstance(LimelightInput.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (LimelightInput) unmarshaller.unmarshal(xml);
    }

    static ReportedPeptide peptideByString(LimelightInput in, String reportedPeptideString) {
        for (ReportedPeptide rp : in.getReportedPeptides().getReportedPeptide()) {
            if (reportedPeptideString.equals(rp.getReportedPeptideString())) {
                return rp;
            }
        }
        return fail("no reported peptide with string '" + reportedPeptideString + "'");
    }

    static List<Psm> allPsms(LimelightInput in) {
        List<Psm> psms = new ArrayList<>();
        for (ReportedPeptide rp : in.getReportedPeptides().getReportedPeptide()) {
            if (rp.getPsms() != null) {
                psms.addAll(rp.getPsms().getPsm());
            }
        }
        return psms;
    }

    static File resource(String name) throws Exception {
        return new File(ConversionTestSupport.class.getClassLoader().getResource(name).toURI());
    }
}

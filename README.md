Casanovo to Limelight XML converter
===============================================

Use this program to convert the results of a Casanovo search to
limelight XML suitable for import into the limelight web application.

How To Run (Java option)
-------------------------
1. Download the [latest release](https://github.com/yeastrc/limelight-import-casanovo/releases).
2. Run the program ``java -jar casanovoToLimelightXML.jar`` with no arguments to see the possible parameters. Requires Java 8 or higher.

How To Run (Docker option)
---------------------------
You can alternatively run using Docker without installing Java or downloading the program. Run with a command
similar to:

``docker run --rm -it --user $(id -u):$(id -g) -v `pwd`:`pwd`  -w `pwd` mriffle/casanovo-to-limelight:latest casanovoToLimelightXML``

with no arguments to see the possible parameters.

Command line documentation
---------------------------

```
java -jar casanovoToLimelightXML.jar [-hvV] -c=<configFile> -m=<mztabFile>
                                     -o=<outFile>

Description:

Convert the results of a Casanovo analysis to a Limelight XML file suitable for
import into Limelight.

More info at: https://github.com/yeastrc/limelight-import-casanovo

Options:
  -m, --mztab=<mztabFile>    Full path to the Casanovo results file (ends with .
                               mztab). E.g., /data/results/results.mztab
  -c, --config_yaml=<configFile>
                             Full path to configuration file. E.g., ./casanovo.yaml
  -o, --out_file=<outFile>   Full path to use for the Limelight XML output file. E.
                               g., /data/my_analysis/crux.limelight.xml
  -v, --verbose              If this parameter is present, error messages will
                               include a full stacktrace. Helpful for debugging.
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.
```

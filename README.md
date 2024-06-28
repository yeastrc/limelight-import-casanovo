CONGA to Limelight XML converter
===============================================
Convert the results of a [CONGA](https://github.com/freejstone/CONGA) open-search proteomics analysis to 
Limelight XML suitable for import into [Limelight](https://limelight-ms.org/).

## How to Run
This converter can be run in one of two ways: 1) As a Java program or 2) as a Docker container.

### Run as a Java program

1. Ensure Java 8 or higher is installed on your system. See: https://www.java.com/en/download/ for more details.
2. Download the latest release of this converter from: https://github.com/yeastrc/limelight-import-conga/releases
3. Run the program with: `java -jar congaToLimelightXML.jar` with no arguments to see command line parameters.

#### Usage example:

```bash
java -jar ~/congaToLimelightXML.jar \
    -f /path/to/proteins.fasta \
    -t ./conga.target_mods.txt \
    -l ./conga.log.txt \
    -o conga.limelight.xml
```

### Run as a Docker container

1. Ensure Docker is installed on your system. For more information see: https://docs.docker.com/get-docker/
2. Run the program with:  `docker run --rm -it mriffle/conga-to-limelight:latest` with no arguments to see command line parameters.

#### Usage example:

```bash
docker run --rm -it -v `pwd`:`pwd` -w `pwd` --user $(id -u):$(id -g) mriffle/conga-to-limelight:latest \
    -f ./proteins.fasta \
    -t ./conga.target_mods.txt \
    -l ./conga.log.txt \
    -o conga.limelight.xml
```

### Command line parameters:

```
  -f, --fasta-file=<fastaFile>
                             Full path to FASTA file used in the experiment. E.g.,
                               /data/yeast.fa
  -t, --conga-target_mods=<targetsFile>
                             Full path to the CONGA targets file (results) E.g.,
                               /data/results/conga.target_mods.txt.
  -l, --conga-log=<logFile>  Full path to the CONGA log file E.g.,
                               /data/results/conga.log.txt
  -o, --out-file=<outFile>   Full path to use for the Limelight XML output file. E.
                               g., /data/my_analysis/crux.limelight.xml
  -v, --verbose              If this parameter is present, error messages will
                               include a full stacktrace. Helpful for debugging.
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.
```

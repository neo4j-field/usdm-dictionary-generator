# usdm-dictionary-generator
Use the pullXmis.sh script to copy the correct USDM_UML.xmi and USDM_CT.xslx files into the current/previous resource folders  

    sh pullXmis <sprint branch>
- main branch get's copied to prevRelease
- sprint branch gets copied to currentRelease
- DDF-RA/Deliverables/CT/USDM_CT.xslx from the sprint branch gets copied to resources

## prerequisites

- The script assumes that both Git Repos (DDF-RA and this one) have been cloned into the same parent folder
- The Data dictionary generator uses the CT spreadsheet for details about the attributes
  - If the order of the columns change or if additional columns are added `CptParse.populateMapwithCpt` method will need to be updated.
  - CT should avoid using line feeds.  Anything following will be truncated.
  - There a problem with the current spreadsheet that is causing an IO error
    - Something added with the Content class is causing the last few lines not to be read
    - Since those lines are from the secons tab and not used by the generator it does not effect the output

Run GeneratorApp with `--compare-releases` to generate the `UML_DELTA.csv` file
    
Run again with `--gen-table` to generate the `datadictionary.MD` file

UML_DELTA get's renamed to UML_DELTA_<prev>_<current> (i.e UML_DELTA_1.11_1.14) and pushed to the sprint branch
dataDictionary.MD is used to update the Wiki at https://wiki.cdisc.org/display/USDMIGv1/USDM+Data+Dictionary  and pushed to the sprint branch in the same folder
    
    DDF-RA/Deliverables/UML



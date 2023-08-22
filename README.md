# usdm-dictionary-generator
Use the pullXmis.sh script to copy the correct USDM_UML.xmi and USDM_CT.xslx files into the current/previous resource folders  

    sh pullXmis <sprint branch>
    main branch get's copied to prevRelease
    sprint branch gets copied to currentRelease
    DDF-RA/Deliverables/CT/USDM_CT.xslx from the sprint branch gets copied to resources

Run GeneratorApp with --compare-releases
    
Check for changes to properties that end with Id or Ids and add them to the cardinality.json file

Run again with --gen-table to get the MD output

UML_DELTA get's renamed to UML_DELTA_<prev>_<current> (i.e UML_DELTA_1.11_1.14) and pushed to the sprint branch
    
    DDF-RA/Deliverables/UML

dataDictionary.MD is used to update the Wiki at https://wiki.cdisc.org/display/USDMIGv1/USDM+Data+Dictionary  and pushed to the sprint branch in the same folder


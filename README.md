# usdm-dictionary-generator
Use the pullXmis.sh script to copy the correct USDM_UML.xmi and USDM_CT.xslx files into the current/previous resource folders  

    sh pullXmis <sprint branch>
    main branch get's copied to prevRelease
    sprint branch gets copied to currentRelease
    CT file from the sprint branch gets copied to resources

Run GeneratorApp with --compare-releases
    
Check for changes to properties that end with Id or Ids and add them to the cardinality.json file

Run again with --gen-table to get the MD output


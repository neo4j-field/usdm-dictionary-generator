cd ../DDF-RA
git checkout main
git pull
cp Deliverables/UML/USDM_UML.xmi ../usdm-dictionary-generator/src/main/resources/prevRelease/.
git checkout $1
git pull
cp Deliverables/UML/USDM_UML.xmi ../usdm-dictionary-generator/src/main/resources/currentRelease/.
cp Deliverables/CT/USDM_CT.xlsx ../usdm-dictionary-generator/src/main/resources/.
